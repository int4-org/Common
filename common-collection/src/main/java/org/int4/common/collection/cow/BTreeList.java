/*
 * MIT License
 *
 * Copyright (c) 2026 John Hendrikx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.int4.common.collection.cow;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.int4.common.collection.capability.Forkable;
import org.int4.common.collection.capability.Freezable;
import org.int4.common.collection.capability.Snapshottable;
import org.int4.common.collection.cow.Node.Slot;

/**
 * A resizable list implementation using a B+ tree for storing its elements. Provides fast
 * random access, fast inserts and deletions at any position, and a constant time {@link #clone()}
 * which shares its structure with the original (copy-on-write).
 * <p>
 * {@code BTreeList} supports all optional list operations, unless it was frozen (see below). It
 * can be used as a drop-in replacement for {@code ArrayList} or {@code LinkedList} in scenarios where frequent (mid-list) insertions or
 * deletions are performance-sensitive, or where cheap snapshots of a list are useful.
 * <p>
 * The elements are stored in the leaves of a tree. Leaves are grouped under internal nodes,
 * which are in turn grouped under other internal nodes, up to a single root. Both leaves and
 * internal nodes are referred to as nodes. A list with only a few elements consists of a single
 * leaf. The <em>leaf size</em> is the maximum number of elements in a leaf, although a list that
 * fits in a single leaf uses a smaller one (see below), and the <em>node size</em> is the maximum
 * number of children of an internal node.
 *
 * <p><strong>Copy-on-write</strong></p>
 *
 * Cloning a list takes constant time. The clone shares the entire tree with the original, and the
 * elements themselves are never copied. Shared nodes are never modified; instead, the first
 * modification of a list after it was cloned copies the nodes on the path to the changed position.
 * From then on that list modifies its own copies in place, until it is cloned again. Everything
 * not on that path stays shared with the other list.
 * <p>
 * This has the following consequences:
 * <ul>
 *   <li>The original and its clone are independent: a modification of one is never visible in the
 *   other. The elements are shared though, so changes to a mutable element are visible in both.</li>
 *   <li>The cost of a clone is paid by the modifications that follow it. Only the first change in
 *   a part of the tree after a clone has to copy it, later changes there cost nothing extra.
 *   Cloning a list again and again with modifications in between makes most modifications copy
 *   part of the tree, in which case smaller leaves are cheaper.</li>
 *   <li>Lists that share structure only use extra memory for the nodes that were copied, so many
 *   clones of a large list are cheap as long as each is changed in few places.</li>
 *   <li>The original and its clone can be used independently on different threads.</li>
 * </ul>
 * <p>
 * A list can be frozen with {@link #freeze()}, which makes it permanently read-only. Every attempt
 * to modify a frozen list, directly or through an iterator or sublist, throws an
 * {@link UnsupportedOperationException}, and it can not be made modifiable again. A clone of a
 * frozen list is modifiable, which makes a frozen list a convenient master copy: it can be shared
 * freely, and whoever needs a changed version takes a clone, in constant time, and changes that.
 * To get an immutable list from a list that may or may not be frozen, without changing it, use
 * {@link #snapshot()}.
 *
 * <p><strong>Structure and tuning</strong></p>
 *
 * The tree's structure is governed by two parameters:
 * <ul>
 *   <li><strong>Node size:</strong> Larger node sizes reduce the tree's height but
 *   increase the cost of modifications.</li>
 *   <li><strong>Leaf size:</strong> Larger leaf sizes make the tree shallower and
 *   improve access speed, but increase the cost of insertions and deletions within
 *   a leaf, due to larger intra-node shifts.</li>
 * </ul>
 * Both sizes must be powers of two, and can be given when constructing the list. The defaults are
 * a reasonable choice for general use, and most lists will not need different values. As a rule of
 * thumb:
 * <ul>
 *   <li>Lists that are mostly read, iterated, or appended to benefit from larger leaves.</li>
 *   <li>Lists that see many insertions and removals in the middle benefit from smaller leaves,
 *   and from a larger node size to keep the tree shallow.</li>
 *   <li>Lists that are cloned and then modified benefit from smaller leaves, as the first
 *   modification of a shared leaf copies it.</li>
 * </ul>
 * The best values depend on the size of the list and the mix of operations, so measure with a
 * realistic workload when performance matters.
 * <p>
 * The tree grows following typical B+ tree behavior: when a leaf becomes full, elements
 * are redistributed among siblings if possible, or a new leaf is created. Similarly, internal
 * nodes are split or merged as needed, potentially increasing the tree height by adding a new root.
 * Leaves are generally kept reasonably full, at around 75% on average. Rebalancing occurs only when
 * a leaf is full or falls below roughly half capacity (or two-thirds if it has only one sibling).
 * Certain removal patterns can drop leaf occupancy to about 50%, or lower when only two leaves
 * remain, until the leaves are small enough to be merged.
 * <p>
 * For very small lists, when the tree consists of only a root leaf node, the leaf size is
 * dynamically adjusted, growing or shrinking by factors of two starting from a minimum size of
 * {@value Node#MINIMUM_NODE_SIZE}, until the configured target leaf size is reached. This keeps
 * memory use and performance reasonable for tiny lists.
 *
 * <p><strong>Time Complexity Summary</strong></p>
 *
 * Random access operations ({@code get}, {@code set}) take {@code O(log n)} time.
 * Insertions and deletions also take {@code O(log n)} time. Their cost grows with the node size
 * and the leaf size, as every node on the path must be updated and elements within the leaf must
 * be shifted. How much work this is depends on where the change falls within each node and the
 * leaf on its path, so it varies from one position to the next. Insertions and removals at either
 * end are the cheapest, as they need very little work in every node.
 *
 * <table border="1">
 *   <caption>Time Complexity of Operations</caption>
 *   <thead>
 *     <tr>
 *       <th>Operation</th>
 *       <th>Time Complexity</th>
 *       <th>Notes</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>{@code get(index)}</td>
 *       <td>{@code O(log n)}</td>
 *       <td>One step per level of the tree.</td>
 *     </tr>
 *     <tr>
 *       <td>{@code getFirst()}, {@code getLast()}</td>
 *       <td>{@code O(log n)}</td>
 *       <td>Fast path down the left- or rightmost branch.</td>
 *     </tr>
 *     <tr>
 *       <td>{@code set(index, E)}</td>
 *       <td>{@code O(log n)}</td>
 *       <td>Same as {@code get}, plus copying the nodes on the path when they are shared with a clone.</td>
 *     </tr>
 *     <tr>
 *       <td>{@code add(index, E)}</td>
 *       <td>{@code O(log n)}</td>
 *       <td>Cost grows with node size and leaf size, and varies with position; cheapest at the ends.</td>
 *     </tr>
 *     <tr>
 *       <td>{@code remove(index)}</td>
 *       <td>{@code O(log n)}</td>
 *       <td>Same as {@code add}.</td>
 *     </tr>
 *     <tr>
 *       <td>{@code add(E)}, {@code addFirst(E)}, {@code addLast(E)}</td>
 *       <td>{@code O(log n)}</td>
 *       <td>Down the left- or rightmost branch; no elements are shifted.</td>
 *     </tr>
 *     <tr>
 *       <td>{@code remove(size - 1)}, {@code removeFirst()}, {@code removeLast()}</td>
 *       <td>{@code O(log n)}</td>
 *       <td>Same as the additions at the edges.</td>
 *     </tr>
 *     <tr>
 *       <td>{@code clone()}</td>
 *       <td>{@code O(1)}</td>
 *       <td>Shares the tree; shared nodes are copied by later modifications.</td>
 *     </tr>
 *     <tr>
 *       <td>{@code clear()}</td>
 *       <td>{@code O(1)}</td>
 *       <td>Clears the structure by creating a new empty root.</td>
 *     </tr>
 *     <tr>
 *       <td>{@code size()}</td>
 *       <td>{@code O(1)}</td>
 *       <td>Constant-time lookup from list metadata.</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p><strong>Iterators and thread safety</strong></p>
 *
 * The iterators returned by this implementation are <em>fail-fast</em>. If the list is structurally
 * modified at any time after the iterator is created, except through the iterator's own methods,
 * the iterator will throw a {@link ConcurrentModificationException}. Replacing an element with
 * {@code set}, directly or through an iterator, is not a structural modification and does not
 * invalidate iterators.
 * <p>
 * This class is <strong>not thread-safe</strong>. External synchronization must be used if accessed
 * concurrently by multiple threads. Consider wrapping the list with {@link Collections#synchronizedList}.
 * A clone and its original can be used independently on different threads, but cloning must not be
 * performed concurrently with use of the original list, and the clone must be safely published to
 * the thread that uses it.
 * <p>
 * A frozen list is the exception: as nothing can modify it, it can be read and cloned by multiple
 * threads at the same time without synchronization, as long as it was safely published after it
 * was frozen. Iterators are never shared; each thread must use its own.
 *
 * @param <E> the type of elements in this list
 * @see java.util.List
 * @author John Hendrikx
 */
public final class BTreeList<E> extends AbstractList<E> implements Cloneable, Forkable<BTreeList<E>>, Freezable, Snapshottable<BTreeList<E>> {
  private static final int DEFAULT_NODE_SIZE = 128;  // must be a power of 2
  private static final int DEFAULT_LEAF_SIZE = 1024;  // must be a power of 2

  private final int leafSize;
  private final int nodeSize;

  private Node root;
  private int height;  // number of levels of non-leaf nodes above the leaves, so 0 when the root is a leaf
  private int size;
  private Node.Owner owner = new Node.Owner();  // null once frozen: without an owner, no node can be modified
  private long version;  // incremented on every modification; notifies iterators that cached nodes may have been replaced

  /**
   * Constructs a new instance with default balanced values for node and leaf sizes.
   */
  public BTreeList() {
    this(DEFAULT_NODE_SIZE, DEFAULT_LEAF_SIZE);
  }

  /**
   * Constructs a new instance containing the elements of the given collection
   * in the order returned by the collection.
   *
   * @param collection a collection to use for initial elements, cannot be {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  public BTreeList(Collection<? extends E> collection) {
    this(DEFAULT_NODE_SIZE, DEFAULT_LEAF_SIZE, collection);
  }

  /**
   * Constructs a new instance containing the elements of the given collection
   * in the order returned by the collection using the given node and leaf sizes.
   *
   * @param nodeSize a node size, must be a power of 2 in the range of {@value Node#MINIMUM_NODE_SIZE} to 2^20
   * @param leafSize a leaf size, must be a power of 2 in the range of {@value Node#MINIMUM_NODE_SIZE} to 2^20
   * @param collection a collection to use for initial elements, cannot be {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  public BTreeList(int nodeSize, int leafSize, Collection<? extends E> collection) {
    this(nodeSize, leafSize);

    // TODO a redundant node is allocated initially due to constructor delegation

    this.root = Node.create(owner, nodeSize, leafSize, collection.iterator());
    this.height = root.height();
    this.size = root.sumChildSizes();
  }

  // TODO consider keeping the nodeSize/leafSize only via factory method

  /**
   * Constructs a new instance with the given node and leaf sizes.
   *
   * @param nodeSize a node size, must be a power of 2 in the range of {@value Node#MINIMUM_NODE_SIZE} to 2^20
   * @param leafSize a leaf size, must be a power of 2 in the range of {@value Node#MINIMUM_NODE_SIZE} to 2^20
   */
  public BTreeList(int nodeSize, int leafSize) {
    if(nodeSize < Node.MINIMUM_NODE_SIZE || nodeSize > 1 << 20) {
      throw new IllegalArgumentException("nodeSize must not be less than " + Node.MINIMUM_NODE_SIZE + " and not larger than 1048576: " + nodeSize);
    }
    if(leafSize < Node.MINIMUM_NODE_SIZE || leafSize > 1 << 20) {
      throw new IllegalArgumentException("leafSize must not be less than " + Node.MINIMUM_NODE_SIZE + " and not larger than 1048576: " + leafSize);
    }
    if(Integer.highestOneBit(nodeSize) != nodeSize) {
      throw new IllegalArgumentException("nodeSize must be a power of 2: " + nodeSize);
    }
    if(Integer.highestOneBit(leafSize) != leafSize) {
      throw new IllegalArgumentException("leafSize must be a power of 2: " + leafSize);
    }

    this.nodeSize = nodeSize;
    this.leafSize = leafSize;
    this.root = Node.empty(owner, Node.MINIMUM_NODE_SIZE);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void clear() {
    checkMutable();

    modCount++;
    version++;

    this.root = Node.empty(owner, Node.MINIMUM_NODE_SIZE);
    this.height = 0;
    this.size = 0;
  }

  @SuppressWarnings("unchecked")
  @Override
  public E get(int index) {
    Objects.checkIndex(index, size);

    int fromEnd = size - index;

    if(fromEnd <= (leafSize >> 2)) {
      Node last = root.getLastLeaf(height);

      if(fromEnd <= last.getUsedSlots()) {
        return (E)last.getValue(last.getUsedSlots() - fromEnd);
      }
    }

    return (E)root.get(index, height);
  }

  @SuppressWarnings("unchecked")
  @Override
  public E getFirst() {
    if(size == 0) {
      throw new NoSuchElementException();
    }

    return (E)root.getFirstLeaf(height).getValue(0);
  }

  @SuppressWarnings("unchecked")
  @Override
  public E getLast() {
    if(size == 0) {
      throw new NoSuchElementException();
    }

    Node last = root.getLastLeaf(height);

    return (E)last.getValue(last.getUsedSlots() - 1);
  }

  @SuppressWarnings("unchecked")
  @Override
  public E set(int index, E element) {
    checkMutable();

    Objects.checkIndex(index, size);

    int copies = owner.copies;

    root = root.owned(owner);

    E old = (E)root.set(owner, index, element);

    if(owner.copies != copies) {
      version++;
    }

    return old;
  }

  @Override
  public void add(int index, E element) {
    checkMutable();

    int newSize = size + 1;

    Objects.checkIndex(index, newSize);

    modCount++;
    version++;

    root = root.owned(owner);
    root.add(owner, index, element);

    if(root.isFull()) {

      /*
       * When root is full and it is still a leaf but size is not yet
       * leafSize, double its capacity. Otherwise increase height of the
       * tree by adding a new level.
       */

      if(!root.isLeaf() || newSize == leafSize) {
        root = root.splitFullRoot(owner, nodeSize);
        height++;
      }
      else {
        int targetCapacity = newSize * 2;

        root = root.copyOf(owner, targetCapacity);
      }
    }

    this.size = newSize;
  }

  @Override
  public E remove(int index) {
    checkMutable();

    Objects.checkIndex(index, size);

    modCount++;
    version++;

    root = root.owned(owner);

    @SuppressWarnings("unchecked")
    E element = (E)root.remove(owner, index);
    int usedSlots = root.getUsedSlots();

    if(root.isLeaf()) {
      int nodeSize = root.nodeSize();

      if(nodeSize > 16 && usedSlots < nodeSize >> 2) {  // less than a quarter filled and not at minimum?
        root = root.copyOf(owner, nodeSize >> 1);
      }
    }
    else if(usedSlots == 1) {  // single node only and not leaf already?
      root = root.promoteOnlyChild();
      height--;
    }

    size--;

    return element;
  }

  /*
   * The methods below do not modify the list themselves, but would otherwise only fail on a
   * frozen list when they happen to change something. They always fail, as they do on the
   * unmodifiable lists of the JDK.
   */

  @Override
  public boolean addAll(Collection<? extends E> c) {
    checkMutable();

    return super.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    checkMutable();

    return super.addAll(index, c);
  }

  @Override
  public boolean remove(Object o) {
    checkMutable();

    return super.remove(o);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    checkMutable();

    return super.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    checkMutable();

    return super.retainAll(c);
  }

  @Override
  public boolean removeIf(Predicate<? super E> filter) {
    checkMutable();

    return super.removeIf(filter);
  }

  @Override
  public void replaceAll(UnaryOperator<E> operator) {
    checkMutable();

    super.replaceAll(operator);
  }

  @Override
  public void sort(Comparator<? super E> c) {
    checkMutable();

    super.sort(c);
  }

  @Override
  public E removeFirst() {
    checkMutable();

    return super.removeFirst();
  }

  @Override
  public E removeLast() {
    checkMutable();

    return super.removeLast();
  }

  @Override
  protected void removeRange(int fromIndex, int toIndex) {
    checkMutable();

    super.removeRange(fromIndex, toIndex);
  }

  /**
   * Returns a shallow copy sharing structure with this instance; the elements themselves are not copied.
   * The copy is independent for subsequent modifications and is created in constant time. The original
   * and the clone may be used on different threads, but cloning itself must not run concurrently with
   * use of this list, unless this list is frozen, as cloning such a list does not change it.
   * <p>
   * The clone is always modifiable, also when this list is frozen.
   *
   * @return a clone of this instance, never {@code null}
   */
  @Override
  public BTreeList<E> clone() {
    try {
      @SuppressWarnings("unchecked")
      BTreeList<E> clone = (BTreeList<E>)super.clone();

      clone.root = root;

      if(owner != null) {  // a frozen list never modifies its nodes, so it has no need for a new owner and is not changed by cloning
        this.owner = new Node.Owner();
      }

      clone.owner = new Node.Owner();

      return clone;
    }
    catch(CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void freeze() {
    owner = null;
  }

  @Override
  public boolean isImmutable() {
    return owner == null;
  }

  @Override
  public BTreeList<E> fork() {
    return clone();
  }

  /**
   * {@inheritDoc}
   * <p>
   * This list itself is returned if it is frozen already, otherwise a frozen clone, in which
   * case the rules for {@link #clone()} apply. This list is not changed.
   */
  @Override
  public BTreeList<E> snapshot() {
    if(owner == null) {
      return this;
    }

    BTreeList<E> clone = clone();

    clone.freeze();

    return clone;
  }

  @Override
  public Iterator<E> iterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<E> listIterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    Objects.checkIndex(index, size + 1);

    return new TreeListIterator(index);
  }

  private void checkMutable() {
    if(owner == null) {
      throw new UnsupportedOperationException("list is frozen");
    }
  }

  private class TreeListIterator implements ListIterator<E> {

    /**
     * The index of the next element to be returned.
     */
    private int nextIndex;

    /**
     * The index of the last element that was returned.
     */
    private int lastReturnedIndex = -1;

    /**
     * The modification count that the list is expected to have. If the list
     * doesn't have this count, then a
     * {@link java.util.ConcurrentModificationException} may be thrown by the
     * operations.
     */
    private int expectedModCount;
    private long expectedVersion;

    private Node leaf;
    private int pos;

    private TreeListIterator(int index) {
      expectedModCount = modCount;
      expectedVersion = version;

      initialize(index);
    }

    @Override
    public boolean hasNext() {
      return nextIndex != size;
    }

    @Override
    public boolean hasPrevious() {
      return nextIndex > 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E next() {
      if(version != expectedVersion) {
        if(modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }

        initialize(nextIndex);
        expectedVersion = version;
      }

      if(nextIndex == size) {
        throw new NoSuchElementException();
      }

      if(pos == leaf.getUsedSlots()) {
        leaf = root.findLeaf(nextIndex);
        pos = 0;
      }

      Object current = leaf.getValue(pos++);

      lastReturnedIndex = nextIndex++;

      return (E)current;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E previous() {
      if(version != expectedVersion) {
        if(modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }

        initialize(nextIndex);
        expectedVersion = version;
      }

      if(nextIndex == 0) {
        throw new NoSuchElementException();
      }

      int index = nextIndex - 1;

      if(pos == 0) {
        leaf = root.findLeaf(index);
        pos = leaf.getUsedSlots();
      }

      pos--;
      nextIndex = index;
      lastReturnedIndex = index;

      return (E)leaf.getValue(pos);
    }

    @Override
    public int nextIndex() {
      return nextIndex;
    }

    @Override
    public int previousIndex() {
      return nextIndex - 1;
    }

    @Override
    public void remove() {
      checkMutable();

      if(lastReturnedIndex < 0) {
        throw new IllegalStateException();
      }

      checkModCount();

      BTreeList.this.remove(lastReturnedIndex);

      expectedModCount = modCount;
      expectedVersion = version;

      if(nextIndex != lastReturnedIndex) {  // last was call to next()
        nextIndex--;
      }

      initialize(nextIndex);

      lastReturnedIndex = -1;  // prevents remove being allowed twice
    }

    @Override
    public void set(E obj) {
      checkMutable();

      if(lastReturnedIndex < 0) {
        throw new IllegalStateException();
      }

      checkModCount();

      try {
        long before = version;

        BTreeList.this.set(lastReturnedIndex, obj);

        if(version != before) {
          expectedVersion = version;
          initialize(nextIndex);
        }
      }
      catch(IndexOutOfBoundsException ex) {  // note: only occurs if list was shrunk concurrently after mod count was already checked
        throw new ConcurrentModificationException();
      }
    }

    @Override
    public void add(E obj) {
      checkMutable();
      checkModCount();

      try {
        BTreeList.this.add(nextIndex, obj);

        nextIndex++;
        lastReturnedIndex = -1;
        expectedModCount = modCount;
        expectedVersion = version;

        initialize(nextIndex);
      }
      catch(IndexOutOfBoundsException ex) {  // note: only occurs if list was shrunk concurrently after mod count was already checked
        throw new ConcurrentModificationException();
      }
    }

    /**
     * Checks the modification count of the list is the value that this object
     * expects.
     */
    private void checkModCount() {
      if(modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
    }

    private void initialize(int index) {
      Slot slot = root.findSlot(index);

      this.leaf = slot.node();
      this.pos = slot.index();
      this.nextIndex = index;
    }
  }
}
