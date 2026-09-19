/*
 * MIT License
 *
 * Copyright (c) 2025 John Hendrikx
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

package org.int4.common.collection;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A resizable, ordered collection of elements that allows duplicates and provides
 * fast append operations and efficient removal or membership testing by equality.
 *
 * <p>This class combines an array-backed list with optional hash-based indexing
 * for large collections. Elements are always iterated in insertion order.
 *
 * <p>Key characteristics:
 * <ul>
 *   <li>Appending an element is O(1) amortized.</li>
 *   <li>Removing an element by equality is O(1) on average for large lists (over ~32 elements),
 *       due to internal hash-based lookup. For smaller lists, removal scans the array, which
 *       is not a performance concern for short lists.</li>
 *   <li>{@link #contains(Object)} behaves similarly: O(n) for small lists and O(1) on average
 *       for larger lists using the hash arrays.</li>
 *   <li>Iteration over elements preserves insertion order.</li>
 *   <li>Duplicates are allowed and remain in the order they were added.</li>
 *   <li>Random access via {@link java.util.List#get(int)} is O(n) in the worst case
 *       (when the list contains tombstones). Consequently, iterating over the list
 *       using an index-based loop is O(n²), similar to {@link java.util.LinkedList}.
 *       Users should prefer the {@link #iterator()} or for-each loops for O(n) total
 *       iteration performance.</li>
 *   <li>The list automatically grows as needed and may shrink when many elements are removed,
 *       keeping memory usage proportional to its size.</li>
 * </ul>
 *
 * <p>Memory usage:
 * <ul>
 *   <li>Small lists (under ~32 elements) use only the array of elements, minimizing overhead.</li>
 *   <li>Larger lists allocate additional arrays for hash-based indexing, increasing memory usage
 *       by roughly a factor of 2-3 compared to the element array alone.</li>
 * </ul>
 *
 * <p>Null elements are not permitted.
 *
 * <p>Example usage:
 * <pre>{@code
 * List<String> list = new HashList<>();
 * list.add("A");
 * list.add("B");
 * list.add("A");  // duplicates allowed
 * list.remove("A");  // removes the first occurrence
 *
 * System.out.println(list);  // prints [B, A]
 * }</pre>
 *
 * @param <T> the type of elements held in this list
 * @see java.util.ArrayList
 * @see java.util.LinkedList
 * @author John Hendrikx
 */
public class HashList<T> extends AbstractList<T> {

  /**
   * The threshold when growing the items array also initializes the supporting
   * hash structures.
   */
  private static final int HASH_THRESHOLD = 32;

  /**
   * Minimum allowed capacity of the items array.
   */
  private static final int MINIMUM_CAPACITY = 4;  // must be a power of 2 currently

  /**
   * The numeric base used in the hash structures, must be 1. All valid indices
   * will be >= 1 while 0 is the terminator.
   */
  private static final int BASE = 1;

  /**
   * The array used to store items. This may contain gaps (tombstones) indicated
   * by a null element. Currently this is restricted to a power of 2, but this is
   * not a hard requirement.
   */
  private Object[] items = new Object[MINIMUM_CAPACITY];  // indexed by 0-based position, contains type T

  /**
   * The 1-based index of the next item with the same hash. This forms singly linked hash chains.
   * Must be the same size as the items array. Being 1-based, zero indicates the end of a chain.
   */
  private int[] nextIndices;  // indexed by 0-based position, contains 1-based next position

  /**
   * Contains the 1-based first index of a hash chain. Must be a power of 2 in size. Its size
   * can differ from the items array size.
   */
  private int[] headIndices;  // indexed by bucket, contains 1-based first position

  /**
   * Contains the 1-based last index of a hash chain, which allows appending to the singly linked
   * list without traversing it. Must be the same size as the head indices array.
   */
  private int[] tailIndices;  // indexed by bucket, contains 1-based last position

  /**
   * The mask used to find a hash bucket. When this is zero, the hash structures
   * are not initialized and should not be used.
   */
  private int mask;  // if not zero, it will equal hashIndices.length - 1.

  /**
   * The number of elements in this list.
   */
  private int size;

  /**
   * The position where the next appended item should be added in the items array.
   * This must be equal to or greater than the list's size. The difference indicates
   * the number of tombstones the items array currently holds.
   */
  private int appendPosition;  // appendPosition - size = tombstone count

  @Override
  public int size() {
    return size;
  }

  @Override
  public void clear() {
    modCount++;

    this.items = new Object[MINIMUM_CAPACITY];
    this.nextIndices = null;
    this.headIndices = null;
    this.tailIndices = null;
    this.mask = 0;
    this.size = 0;
    this.appendPosition = 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T get(int index) {
    Objects.checkIndex(index, size);

    return (T)items[getPhysicalIndex(index)];
  }

  @Override
  public boolean add(T item) {
    Objects.requireNonNull(item, "item");

    modCount++;

    ensureCapacity(appendPosition + 1);

    int position = appendPosition;

    items[position] = item;

    if(mask != 0) {
      hashInsert(item, position);
    }

    appendPosition++;
    size++;

    return true;
  }

  @Override
  public boolean remove(Object item) {
    if(item == null) {
      return false;
    }

    if(mask == 0) {
      for(int i = 0; i < appendPosition; i++) {
        if(item.equals(items[i])) {
          modCount++;

          items[i] = null;
          size--;

          ensureCapacity(size);  // checks for excessive tomb stones or excessive capacity

          return true;
        }
      }

      return false;
    }

    int bucket = item.hashCode() & mask;
    int p = headIndices[bucket] - BASE;  // convert to zero-based position
    int previous = -BASE;

    for(;;) {
      if(p == -BASE) {
        return false;
      }

      if(item.equals(items[p])) {  // items[p] here can't ever be a tombstone as hash chains don't contain these
        modCount++;

        unlink(bucket, p, previous);

        items[p] = null;  // tombstone
        size--;

        ensureCapacity(size);  // checks for excessive tomb stones or excessive capacity

        return true;
      }

      previous = p;
      p = nextIndices[p] - BASE;  // convert to zero-based position
    }
  }

  @Override
  public T remove(int index) {
    Objects.checkIndex(index, size);

    int p = getPhysicalIndex(index);

    @SuppressWarnings("unchecked")
    T removed = (T)items[p];

    removeAt(p);

    return removed;
  }

  @Override
  public boolean contains(Object o) {
    if(o == null) {
      return false;
    }

    if(mask == 0) {
      for(int i = 0; i < appendPosition; i++) {
        if(o.equals(items[i])) {
          return true;
        }
      }

      return false;
    }

    int bucket = o.hashCode() & mask;
    int p = headIndices[bucket] - BASE; // convert to zero-based position

    while(p != -BASE) {
      if(o.equals(items[p])) {
        return true;
      }

      p = nextIndices[p] - BASE;
    }

    return false;
  }

  @Override
  public Iterator<T> iterator() {
    return listIterator();
  }

  @Override
  public ListIterator<T> listIterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    Objects.checkIndex(index, size + 1);

    return new ListIterator<>() {
      private int logicalIndex = index;
      private int physicalIndex = getPhysicalIndex(index);
      private int expectedModCount = modCount;
      private int lastReturned = -1;  // physical position of the last returned element, -1 if none
      private boolean lastWasNext;

      @Override
      public boolean hasNext() {
        return logicalIndex < size;
      }

      @Override
      public T next() {
        checkModCount();

        if(!hasNext()) {
          throw new NoSuchElementException();
        }

        @SuppressWarnings("unchecked")
        T result = (T)items[physicalIndex];

        lastReturned = physicalIndex;
        lastWasNext = true;

        physicalIndex++;

        while(physicalIndex < appendPosition && items[physicalIndex] == null) {
          physicalIndex++;
        }

        logicalIndex++;

        return result;
      }

      @Override
      public boolean hasPrevious() {
        return logicalIndex > 0;
      }

      @Override
      public T previous() {
        checkModCount();

        if(!hasPrevious()) {
          throw new NoSuchElementException();
        }

        physicalIndex--;

        while(physicalIndex >= 0 && items[physicalIndex] == null) {
          physicalIndex--;
        }

        logicalIndex--;

        lastReturned = physicalIndex;
        lastWasNext = false;

        @SuppressWarnings("unchecked")
        T result = (T)items[physicalIndex];

        return result;
      }

      @Override
      public int nextIndex() {
        return logicalIndex;
      }

      @Override
      public int previousIndex() {
        return logicalIndex - 1;
      }

      @Override
      public void remove() {
        if(lastReturned < 0) {
          throw new IllegalStateException();
        }

        checkModCount();

        int p = lastReturned;

        lastReturned = -1;  // only one removal per next()/previous()

        if(lastWasNext) {
          logicalIndex--;
        }

        int appendBefore = appendPosition;

        removeAt(p);

        if(appendPosition != appendBefore) {
          physicalIndex = logicalIndex;  // fully compacted, physical index now equals logical index
        }
        else if(!lastWasNext) {
          physicalIndex = p + 1;  // previous() left the cursor on the removed slot, step off it

          while(physicalIndex < appendPosition && items[physicalIndex] == null) {
            physicalIndex++;
          }
        }
        // when the last call was next() and no compaction occurred, physicalIndex already points
        // past the removed element

        expectedModCount = modCount;
      }

      @Override
      public void set(T t) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void add(T t) {
        throw new UnsupportedOperationException();
      }

      private void checkModCount() {
        if(modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
      }
    };
  }

  // Convert logical to physical index
  private int getPhysicalIndex(int index) {
    int physicalIndex = index;
    int tombstoneCount = appendPosition - size;

    if(tombstoneCount > 0) {  // slow case when array contains one or more tombstones
      for(int i = 0; i <= physicalIndex && physicalIndex < appendPosition; i++) {
        if(items[i] == null) {
          physicalIndex++;  // tombstone encountered, expand index scan by one
        }
      }
    }

    return physicalIndex;
  }

  /**
   * Removes the element at the given physical position, unlinking it from its hash chain
   * (if one exists) and leaving a tombstone. Consolidates or shrinks the backing array
   * when too many tombstones have accumulated.
   *
   * @param p the zero-based physical position of the element to remove
   */
  private void removeAt(int p) {
    if(mask != 0) {
      int bucket = items[p].hashCode() & mask;
      int current = headIndices[bucket] - BASE;
      int previous = -BASE;

      while(current != p) {
        previous = current;
        current = nextIndices[current] - BASE;
      }

      unlink(bucket, p, previous);
    }

    items[p] = null;  // tombstone
    size--;
    modCount++;

    ensureCapacity(size);  // checks for excessive tomb stones or excessive capacity
  }

  /**
   * Unlinks the element at the given physical position from the hash chain of the given bucket.
   *
   * @param bucket the bucket the element belongs to
   * @param p the zero-based physical position of the element
   * @param previous the zero-based physical position of its predecessor, or {@code -BASE} if it is the head
   */
  private void unlink(int bucket, int p, int previous) {
    if(previous == -BASE) {
      headIndices[bucket] = nextIndices[p];
    }
    else {
      nextIndices[previous] = nextIndices[p];
    }

    if(tailIndices[bucket] == p + BASE) {
      tailIndices[bucket] = previous + BASE;  // if previous was -BASE, this becomes the 0 terminator automatically
    }
  }

  private void ensureCapacity(int requiredSize) {
    int tombstoneCount = appendPosition - size;

    if(tombstoneCount > items.length / 4) {
      boolean needsShrink = requiredSize <= items.length / 4 && items.length > MINIMUM_CAPACITY;
      boolean rehashImmediately = mask != 0 && !needsShrink;

      // Consolidate
      if(rehashImmediately) {
        Arrays.fill(headIndices, 0);
        Arrays.fill(tailIndices, 0);
        Arrays.fill(nextIndices, 0);
      }

      int p = 0;

      for(int i = 0; i < appendPosition; i++) {
        if(items[i] != null) {
          items[p] = items[i];  // shifts item back, filling in gaps

          if(rehashImmediately) {
            hashInsert(items[i], p);  // reinserts item to rebuild previously reset hash structures
          }

          p++;
        }
      }

      appendPosition = size;

      // Clear remaining slots:
      Arrays.fill(items, size, items.length, null);

      if(needsShrink) {

        // Shrink by half
        int newCapacity = items.length / 2;  // always power of 2

        items = Arrays.copyOf(items, newCapacity);

        if(newCapacity < HASH_THRESHOLD) {
          headIndices = null;
          tailIndices = null;
          nextIndices = null;
          mask = 0;
        }
        else {
          headIndices = new int[newCapacity / 2];
          tailIndices = new int[newCapacity / 2];
          nextIndices = new int[newCapacity];
          mask = newCapacity / 2 - 1;

          for(int i = 0; i < appendPosition; i++) {
            hashInsert(items[i], i);
          }
        }
      }
    }
    else if(requiredSize > items.length) {
      // Expand to double size
      int newCapacity = items.length * 2;  // always power of 2

      if(newCapacity < HASH_THRESHOLD) {
        items = Arrays.copyOf(items, newCapacity);
      }
      else {
        if(nextIndices == null) {
          headIndices = new int[newCapacity / 2];
        }
        else {
          headIndices = nextIndices;  // reuse old array, as it has the size needed
          Arrays.fill(headIndices, 0);  // clear reused array
        }

        tailIndices = new int[newCapacity / 2];
        nextIndices = new int[newCapacity];
        items = Arrays.copyOf(items, newCapacity);
        mask = newCapacity / 2 - 1;

        for(int i = 0; i < appendPosition; i++) {
          if(items[i] != null) {  // skip tombstones
            hashInsert(items[i], i);
          }
        }
      }
    }
  }

  private void hashInsert(Object item, int zeroBasedPosition) {
    int position = zeroBasedPosition + 1;
    int b = item.hashCode() & mask;
    int p = headIndices[b];

    if(p == 0) {
      headIndices[b] = position;
      tailIndices[b] = position;
    }
    else {
      nextIndices[tailIndices[b] - 1] = position;
      tailIndices[b] = position;
    }
  }
}
