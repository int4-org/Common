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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.int4.common.collection.util.RingArrays;

final class Node {  // package private until there is a good reason to allow public use
  static final int MINIMUM_NODE_SIZE = 8;  // must be a power of 2

  /**
   * The origin that a non-leaf node starts with. Entries in childStarts are stored as
   * offset + origin, so the origin positions the range of possible entries within the int range.
   * Offsets are never negative and at most Integer.MAX_VALUE, so the origin must be at most 0
   * (with some room to spare, as entries must stay below the Integer.MAX_VALUE terminator) and
   * at least Integer.MIN_VALUE. It is placed in the middle of that range, leaving the same
   * amount of room on either side for the origin to move.
   */
  static final int ORIGIN = -(1 << 30);

  /**
   * How far the origin of a non-leaf node may move away from {@link #ORIGIN} before it is moved
   * back. This is simply a range of allowed values; it does not need to be a power of two, and
   * 1 << 29 was chosen only because it is a convenient value that is safely within the limits.
   * Any value that keeps ORIGIN - ORIGIN_BAND at or above Integer.MIN_VALUE and
   * ORIGIN + ORIGIN_BAND safely below 0 would work. A larger band makes moving the origin back
   * rarer, and allows larger single size changes to use the moving origin.
   */
  static final int ORIGIN_BAND = 1 << 29;

  static final class Owner {
    int copies;
  }

  static final class Slot {
    private final Node node;
    private final int index;

    Slot(Node node, int index) {
      this.node = node;
      this.index = index;
    }

    Node node() { return node; }
    int index() { return index; }
  }

  static Node empty(Owner owner, int leafSize) {
    Objects.requireNonNull(owner, "owner");

    return new Node(owner, leafSize, true);
  }

  static Node leaf(Owner owner, int leafSize, Object... values) {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(values, "values");

    Node node = new Node(owner, leafSize, true);

    if(values.length > 0) {
      int actual = node.insertMaxValues(0, values, 0, values.length);

      if(actual != values.length) {
        throw new IllegalArgumentException("too many values for a leaf of size " + leafSize + ": " + values.length);
      }
    }

    return node;
  }

  /**
   * Creates a complete tree of nodes containing all values supplied by the given
   * iterator in the order they were returned by the iterator.
   *
   * @param owner the owner the created nodes belong to, cannot be {@code null}
   * @param nodeSize the size of non-leaf (internal) nodes, cannot be less than {@value #MINIMUM_NODE_SIZE}, or larger than 1048576, and must be a power of 2
   * @param leafSize the size of leaf nodes, cannot be less than {@value #MINIMUM_NODE_SIZE}, or larger than 1048576, and must be a power of 2
   * @param iterator an iterator, cannot be {@code null}
   * @return the root node of the constructed tree, never {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  public static Node create(Owner owner, int nodeSize, int leafSize, Iterator<?> iterator) {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(iterator, "iterator");

    List<Node> nodes = new ArrayList<>();
    Node previous = null;

    // Create all leaf nodes:
    while(iterator.hasNext()) {
      Node leaf = previous == null ? new Node(owner, MINIMUM_NODE_SIZE, true) : createSibling(previous, owner);

      for(;;) {
        while(iterator.hasNext() && leaf.usedSlots < leaf.capacity()) {
          leaf.insertValue(leaf.usedSlots, iterator.next());
        }

        if(leaf.nodeSize() == leafSize || !iterator.hasNext()) {
          break;
        }

        leaf = leaf.copyOf(owner, leaf.nodeSize() << 1);  // grow initial node until it reaches the target size
      }

      nodes.add(leaf);

      previous = leaf;
    }

    // Create internal nodes, until a single root node is reached:
    while(nodes.size() > 1) {
      List<Node> children = nodes;

      previous = null;
      nodes = new ArrayList<>();

      Iterator<Node> childrenIterator = children.iterator();

      while(childrenIterator.hasNext()) {
        Node node = previous == null ? new Node(owner, nodeSize, false) : createSibling(previous, owner);

        nodes.add(node);

        while(childrenIterator.hasNext() && node.usedSlots < node.capacity()) {
          Node child = childrenIterator.next();

          node.insertNode(node.usedSlots, child);
        }

        previous = node;
      }

      /*
       * The last node of each tree level could be nearly empty, redistribute
       * it if it is below capacity.
       */

      Node parent = nodes.getLast();
      Node last = parent.getNode(parent.usedSlots - 1);

      if(last.isBelowCapacity(parent.usedSlots)) {
        last.redistributeAfterDelete(owner, parent, parent.usedSlots - 1);
      }
    }

    return nodes.size() == 1 ? nodes.getFirst() : Node.empty(owner, MINIMUM_NODE_SIZE);
  }

  static Node nodes(Owner owner, int capacity, Node... nodes) {  // Note: can't be used to create nodes of non-leaf nodes currently
    Objects.requireNonNull(owner, "owner");

    return new Node(owner, capacity, nodes);
  }

  /*
   * Only used when not a leaf, null indicates it is a leaf. For each used child it holds the
   * offset of the child's first element (relative to this node) plus the origin (see rotation).
   * The first unused entry holds Integer.MAX_VALUE as a terminator, which allows searching
   * without bounds checks on the number of used slots: entry 0 is always equal to the origin
   * (the first child starts at offset 0, so entry 0 is 0 + origin; it is a copy of rotation that
   * is kept in sync), which is never more than a search key and ends a search to the left, and
   * the terminator is more than any search key and ends a search to the right, even for the end
   * of the node. A search is only done on nodes that are not full, so the terminator always has
   * a slot. Entries after the terminator are not used.
   */
  private final int[] childStarts;
  private final Object[] slots;    // either nodes (non-leaf) or elements (leaf)
  private final Owner owner;

  private int total;               // total size of the children (non-leaf only), the end offset of the last child
  private int usedSlots;

  /*
   * For a leaf, the index in slots of its first element. For a non-leaf, the origin: the value
   * that is added to an offset to get its entry in childStarts. When the size of a child changes,
   * the offsets of the children after it must change. Moving the origin instead allows updating
   * the children before it (and the child itself) as the offsets of the children after it then
   * keep their entry, so whichever side has fewer children can be updated. The origin starts at
   * ORIGIN and is kept within ORIGIN_BAND of it, which guarantees that entries and search keys
   * never overflow and stay below the terminator. The origin is also stored in entry 0 of
   * childStarts, but this field is used to read it. A non-leaf node could also use a ring like a
   * leaf does, but that would only speed up adding and removing children, which is not worth
   * it as non-leaf nodes tend to be small.
   */
  private int rotation;

  /*
   * A hint for guessing which child holds an index: the number of children per element
   * as a fixed point number with 31 fractional bits (from 0 up to just below 1.0), as of
   * the last refresh. It only speeds up the search and can be stale or zero without
   * affecting correctness. 31 fractional bits are enough as a list cannot hold more
   * than 2^31 - 1 elements, which keeps the error of the guess below one child.
   */
  private int scale;

  private Node(Owner owner, int capacity, Node... nodes) {
    this(owner, capacity, false);

    Objects.requireNonNull(nodes, "nodes");

    if(nodes.length == 0 || nodes.length > capacity) {
      throw new IllegalArgumentException("nodes cannot be empty or exceed capacity: " + nodes.length);
    }

    int running = 0;

    for(int i = 0; i < nodes.length; i++) {
      Node child = nodes[i];

      this.childStarts[i] = running + rotation;
      this.slots[i] = child;

      running += child.sumChildSizes();
    }

    this.total = running;
    this.usedSlots = nodes.length;

    terminate();
    refreshScale();
  }

  private Node(Owner owner, int capacity, boolean isLeaf) {
    if(Integer.highestOneBit(capacity) != capacity) {
      throw new IllegalArgumentException("capacity must be a power of 2: " + capacity);
    }
    if(capacity < MINIMUM_NODE_SIZE || capacity > (1 << 20)) {
      throw new IllegalArgumentException("capacity outside valid range: " + capacity);
    }

    this.owner = owner;
    this.childStarts = isLeaf ? null : new int[capacity];
    this.slots = new Object[capacity];

    if(!isLeaf) {
      this.rotation = ORIGIN;
    }
  }

  private Node(Node node, Owner owner) {  // shallow copy constructor for copy-on-write
    this.childStarts = node.childStarts == null ? null : node.childStarts.clone();
    this.slots = node.slots.clone();
    this.usedSlots = node.usedSlots;
    this.rotation = node.rotation;
    this.scale = node.scale;
    this.total = node.total;
    this.owner = owner;
  }

  Node owned(Owner owner) {
    Objects.requireNonNull(owner, "owner");

    if(this.owner == owner) {
      return this;
    }

    owner.copies++;

    return new Node(this, owner);
  }

  /**
   * Returns the size of this node, which is the number of slots it has. A node is full
   * (see {@link #isFull()}) when all of its slots are in use. The size of a leaf that is
   * the root of a small tree can be smaller than the configured leaf size, as such a leaf
   * grows and shrinks with the number of values it holds.
   *
   * @return the number of slots of this node, at least {@value #MINIMUM_NODE_SIZE}
   */
  public int nodeSize() {
    return slots.length;
  }

  /**
   * Finds the leaf that holds the value at the given index of the tree rooted at this node,
   * and the position of that value within the leaf. An index equal to the size of the tree
   * (see {@link #sumChildSizes()}) is allowed and results in the last leaf, with a position
   * just past its last value.
   * <p>
   * The index is not validated; the result is unspecified if it is negative or larger than
   * the size of the tree.
   *
   * @param index an index relative to this node, cannot be negative or exceed the size of the tree
   * @return a {@link Slot} with the leaf and the index relative to that leaf, never {@code null}
   */
  public Slot findSlot(int index) {
    Node node = this;
    int relativeIndex = index;

    while(!node.isLeaf()) {
      int i = node.childFor(relativeIndex);

      relativeIndex -= node.before(i);
      node = node.getNode(i);
    }

    return new Slot(node, relativeIndex);  // usually removed by escape analysis
  }

  /**
   * Finds the leaf that holds the value at the given index of the tree rooted at this node.
   * This is the same as {@link #findSlot(int)}, but only returns the leaf and does not
   * allocate.
   * <p>
   * The index is not validated; the result is unspecified if it is negative or larger than
   * the size of the tree.
   *
   * @param index an index relative to this node, cannot be negative or exceed the size of the tree
   * @return the leaf holding the given index, never {@code null}
   */
  public Node findLeaf(int index) {
    Node node = this;
    int relativeIndex = index;

    while(!node.isLeaf()) {
      int i = node.childFor(relativeIndex);

      relativeIndex -= node.before(i);
      node = node.getNode(i);
    }

    return node;
  }

  /*
   * Returns the number of levels of non-leaf nodes above the leaves, which is 0 when this node
   * is a leaf. All leaves are at the same depth, so following the first child is enough.
   */
  int height() {
    int height = 0;

    for(Node node = this; !node.isLeaf(); node = node.getNode(0)) {
      height++;
    }

    return height;
  }

  /*
   * Returns the first leaf of a tree with the given height (see height()).
   */
  Node getFirstLeaf(int height) {
    Node node = this;

    for(int h = height; h > 0; h--) {
      node = (Node)node.slots[0];
    }

    assert node.isLeaf() : "tree is higher than the given height: " + height;

    return node;
  }

  /*
   * Returns the last leaf of a tree with the given height (see height()).
   */
  Node getLastLeaf(int height) {
    Node node = this;

    for(int h = height; h > 0; h--) {
      node = (Node)node.slots[node.usedSlots - 1];
    }

    assert node.isLeaf() : "tree is higher than the given height: " + height;

    return node;
  }

  Node copyOf(Owner owner, int newLeafSize) {
    if(!isLeaf()) {
      throw new IllegalArgumentException("can only make copies of leaf nodes: " + this);
    }
    if(newLeafSize < usedSlots) {
      throw new IllegalArgumentException("newLeafSize must be large enough to hold all used slots: " + newLeafSize + " > " + usedSlots);
    }

    Node newNode = empty(owner, newLeafSize);

    RingArrays.copy(slots, rotation, newNode.slots, 0, usedSlots);

    newNode.usedSlots = usedSlots;

    return newNode;
  }

  /**
   * Returns the value at the given index of the tree rooted at this node. The height of the
   * tree is determined first, which takes time proportional to the height; a caller that
   * knows the height should use the variant that takes it.
   * <p>
   * The index is not validated; the result is unspecified if it is negative or not less than
   * the size of the tree.
   *
   * @param relativeIndex an index relative to this node, cannot be negative or exceed the size of the tree minus one
   * @return the value at the given index, can be {@code null} if {@code null} values are stored
   */
  public Object get(int relativeIndex) {
    return get(relativeIndex, height());
  }

  /*
   * Returns the element at the given index, for a tree of the given height (see height()).
   * Counting the levels down is cheaper than testing every node on the way for being a leaf,
   * as it gives a loop that is known to be finite and has no data dependent exit.
   */
  Object get(int relativeIndex, int height) {
    Node node = this;
    int index = relativeIndex;

    for(int h = height; h > 0; h--) {
      int key = index + node.rotation;
      int i = node.childFor(index, key);

      index = key - node.childStarts[i];
      node = (Node)node.slots[i];
    }

    assert node.isLeaf() : "tree is higher than the given height: " + height;

    return node.slots[(index + node.rotation) & (node.slots.length - 1)];
  }

  Object set(Owner owner, int relativeIndex, Object value) {
    if(isLeaf()) {
      return setValue(relativeIndex, value);
    }

    int i = childFor(relativeIndex);
    Node child = ownedChild(owner, i);

    return child.set(owner, relativeIndex - before(i), value);
  }

  void add(Owner owner, int relativeIndex, Object value) {
    if(isLeaf()) {
      insertValue(relativeIndex, value);

      return;
    }

    /*
     * An index on the boundary between two children is added to the end of
     * the earlier child, which is the child holding the element before it.
     */

    int i = relativeIndex == 0 ? 0 : childFor(relativeIndex - 1);
    Node child = ownedChild(owner, i);

    child.add(owner, relativeIndex - before(i), value);

    /*
     * When add returns, a new value or node was added to the child. The size of the
     * child, and with it the offsets of the children after it and the total, must be
     * updated to reflect this.
     */

    adjustSize(i, 1);
    checkDrift();

    /*
     * It's possible that this node is now full. If so, redistribute
     * its children to other nodes or create a new node:
     */

    if(child.isFull()) {
      child.redistributeAfterInsert(owner, this, i);
    }
  }

  Object remove(Owner owner, int relativeIndex) {
    if(isLeaf()) {
      return removeValue(relativeIndex);
    }

    int i = childFor(relativeIndex);
    Node child = ownedChild(owner, i);

    Object removedElement = child.remove(owner, relativeIndex - before(i));

    adjustSize(i, -1);
    checkDrift();

    if(child.isBelowCapacity(usedSlots)) {
      child.redistributeAfterDelete(owner, this, i);
    }

    return removedElement;
  }

  Node splitFullRoot(Owner owner, int capacity) {
    if(!isFull()) {
      throw new IllegalStateException("Can only split full root nodes: " + this);
    }

    /*
     * Create a new root, add this node to it as its only child:
     */

    Node newRoot = new Node(owner, capacity, false);

    newRoot.insertNode(0, this);

    /*
     * Now split the only child, which will add an element in the new root:
     */

    redistributeAfterInsert(owner, newRoot, 0);

    assert newRoot.usedSlots == 2;

    return newRoot;
  }

  /**
   * Returns the only child of this node. A root node that is left with a single child
   * is replaced by that child, which reduces the height of the tree by one.
   *
   * @return the only child of this node, never {@code null}
   * @throws IllegalStateException if this node is a leaf, or does not have exactly one child
   */
  public final Node promoteOnlyChild() {
    if(usedSlots != 1 || isLeaf()) {
      throw new IllegalStateException("Can only promote a non-leaf Node with exactly one child: " + usedSlots);
    }

    return getNode(0);
  }

  /**
   * Makes a copy of the tree and all its nodes rooted by this node. The values
   * stored in the tree are only shallowly copied.
   *
   * @param owner the owner the created nodes belong to, cannot be {@code null}
   * @return a new {@code Node}, never {@code null}
   */
  public Node copy(Owner owner) {
    Node copy = new Node(this, owner);

    if(!isLeaf()) {
      for(int i = 0; i < usedSlots; i++) {
        copy.slots[i] = getNode(i).copy(owner);
      }
    }

    return copy;
  }

  /**
   * Indicates this node is full, and must be split or redistributed before it
   * can be used again.
   *
   * @return {@code true} if this node is full, otherwise {@code false}
   */
  public boolean isFull() {
    return slots.length == usedSlots;
  }

  private boolean isBelowCapacity(int parentUsedSlots) {
    return parentUsedSlots == 2 ? usedSlots <= slots.length / 3 : usedSlots < slots.length / 2;
  }

  private Object setValue(int index, Object value) {
    int mask = slots.length - 1;
    int rotatedIndex = (rotation + index) & mask;
    Object old = slots[rotatedIndex];

    slots[rotatedIndex] = value;

    return old;
  }

  private void insertValue(int index, Object value) {
    assert isLeaf();
    assert !isFull() : "trying to insert in " + this + " which is already full";

    int mask = slots.length - 1;
    int endSize = usedSlots - index;
    int r = rotation;
    int writeIndex;

    if(index >= endSize) {  // insert near end?
      writeIndex = (index + r) & mask;

      if(endSize > 0) {  // nothing to move when adding at the end
        RingArrays.copy(slots, writeIndex, (writeIndex + 1) & mask, endSize);
      }
    }
    else {
      rotation = (r - 1) & mask;
      writeIndex = (rotation + index) & mask;

      if(index > 0) {  // nothing to move when adding at the start
        RingArrays.copy(slots, r, rotation, index);
      }
    }

    slots[writeIndex] = value;  // the slot that was freed, now at position index of the sequence

    usedSlots++;
  }

  private int insertMaxValues(int index, Object[] values, int offset, int count) {
    assert isLeaf();
    assert !isFull() : "trying to insert in " + this + " which is already full";

    int max = Math.min(slots.length - usedSlots, count);
    int mask = slots.length - 1;
    int endSize = usedSlots - index;
    int r = rotation;
    int writeIndex;

    if(index >= endSize) {  // insert near end?
      writeIndex = (index + r) & mask;

      if(endSize > 0) {  // nothing to move when adding at the end
        RingArrays.copy(slots, writeIndex, (writeIndex + max) & mask, endSize);
      }
    }
    else {
      rotation = (r - max) & mask;
      writeIndex = (rotation + index) & mask;

      if(index > 0) {  // nothing to move when adding at the start
        RingArrays.copy(slots, r, rotation, index);
      }
    }

    RingArrays.copy(values, offset, slots, writeIndex, max);  // into the slots that were freed

    usedSlots += max;

    return max;
  }

  private void insertNode(int index, Node value) {
    assert !isFull() : "trying to insert in " + this + " which is already full";

    int totalSize = value.sumChildSizes();
    int copyCount = usedSlots - index;
    int start = index < usedSlots ? before(index) : total;

    if(copyCount != 0) {
      System.arraycopy(slots, index, slots, index + 1, copyCount);
      System.arraycopy(childStarts, index, childStarts, index + 1, copyCount);
    }

    slots[index] = value;
    childStarts[index] = start + rotation;
    usedSlots++;

    terminate();

    // the new child is in place but still counted as empty; growing it to its size keeps the offsets and the total in step
    adjustSize(index, totalSize);
    refreshScale();
  }

  private Object removeValue(int index) {
    int mask = slots.length - 1;
    int endSize = usedSlots - index - 1;  // number of values after the removed one
    int r = rotation;
    int removeIndex = (index + r) & mask;
    int freedIndex;
    Object removedValue = slots[removeIndex];

    if(index >= endSize) {  // remove near end?
      freedIndex = (usedSlots - 1 + r) & mask;

      if(endSize > 0) {  // nothing to move when removing the last value
        RingArrays.copy(slots, (removeIndex + 1) & mask, removeIndex, endSize);
      }
    }
    else {
      freedIndex = r;
      rotation = (r + 1) & mask;

      if(index > 0) {  // nothing to move when removing the first value
        RingArrays.copy(slots, r, rotation, index);
      }
    }

    slots[freedIndex] = null;  // the slot that is no longer part of the sequence

    usedSlots--;

    return removedValue;
  }

  private Node removeNode(int index) {
    Node value = getNode(index);
    int copyCount = usedSlots - index - 1;

    // shrinking the child to empty keeps the offsets and the total in step, so removing it afterwards needs no further size changes
    adjustSize(index, -sizeOf(index));

    if(copyCount != 0) {
      System.arraycopy(slots, index + 1, slots, index, copyCount);
      System.arraycopy(childStarts, index + 1, childStarts, index, copyCount);
    }

    usedSlots--;

    slots[usedSlots] = null;
    childStarts[usedSlots] = Integer.MAX_VALUE;

    refreshScale();

    return value;
  }

  boolean isLeaf() {
    return childStarts == null;
  }

  int getUsedSlots() {
    return usedSlots;
  }

  private static final int REDIST_SIZE = 4;

  private void redistributeAfterInsert(Owner owner, Node parent, int childIndex) {
    assert isFull();
    assert parent != null;

    int nodeCount = parent.usedSlots > REDIST_SIZE ? REDIST_SIZE : parent.usedSlots;
    int startIndex = childIndex - 1;

    if(startIndex < 0) {
      startIndex = 0;
    }
    else if(startIndex + nodeCount > parent.usedSlots) {
      startIndex = parent.usedSlots - nodeCount;
    }

    int totalSlotsRequired = parent.sumUsedSlots(startIndex, nodeCount);
    int newNodeCount = shouldIncreaseNodes(nodeCount, totalSlotsRequired) ? nodeCount + 1 : nodeCount;

    assert totalSlotsRequired >= (capacity() - 1) / 2 * newNodeCount : "one or more nodes would get below capacity: parent = " + parent + "; totalSlotsRequired = " + totalSlotsRequired + "; nodeCount = " + nodeCount + "; newNodeCount = " + newNodeCount + "; capacity = " + capacity();

    parent.ownChildren(owner, startIndex, nodeCount);

    if(newNodeCount > nodeCount) {

      /*
       * Always insert new node near the middle of the range, but ensuring
       * that there is a previous node for linking. Redistribution will need
       * to copy less nodes back and forth this way:
       *
       * nodeCount=1: [OriginalNode, NewNode]
       * nodeCount=2: [OriginalNode, NewNode, OriginalNode]
       * nodeCount=3: [OriginalNode, OriginalNode, NewNode, OriginalNode]
       * nodeCount=4: [OriginalNode, OriginalNode, NewNode, OriginalNode, OriginalNode]
       */

      int insertionPoint = startIndex + (newNodeCount >> 1);

      parent.insertNode(insertionPoint, createSibling(parent.getNode(insertionPoint - 1), owner));
    }

    parent.rebalance(startIndex, startIndex + newNodeCount - 1, totalSlotsRequired);

    assert parent.isBalanced(startIndex, startIndex + newNodeCount - 1);
  }

  private void redistributeAfterDelete(Owner owner, Node parent, int childIndex) {
    assert isBelowCapacity(parent.usedSlots);
    assert parent != null;

    int nodeCount = parent.usedSlots > REDIST_SIZE ? REDIST_SIZE : parent.usedSlots;
    int startIndex = childIndex - 1;

    if(startIndex < 0) {
      startIndex = 0;
    }
    else if(startIndex + nodeCount > parent.usedSlots) {
      startIndex = parent.usedSlots - nodeCount;
    }

    int totalSlotsRequired = parent.sumUsedSlots(startIndex, nodeCount);
    int newNodeCount = shouldReduceNodes(nodeCount, totalSlotsRequired) ? nodeCount - 1 : nodeCount;

    parent.ownChildren(owner, startIndex, nodeCount);

    if(newNodeCount < nodeCount) {
      parent.empty(startIndex, startIndex + nodeCount - 1, totalSlotsRequired, startIndex + 1);

      assert parent.isBalanced(startIndex, startIndex + nodeCount - 2);
    }
    else {
      parent.rebalance(startIndex, startIndex + nodeCount - 1, totalSlotsRequired);

      assert parent.isBalanced(startIndex, startIndex + nodeCount - 1);
    }

  }

  private static Node createSibling(Node previous, Owner owner) {
    assert previous != null;

    return new Node(owner, previous.slots.length, previous.isLeaf());
  }

  private void ownChildren(Owner owner, int startIndex, int count) {
    for(int i = startIndex; i < startIndex + count; i++) {
      ownedChild(owner, i);
    }
  }

  /*
   * Returns the child at the given index as a node owned by the given owner, replacing it in this
   * node when it had to be copied. A child that is owned already is not written back, as storing
   * the same reference again still costs a GC write barrier.
   */
  private Node ownedChild(Owner owner, int index) {
    Node child = getNode(index);
    Node owned = child.owned(owner);

    if(owned != child) {
      slots[index] = owned;
    }

    return owned;
  }

  /*
   * Attempt to keep node fill percentages within reasonable limits. A
   * redistribute is triggered when a node is either considered too empty after
   * a delete (below half its capacity, or slightly lower if there is only one
   * other sibling(*)), or when it is over its maximum capacity after an insert.
   * The percentages are chosen to:
   *
   * - Avoid creating nodes that are immediately below capacity or above it, or
   *   are so full or empty that a redistribute is soon needed again
   * - Favor redistributes over creating or removing a node (redistributes
   *   are far cheaper than creating or removing a node as many values may
   *   need to be moved to the new node, or from the node to be deleted).
   * - Attempt to avoid cases where the tree's capacity is less than 50%
   *   utilized.
   *
   * (*) The case when there is only one other sibling happens when the root
   * only has two children, the children being too large to merge into a single
   * node, and too small to add another sibling.
   */

  private boolean shouldIncreaseNodes(int nodeCount, int totalChildren) {
    return totalChildren > (nodeCount * (long)capacity() * 972) >> 10;  // Increase if current nodes exceed 95% fill rate
  }

  private boolean shouldReduceNodes(int nodeCount, int totalChildren) {
    return totalChildren <= ((nodeCount - 1) * (long)capacity() * 820) >> 10;  // Reduce if remaining nodes won't exceed 80% fill rate
  }

  Node getNode(int index) {
    return (Node)slots[index];
  }

  Object getValue(int index) {
    return slots[(index + rotation) & (slots.length - 1)];
  }

  private int capacity() {
    return slots.length - 1;
  }

  /**
   * Balances the values in the given range of nodes. If perfect balance is
   * unachievable, the left most nodes are given any extra values left over due
   * to rounding.
   * <p>
   * This method will do nothing if given a range of zero or one nodes long.
   *
   * @param fromIndex start index of the range (inclusive), cannot be negative or exceed {@code usedSlots - 1}
   * @param toIndex end index of the range (exclusive), cannot be negative or exceed {@code usedSlots}
   * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex} is out of range, or if {@code fromIndex}
   *   is greater than {@code toIndex}
   */
  public void rebalance(int fromIndex, int toIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, usedSlots);

    if(toIndex - fromIndex < 2) {  // nothing to balance with fewer than two nodes
      return;
    }

    int endIndex = toIndex - 1;

    rebalance(fromIndex, endIndex, sumUsedSlots(fromIndex, toIndex - fromIndex));
  }

  /**
   * Empties out the given node at {@code toEmptyIndex} that is part of the range
   * given by {@code fromIndex} and {@code toIndex} (exclusive).
   * <p>
   * If the indicated node could not be emptied, this function returns {@code false}
   * and does not modify any nodes.
   *
   * @param fromIndex start index of the range (inclusive), cannot be negative or exceed {@code usedSlots - 1}
   * @param toIndex end index of the range (exclusive), cannot be negative or exceed {@code usedSlots}
   * @param toEmptyIndex index of node to make empty if possible, must be part of the range
   * @return {@code true} if the node was empty and was removed, otherwise {@code false}
   * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex} is out of range, or if {@code fromIndex}
   *   is greater than {@code toIndex}
   * @throws IllegalArgumentException if {@code toEmptyIndex} is not part of the range, which is always the case for an
   *   empty range
   */
  public boolean emptyOut(int fromIndex, int toIndex, int toEmptyIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, usedSlots);

    if(toEmptyIndex < fromIndex || toEmptyIndex >= toIndex) {
      throw new IllegalArgumentException("toEmptyIndex must be part of the specified range: " + toEmptyIndex);
    }

    int sum = sumUsedSlots(fromIndex, toIndex - fromIndex);
    int newNodeCount = toIndex - fromIndex - 1;

    if(sum > newNodeCount * getNode(0).capacity()) {
      return false;
    }

    empty(fromIndex, toIndex - 1, sum, toEmptyIndex);

    return true;
  }

  private void rebalance(int startIndex, int endIndex, int total) {
    int desiredNodeCount = endIndex - startIndex + 1;

    if(balanceLeft(startIndex, endIndex, total, desiredNodeCount)) {
      boolean unbalanced = balanceRight(startIndex, endIndex, total, desiredNodeCount);

      assert unbalanced == false;
    }

    assert isBalanced(startIndex, endIndex);
  }

  private void empty(int startIndex, int endIndex, int total, int desiredEmptyIndex) {
    int desiredNodeCount = endIndex - startIndex;

    assert total <= desiredNodeCount * getNode(0).capacity();

    // Cases for reducing 4 nodes to 3 nodes, trying to get the second node empty:
    //
    // [30, 30, 30, 30] -> balanceLeft (0, 1, 120, 3) -> [40, 20, 30, 30] (and returns balanced)
    //                  -> balanceRight(1, 3, 120, 3) -> [40,  0, 40, 40] (and returns balanced)
    //                  -> delete empty node
    //
    // [50, 30, 20, 20] -> balanceLeft (0, 1, 120, 3) -> [50, 30, 20, 20] (and returns unbalanced)
    //                  -> balanceRight(1, 3, 120, 3) -> [50,  0, 30, 40] (and returns unbalanced)
    //                  -> delete empty node          -> [50, 30, 40]
    //                  -> balanceLeft (0, 2, 120, 3) -> [40, 40, 40] (and returns balanced)
    //
    // [20, 20, 45, 35] -> balanceLeft (0, 1, 120, 3) -> [40,  0, 45, 35] (and returns balanced)
    //                  -> balanceRight(1, 3, 120, 3) -> [40,  0, 40, 40] (and returns balanced)
    //                  -> delete empty node          -> [40, 40, 40]
    //                  -> balanceLeft (0, 2, 120, 3) -> [40, 40, 40] (and returns balanced)
    //
    // [20, 20, 35, 45] -> balanceLeft (0, 1, 120, 3) -> [40,  0, 35, 45] (and returns balanced)
    //                  -> balanceRight(1, 3, 120, 3) -> [40,  0, 35, 45] (and returns unbalanced)
    //                  -> delete empty node          -> [40, 35, 45]
    //                  -> balanceLeft (0, 2, 120, 3) -> [40, 40, 40] (and returns balanced)

    boolean leftUnbalanced = balanceLeft(startIndex, desiredEmptyIndex, total, desiredNodeCount);
    boolean rightUnbalanced = balanceRight(desiredEmptyIndex, endIndex, total, desiredNodeCount);

    assert getNode(desiredEmptyIndex).usedSlots == 0;

    removeNode(desiredEmptyIndex);

    if(leftUnbalanced || rightUnbalanced) {
      rebalance(startIndex, endIndex - 1, total);
    }

    assert isBalanced(startIndex, endIndex - 1);
  }

  private boolean isBalanced(int startIndex, int endIndex) {
    int leftMostUsedSlots = getNode(startIndex).usedSlots;

    for(int i = startIndex + 1; i <= endIndex; i++) {
      if(leftMostUsedSlots - getNode(i).usedSlots > 1) {
        return false;
      }
    }

    return true;
  }

  /**
   * Balances the child nodes, given by the range {@code startIndex} and {@code endIndex}, by
   * shifting values to the left. It only shifts values if the balance would be
   * improved, otherwise does nothing. This function should be used in conjunction with
   * {@code balanceRight}.
   * <p>
   * The provided total must be the total number of values in the range of nodes being
   * balanced, or the result will be undefined. The desired node count is generally either
   * equal to the length of the range being balanced (when doing redistributes) or one less
   * (when shifting values to clear out a node for removal). It interacts with the total
   * by changing the calculation as to how many values should reside in each node. A lower
   * desired node count means the remaining nodes will need to be more filled.
   * <p>
   * For example, consider three nodes [25, 40, 25] (total = 90). Calling balance left with
   * a desired node count of two will result in [45, 45, 0] and a return value of {@code false}
   * to indicate full balance was achieved. Calling it with a desired node count of 3
   * will result in a redistribution (towards the left) only resulting in [30, 35, 25], returning
   * {@code true} to indicate a balance right will be needed to fix the remaining imbalance.
   *
   * @param startIndex the first index of the range to balance, cannot be negative and must be less than {@code usedSlots}
   * @param endIndex the last index of the range to balance, cannot be negative and must be less than {@code usedSlots}
   * @param total the total number of values that are being balanced, must be positive
   * @param desiredNodeCount the number of nodes to use for distribution calculations, must be positive
   * @return {@code false} indicates the nodes are fully balanced, or {@code true} to indicate a balance right is needed
   */
  private boolean balanceLeft(int startIndex, int endIndex, int total, int desiredNodeCount) {
    assert startIndex >= 0 && startIndex < usedSlots;
    assert endIndex >= 0 && endIndex < usedSlots;
    assert startIndex <= endIndex;  // does nothing if equal, is fine for ease of calling
    assert total > 0;
    assert desiredNodeCount > 0;

    int targetIndex = startIndex;
    int stealCount = 0;
    int t = total;
    int nodesLeft = desiredNodeCount;
    Node target = getNode(targetIndex);
    boolean unbalanced = false;

    while(targetIndex < endIndex) {
      int desiredInNode = (t + nodesLeft - 1) / nodesLeft;    // rounding up for left shifting
      int needed = desiredInNode - target.usedSlots;

      assert desiredInNode <= target.capacity();  // exactly capacity can happen with small nodes sizes to keep a good fill rate, but definitely not MORE than capacity...

      stealCount += needed;  // may become negative, lowering steal amount from subsequent sibling to maintain balance across all nodes

      if(stealCount < 0) {  // if ever negative, need to balance the other way to resolve it
        unbalanced = true;
      }

      int sourceIndex = targetIndex;

      while(stealCount > 0 && sourceIndex < endIndex) {
        Node source = getNode(++sourceIndex);
        int max = Math.min(stealCount, source.usedSlots);

        target.stealSlotsFromStart(source, max);
        stealCount -= max;

        shiftOffsets(targetIndex + 1, sourceIndex + 1, target.sumChildSizes(target.usedSlots - max, max));  // the values moved from the source to the target, which is before it
      }

      t -= desiredInNode;
      targetIndex++;
      target = getNode(targetIndex);
      nodesLeft--;
    }

    return unbalanced;
  }

  private boolean balanceRight(int startIndex, int endIndex, int total, int desiredNodeCount) {
    assert startIndex >= 0 && startIndex < usedSlots;
    assert endIndex >= 0 && endIndex < usedSlots;
    assert startIndex <= endIndex;  // does nothing if equal, is fine for ease of calling
    assert total > 0;
    assert desiredNodeCount > 0;

    int targetIndex = endIndex;
    int t = total;
    int stealCount = 0;
    Node target = getNode(targetIndex);
    int nodesLeft = desiredNodeCount;
    boolean unbalanced = false;

    while(targetIndex > startIndex) {
      int desiredInNode = t / nodesLeft;    // rounding down for right shifting (counterpart to balanceLeft's calculation)

      assert desiredInNode <= target.capacity();  // exactly capacity can happen with small nodes sizes to keep a good fill rate, but definitely not MORE than capacity...

      stealCount = desiredInNode - target.usedSlots;  // may become negative, lowering steal amount from subsequent sibling to maintain balance across all nodes

      if(stealCount < 0) {
        unbalanced = true;
      }

      int sourceIndex = targetIndex;

      while(stealCount > 0 && sourceIndex > startIndex) {
        Node source = getNode(--sourceIndex);
        int max = Math.min(stealCount, source.usedSlots);

        target.stealSlotsFromEnd(source, max);
        stealCount -= max;

        shiftOffsets(sourceIndex + 1, targetIndex + 1, -target.sumChildSizes(0, max));  // the values moved from the source to the target, which is after it
      }

      t -= desiredInNode;
      targetIndex--;
      target = getNode(targetIndex);
      nodesLeft--;
    }

    return unbalanced;
  }

  /**
   * Steals the first {@code slotCount} slots from source and moves them to this
   * end of this node; this function only updates the nodes involved, and leaves
   * updating any ancestors up to the caller.
   *
   * @param source node to steal values from, cannot be {@code null}
   * @param slotCount number of values to steal, must be positive and no more than the number of used slots in the source
   * @throws NullPointerException if any argument is {@code null}
   */
  private void stealSlotsFromStart(Node source, int slotCount) {
    assertStealIsValid(source, slotCount);

    int firstStolen = usedSlots;  // index in this node where the stolen slots end up

    source.usedSlots -= slotCount;
    usedSlots += slotCount;

    if(isLeaf()) {
      int mask = slots.length - 1;

      RingArrays.copy(source.slots, source.rotation, slots, (firstStolen + rotation) & mask, slotCount);
      RingArrays.clear(source.slots, source.rotation, slotCount);

      source.rotation = (source.rotation + slotCount) & mask;
    }
    else {
      int stolen = source.usedSlots > 0 ? source.before(slotCount) : source.total;

      System.arraycopy(source.slots, 0, slots, firstStolen, slotCount);

      for(int i = 0; i < slotCount; i++) {
        childStarts[firstStolen + i] = source.before(i) + total + rotation;
      }

      if(source.usedSlots > 0) {
        System.arraycopy(source.slots, slotCount, source.slots, 0, source.usedSlots);  // Shift slots in node (if any were left after stealing)

        for(int i = 0; i < source.usedSlots; i++) {
          source.childStarts[i] = source.childStarts[slotCount + i] - stolen;
        }
      }

      completeStealOfNodes(source, slotCount, stolen);
    }
  }

  /**
   * Steals the last {@code slotCount} slots from source and moves them to this
   * start of this node; this function only updates the nodes involved, and leaves
   * updating any ancestors up to the caller.
   *
   * @param source node to steal values from, cannot be {@code null}
   * @param slotCount number of values to steal, must be positive and no more than the number of used slots in the source
   * @throws NullPointerException if any argument is {@code null}
   */
  private void stealSlotsFromEnd(Node source, int slotCount) {
    assertStealIsValid(source, slotCount);

    int existing = usedSlots;  // number of slots this node already had, which move up to make room

    source.usedSlots -= slotCount;
    usedSlots += slotCount;

    if(isLeaf()) {
      int mask = slots.length - 1;
      int sourceRotatedCopyStart = (source.usedSlots + source.rotation) & mask;

      rotation = (rotation - slotCount) & mask;  // make space at start in target by moving location of first element

      RingArrays.copy(source.slots, sourceRotatedCopyStart, slots, rotation, slotCount);
      RingArrays.clear(source.slots, sourceRotatedCopyStart, slotCount);
    }
    else {
      int sourceBefore = source.before(source.usedSlots);
      int stolen = source.total - sourceBefore;

      if(existing > 0) {
        System.arraycopy(slots, 0, slots, slotCount, existing);  // Shifts existing slots to make room
        System.arraycopy(childStarts, 0, childStarts, slotCount, existing);  // Shifts existing slots to make room

        for(int i = slotCount; i < usedSlots; i++) {
          childStarts[i] += stolen;
        }
      }

      System.arraycopy(source.slots, source.usedSlots, slots, 0, slotCount);

      for(int i = 0; i < slotCount; i++) {
        childStarts[i] = source.before(source.usedSlots + i) - sourceBefore + rotation;
      }

      completeStealOfNodes(source, slotCount, stolen);
    }
  }

  /*
   * Completes a steal of non-leaf slots, after the slot counts were updated and the slots were
   * moved: the vacated slots of the source are cleared, the stolen total moves from the source
   * to this node, both nodes get a terminator after their last child, and a fresh scale.
   */
  private void completeStealOfNodes(Node source, int slotCount, int stolen) {
    for(int i = source.usedSlots; i < source.usedSlots + slotCount; i++) {
      source.slots[i] = null;
      source.childStarts[i] = 0;
    }

    total += stolen;
    source.total -= stolen;

    terminate();
    source.terminate();

    refreshScale();
    source.refreshScale();
  }

  /**
   * Returns the total number of values stored in the (sub)tree with this node
   * as its root. For a leaf this is the number of used slots, and for any other
   * node it is the total that is kept up to date for its children, so this
   * function takes constant time.
   *
   * @return the size of the (sub)tree represented by this node, never negative
   */
  public int sumChildSizes() {
    return isLeaf() ? usedSlots : total;
  }

  private int sumChildSizes(int offset, int count) {
    if(isLeaf()) {
      return count;
    }

    if(count == 0) {
      return 0;
    }

    return end(offset + count - 1) - before(offset);
  }

  /*
   * Returns the start offset of the given child.
   */
  private int before(int index) {
    return childStarts[index] - rotation;
  }

  /*
   * Returns the end offset of the given child.
   */
  private int end(int index) {
    return index + 1 < usedSlots ? childStarts[index + 1] - rotation : total;
  }

  /*
   * Writes the terminator in the first unused slot. A node that is full has no unused slot,
   * but it also is not searched, and is made to have one (by redistributing) right away.
   */
  private void terminate() {
    if(usedSlots < childStarts.length) {
      childStarts[usedSlots] = Integer.MAX_VALUE;
    }
  }

  /*
   * Returns the size of the given child.
   */
  private int sizeOf(int index) {
    return end(index) - before(index);
  }

  /*
   * Adds the given amount to the size of the given child, and to the total. This changes the offsets
   * of the children after it, which is done by updating either those, or the children before it
   * (and itself) while moving the origin, whichever are fewer.
   */
  private void adjustSize(int index, int amount) {
    if(2 * index + 1 < usedSlots - 1 && moveOrigin(amount)) {
      shiftOffsets(0, index + 1, -amount);

      rotation -= amount;
    }
    else {
      shiftOffsets(index + 1, usedSlots, amount);
    }

    total += amount;
  }

  /*
   * Returns whether the origin can be moved by the given amount and stay within its band. If it
   * can't from where it is, but could from its starting value, it is moved back there first, which
   * costs an update of all offsets but is only needed after about 2^29 changes in the same direction.
   */
  private boolean moveOrigin(int amount) {
    if(Math.abs((long)(rotation - ORIGIN) - amount) <= ORIGIN_BAND) {
      return true;
    }

    if(Math.abs((long)amount) > ORIGIN_BAND) {
      return false;
    }

    shiftOffsets(0, usedSlots, ORIGIN - rotation);

    rotation = ORIGIN;

    return true;
  }

  /*
   * Adds the given amount to the offsets of the children from the first given index up to
   * (not including) the second. A size change of a child needs this for all children after
   * it, and a move of size between two children needs it for the children in between only.
   */
  private void shiftOffsets(int fromIndex, int toIndex, int amount) {
    int[] starts = childStarts;

    for(int i = fromIndex; i < toIndex; i++) {
      starts[i] += amount;
    }
  }

  /*
   * Returns the index of the child that holds the given relative index, which for an index
   * at or beyond the end of this node is the last child. It starts with a guess based on the
   * average child size, which is then corrected by moving left or right until the child is
   * found. The guess only affects how many steps are needed, never the result.
   *
   * The sentinels (entry 0 and the terminator) make the bounds checks on the used slots
   * unnecessary, but they do require that the guess is never more than usedSlots (see
   * checkDrift), as a guess beyond that could be outside of the array, and that this node is
   * not full.
   */
  private int childFor(int index) {
    return childFor(index, index + rotation);
  }

  /*
   * Same as childFor(index), for callers that already have the key (the index plus the origin)
   * and can use it to calculate the index within the child.
   */
  private int childFor(int index, int key) {
    int[] starts = childStarts;
    int i = (int)(((long)index * scale) >>> 31);

    assert i <= usedSlots : "guess beyond the used slots: " + i;
    assert starts[0] == rotation : "first entry differs from the origin: " + starts[0] + " != " + rotation;

    while(key < starts[i]) {
      i--;
    }

    while(key >= starts[i + 1]) {
      i++;
    }

    return i;
  }

  /*
   * Called after the size of a child was changed by one. The cached scale predicts how many
   * children this node has at its current total. The prediction must never exceed the used
   * slots, as that would allow a guess beyond the used slots, and is refreshed when it falls
   * more than one child short. The refresh is the only division, and is rare.
   */
  private void checkDrift() {
    long predicted = ((long)scale * total) >>> 31;

    if(predicted > usedSlots || predicted < usedSlots - 1) {
      refreshScale();
    }
  }

  /*
   * Recomputes the scale from the current number of children and the current total.
   * Must be called whenever the number of children or the total changes by more than
   * the single element handled by checkDrift.
   */
  private void refreshScale() {
    scale = total == 0 ? 0 : (int)Math.min(Integer.MAX_VALUE, ((long)usedSlots << 31) / total);
  }

  private int sumUsedSlots(int offset, int count) {
    int sum = 0;

    for(int i = offset, max = offset + count; i < max; i++) {
      sum += getNode(i).usedSlots;
    }

    return sum;
  }

  private void assertStealIsValid(Node source, int slotCount) {
    assert slotCount <= source.usedSlots && slotCount >= 1 : "slotCount must be 1 <= x <= " + source.usedSlots + ": " + slotCount;
    assert usedSlots + slotCount < slots.length : "insufficient slots available in target node; needed " + slotCount + " but only " + (slots.length - usedSlots) + " available";
    assert source.isLeaf() == isLeaf() : "nodes must be of same type (leaf or non-leaf)";
  }

  @Override
  public String toString() {
    return "Node[slots.length=" + slots.length + "; usedSlots=" + usedSlots + "; rotation=" + rotation + "; total=" + total + "; childStarts=" + (childStarts == null ? "LEAF" : Arrays.toString(childStarts)) + (childStarts == null ? "; slots=" + Arrays.toString(slots) : "") + "]";
  }
}
