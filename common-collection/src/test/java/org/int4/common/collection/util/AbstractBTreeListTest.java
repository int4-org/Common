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

package org.int4.common.collection.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class AbstractBTreeListTest {

  protected abstract List<String> createList();
  protected abstract List<String> createList(int nodeSize, int leafSize);
  protected abstract List<Integer> createIntList(int nodeSize, int leafSize);
  protected abstract List<Integer> createIntList(int nodeSize, int leafSize, Collection<Integer> values);
  protected abstract <T> List<T> copy(List<T> list);

  @Nested
  class Standard extends AbstractListTest {
    {
      stressTestOperations = 10000;
      exhaustiveTestRange = 9;
    }

    @Override
    protected List<String> createList() {
      return AbstractBTreeListTest.this.createList();
    }
  }

  @Nested
  class Tiny extends AbstractListTest {
    {
      stressTestOperations = 10000;
      exhaustiveTestRange = 9;
    }

    @Override
    protected List<String> createList() {
      return AbstractBTreeListTest.this.createList(8, 8);  // use smallest allowed sizes, more chance to break node rebalancing logic
    }
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8,     100",
    "8,   8,     1000",
    "8,   16,    1000",
    "16,  8,     1000",
    "32,  32,    1000",
    "32,  4096,  200000",
    "64,  4096,  500000"
  })
  void cloneShouldReturnIndependentCopy(int nodeSize, int leafSize, int size) {
    List<Integer> list = createIntList(nodeSize, leafSize);

    for(int i = 0; i < size; i++) {
      list.add(i);
    }

    List<Integer> clone = copy(list);

    assertThat(clone).isNotSameAs(list);
    assertThat(clone).hasSize(size);
    assertThat(clone).containsExactlyElementsOf(list);
    assertThat(clone).isEqualTo(list);

    list.remove(0);

    assertThat(clone).hasSize(size);
    assertThat(clone.get(0)).isEqualTo(0);
    assertThat(clone.get(size - 1)).isEqualTo(size - 1);
  }

  @Test
  void cloneShouldBeIndependentInBothDirections() {
    List<Integer> values = new ArrayList<>();

    for(int i = 0; i < 100; i++) {
      values.add(i);
    }

    List<Integer> list = createIntList(8, 8);
    list.addAll(values);

    List<Integer> clone = copy(list);
    List<Integer> expectedClone = new ArrayList<>(values);

    clone.set(50, -1);
    expectedClone.set(50, -1);
    clone.remove(0);
    expectedClone.remove(0);
    clone.add(10, 999);
    expectedClone.add(10, 999);

    assertThat(list).containsExactlyElementsOf(values);
    assertThat(clone).containsExactlyElementsOf(expectedClone);

    List<Integer> expectedList = new ArrayList<>(values);

    list.remove(50);
    expectedList.remove(50);
    list.set(0, 123);
    expectedList.set(0, 123);
    list.add(0, -5);
    expectedList.add(0, -5);

    assertThat(list).containsExactlyElementsOf(expectedList);
    assertThat(clone).containsExactlyElementsOf(expectedClone);
  }

  @Test
  void multipleClonesShouldBeIndependent() {
    List<Integer> list = createIntList(8, 8);

    for(int i = 0; i < 100; i++) {
      list.add(i);
    }

    List<Integer> clone1 = copy(list);
    List<Integer> clone2 = copy(list);

    clone1.set(0, -1);
    clone1.remove(99);
    clone1.add(100);
    clone2.remove(0);
    clone2.add(0, -2);

    assertThat(list).hasSize(100);
    assertThat(list.get(0)).isEqualTo(0);
    assertThat(clone1).hasSize(100);
    assertThat(clone1.get(0)).isEqualTo(-1);
    assertThat(clone2).hasSize(100);
    assertThat(clone2.get(0)).isEqualTo(-2);
  }

  @Test
  void cloneOfCloneShouldBeIndependent() {
    List<Integer> list = createIntList(8, 8);

    for(int i = 0; i < 100; i++) {
      list.add(i);
    }

    List<Integer> clone = copy(list);
    List<Integer> cloneOfClone = copy(clone);

    cloneOfClone.set(50, -1);

    assertThat(list.get(50)).isEqualTo(50);
    assertThat(clone.get(50)).isEqualTo(50);
    assertThat(cloneOfClone.get(50)).isEqualTo(-1);
  }

  @Test
  void iteratorSetShouldBeVisibleToPreviousAfterClone() {
    List<String> list = createList();

    list.addAll(List.of("a", "b", "c", "d"));

    List<String> clone = copy(list);
    ListIterator<String> iterator = list.listIterator();

    iterator.next();
    iterator.set("X");

    assertThat(iterator.previous()).isEqualTo("X");
    assertThat(list).containsExactly("X", "b", "c", "d");
    assertThat(clone).containsExactly("a", "b", "c", "d");
  }

  @Test
  void listSetDuringIterationShouldBeVisibleToIteratorAfterClone() {
    List<String> list = createList();

    list.addAll(List.of("a", "b", "c", "d"));

    List<String> clone = copy(list);
    ListIterator<String> iterator = list.listIterator();

    iterator.next();
    list.set(1, "NEW");

    assertThat(iterator.next()).isEqualTo("NEW");
    assertThat(clone).containsExactly("a", "b", "c", "d");
  }

  @Test
  void cloneDuringIterationShouldNotHideSubsequentSetFromIterator() {
    List<Integer> list = createIntList(8, 8);

    for(int i = 0; i < 100; i++) {
      list.add(i);
    }

    ListIterator<Integer> iterator = list.listIterator();

    for(int i = 0; i < 4; i++) {
      iterator.next();
    }

    List<Integer> clone = copy(list);

    list.set(4, -4);

    assertThat(iterator.next()).isEqualTo(-4);
    assertThat(clone.get(4)).isEqualTo(4);
  }

  @Test
  void clearOnCloneShouldNotAffectOriginal() {
    List<Integer> list = createIntList(8, 8);

    for(int i = 0; i < 100; i++) {
      list.add(i);
    }

    List<Integer> clone = copy(list);

    clone.clear();

    assertThat(clone).isEmpty();
    assertThat(list).hasSize(100);

    list.add(100);

    assertThat(clone).isEmpty();
    assertThat(list).hasSize(101);
  }

  @Test
  void clonesShouldBeIndependentlyMutableOnSeparateThreads() throws Exception {
    List<Integer> values = new ArrayList<>();
    List<Integer> original = createIntList(8, 8);

    for(int i = 0; i < 1000; i++) {
      original.add(i);
      values.add(i);
    }

    List<Integer> clone = copy(original);

    try(ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<?> originalTask = executor.submit(() -> {
        for(int i = 0; i < 500; i++) {
          original.add(values.size() + i);
        }
      });
      Future<?> cloneTask = executor.submit(() -> {
        for(int i = 0; i < 500; i++) {
          clone.remove(0);
        }
      });

      originalTask.get();
      cloneTask.get();
    }

    assertThat(original).hasSize(1500);
    assertThat(original.subList(0, values.size())).containsExactlyElementsOf(values);
    assertThat(clone).hasSize(500);
    assertThat(clone).containsExactlyElementsOf(values.subList(500, values.size()));
  }

  @Test
  void cloneShouldRemainUsableAfterRepeatedFrontRemovals() {
    List<Integer> list = createIntList(8, 8);

    for(int i = 0; i < 1000; i++) {
      list.add(i);
    }

    List<Integer> clone = copy(list);

    for(int i = 0; i < 500; i++) {
      clone.remove(0);
    }

    assertThat(clone).hasSize(500);
    assertThat(clone.get(0)).isEqualTo(500);
    assertThat(clone.get(499)).isEqualTo(999);
    assertThat(list).hasSize(1000);
  }

  @Test
  void listIteratorSetAfterCloneShouldKeepIteratorPosition() {
    List<String> list = createList(8, 8);

    list.addAll(List.of("A", "B", "C", "D"));

    List<String> clone = copy(list);
    ListIterator<String> iterator = clone.listIterator();

    assertThat(iterator.next()).isEqualTo("A");

    iterator.set("X");

    assertThat(iterator.previous()).isEqualTo("X");
    assertThat(iterator.next()).isEqualTo("X");
    assertThat(iterator.next()).isEqualTo("B");
    assertThat(iterator.next()).isEqualTo("C");
    assertThat(iterator.next()).isEqualTo("D");

    assertThat(clone).containsExactly("X", "B", "C", "D");
    assertThat(list).containsExactly("A", "B", "C", "D");
  }

  @Test
  void cloneAfterMixedMutationsShouldPreserveContents() {
    List<Integer> list = createIntList(8, 8);

    for(int i = 0; i < 200; i++) {
      list.add(i);
    }

    for(int i = 0; i < 50; i++) {
      list.remove(i % 2 == 0 ? 0 : list.size() - 1);
    }

    for(int i = 0; i < 20; i++) {
      list.add(list.size() / 2, 1000 + i);
    }

    for(int i = 0; i < 10; i++) {
      list.set(i, -i);
    }

    List<Integer> clone = copy(list);

    assertThat(clone).containsExactlyElementsOf(list);

    list.add(0, -999);
    clone.add(0, 999);

    assertThat(list.get(0)).isEqualTo(-999);
    assertThat(clone.get(0)).isEqualTo(999);
    assertThat(list).hasSize(clone.size());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65})
  void cloneShouldWorkAtCapacityBoundaries(int size) {
    List<Integer> list = createIntList(8, 8);

    for(int i = 0; i < size; i++) {
      list.add(i);
    }

    List<Integer> clone = copy(list);

    assertThat(clone).containsExactlyElementsOf(list);

    if(size > 0) {
      list.remove(0);

      assertThat(clone).hasSize(size);
      assertThat(clone.get(0)).isEqualTo(0);
      assertThat(clone.get(size - 1)).isEqualTo(size - 1);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 7, 8, 9, 15, 16, 17, 63, 64, 65, 127, 128, 129, 511, 512, 513})
  void shouldHandleRootTransitions(int size) {
    List<String> list = createList(8, 8);
    List<String> reference = new ArrayList<>();

    for(int i = 0; i < size; i++) {
      list.add("" + i);
      reference.add("" + i);
    }

    assertThat(list).containsExactlyElementsOf(reference);

    while(reference.size() > 3) {
      int index = reference.size() / 2;

      assertThat(list.remove(index)).isEqualTo(reference.remove(index));
    }

    assertThat(list).containsExactlyElementsOf(reference);

    List<String> clone = copy(list);

    assertThat(clone).containsExactlyElementsOf(list);
  }

  @Test
  void sequencedAccessorsShouldWorkOnMultiLeafLists() {
    List<String> list = createList(8, 8);
    List<String> reference = new ArrayList<>();

    for(int i = 0; i < 17; i++) {
      list.add("" + i);
      reference.add("" + i);
    }

    assertThat(list.getFirst()).isEqualTo("0");
    assertThat(list.getLast()).isEqualTo("16");
    assertThat(list.removeFirst()).isEqualTo(reference.removeFirst());
    assertThat(list.removeLast()).isEqualTo(reference.removeLast());

    assertThat(list).containsExactlyElementsOf(reference);
    assertThat(list.getFirst()).isEqualTo("1");
    assertThat(list.getLast()).isEqualTo("15");
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8",
    "16,  8",
    "8,   16",
    "32,  32"
  })
  void getFirstAndGetLastShouldWorkAtEveryHeightWhileGrowingAndShrinking(int nodeSize, int leafSize) {
    List<Integer> list = createIntList(nodeSize, leafSize);
    List<Integer> reference = new ArrayList<>();

    assertThatThrownBy(list::getFirst).isInstanceOf(NoSuchElementException.class);
    assertThatThrownBy(list::getLast).isInstanceOf(NoSuchElementException.class);

    for(int i = 0; i < 1500; i++) {
      list.add(i % 3 == 0 ? 0 : list.size(), i);
      reference.add(i % 3 == 0 ? 0 : reference.size(), i);

      assertThat(list.getFirst()).as("first at size %d", list.size()).isEqualTo(reference.getFirst());
      assertThat(list.getLast()).as("last at size %d", list.size()).isEqualTo(reference.getLast());
    }

    while(!reference.isEmpty()) {
      if(reference.size() % 2 == 0) {
        assertThat(list.removeFirst()).isEqualTo(reference.removeFirst());
      }
      else {
        assertThat(list.removeLast()).isEqualTo(reference.removeLast());
      }

      if(!reference.isEmpty()) {
        assertThat(list.getFirst()).as("first at size %d", list.size()).isEqualTo(reference.getFirst());
        assertThat(list.getLast()).as("last at size %d", list.size()).isEqualTo(reference.getLast());
      }
    }

    assertThatThrownBy(list::getFirst).isInstanceOf(NoSuchElementException.class);
    assertThatThrownBy(list::getLast).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void iteratorShouldCrossLeafBoundaries() {
    List<String> list = createList(8, 8);

    for(int i = 0; i < 17; i++) {
      list.add("" + i);
    }

    ListIterator<String> iterator = list.listIterator();

    for(int i = 0; i < 17; i++) {
      assertThat(iterator.next()).isEqualTo("" + i);
    }

    assertThat(iterator.hasNext()).isFalse();
    assertThat(iterator.nextIndex()).isEqualTo(17);
    assertThat(iterator.previousIndex()).isEqualTo(16);
    assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);

    for(int i = 16; i >= 0; i--) {
      assertThat(iterator.previous()).isEqualTo("" + i);
    }

    assertThat(iterator.hasPrevious()).isFalse();
    assertThat(iterator.previousIndex()).isEqualTo(-1);
    assertThatThrownBy(iterator::previous).isInstanceOf(NoSuchElementException.class);

    for(int index : new int[] {0, 1, 7, 8, 9, 15, 16, 17}) {
      ListIterator<String> at = list.listIterator(index);

      assertThat(at.nextIndex()).isEqualTo(index);
      assertThat(at.previousIndex()).isEqualTo(index - 1);

      if(index > 0) {
        assertThat(at.previous()).isEqualTo("" + (index - 1));
        assertThat(at.next()).isEqualTo("" + (index - 1));
      }
      if(index < 17) {
        assertThat(at.next()).isEqualTo("" + index);
      }
    }

    ListIterator<String> alternating = list.listIterator(8);

    assertThat(alternating.previous()).isEqualTo("7");
    assertThat(alternating.next()).isEqualTo("7");
    assertThat(alternating.next()).isEqualTo("8");
    assertThat(alternating.previous()).isEqualTo("8");
    assertThat(alternating.previous()).isEqualTo("7");

    List<String> edited = createList(8, 8);
    List<String> editedReference = new ArrayList<>();

    for(int i = 0; i < 17; i++) {
      edited.add("" + i);
      editedReference.add("" + i);
    }

    ListIterator<String> edit = edited.listIterator(8);
    ListIterator<String> editReference = editedReference.listIterator(8);

    assertThat(edit.previous()).isEqualTo(editReference.previous());
    assertThat(edit.next()).isEqualTo(editReference.next());

    edit.set("8a");
    editReference.set("8a");
    edit.remove();
    editReference.remove();
    edit.add("8b");
    editReference.add("8b");

    assertThat(edited).containsExactlyElementsOf(editedReference);
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8",
    "16,  64",
    "32,  4096"
  })
  void iteratorSetShouldNotLeakIntoCloneInEitherDirection(int nodeSize, int leafSize) {
    int size = 500;
    List<Integer> forward = createSequence(size, nodeSize, leafSize);
    List<Integer> forwardClone = copy(forward);

    for(ListIterator<Integer> iterator = forward.listIterator(); iterator.hasNext();) {
      int value = iterator.next();

      iterator.set(-value - 1);
    }

    List<Integer> backward = createSequence(size, nodeSize, leafSize);
    List<Integer> backwardClone = copy(backward);

    for(ListIterator<Integer> iterator = backward.listIterator(size); iterator.hasPrevious();) {
      int value = iterator.previous();

      iterator.set(-value - 1);
    }

    for(int i = 0; i < size; i++) {
      assertThat(forwardClone.get(i)).as("forward clone at %d", i).isEqualTo(i);
      assertThat(forward.get(i)).as("forward at %d", i).isEqualTo(-i - 1);
      assertThat(backwardClone.get(i)).as("backward clone at %d", i).isEqualTo(i);
      assertThat(backward.get(i)).as("backward at %d", i).isEqualTo(-i - 1);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8",
    "16,  64",
    "32,  4096"
  })
  void iteratorSetOnCloneShouldNotLeakIntoOriginalOrCloneOfClone(int nodeSize, int leafSize) {
    int size = 500;
    List<Integer> list = createSequence(size, nodeSize, leafSize);
    List<Integer> clone = copy(list);
    List<Integer> cloneOfClone = copy(clone);

    for(ListIterator<Integer> iterator = clone.listIterator(); iterator.hasNext();) {
      iterator.next();
      iterator.set(7);
    }

    for(int i = 0; i < size; i++) {
      assertThat(list.get(i)).as("original at %d", i).isEqualTo(i);
      assertThat(cloneOfClone.get(i)).as("clone of clone at %d", i).isEqualTo(i);
      assertThat(clone.get(i)).as("clone at %d", i).isEqualTo(7);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8",
    "16,  64",
    "32,  4096"
  })
  void cloneTakenInTheMiddleOfIterationShouldKeepEarlierSetsOnly(int nodeSize, int leafSize) {
    int size = 500;
    List<Integer> list = createSequence(size, nodeSize, leafSize);
    ListIterator<Integer> iterator = list.listIterator();
    List<Integer> clone = null;

    for(int i = 0; i < size; i++) {
      if(i == size / 3) {
        clone = copy(list);
      }

      assertThat(iterator.next()).isEqualTo(i);

      iterator.set(-i - 1);
    }

    assert clone != null;

    for(int i = 0; i < size; i++) {
      assertThat(clone.get(i)).as("clone at %d", i).isEqualTo(i < size / 3 ? -i - 1 : i);
      assertThat(list.get(i)).as("list at %d", i).isEqualTo(-i - 1);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8,     false",
    "8,   8,     true",
    "16,  64,    false",
    "16,  64,    true",
    "32,  4096,  true"
  })
  void listSetOfUpcomingElementsShouldBeSeenByIterator(int nodeSize, int leafSize, boolean cloned) {
    int size = 500;
    List<Integer> list = createSequence(size, nodeSize, leafSize);
    List<Integer> clone = cloned ? copy(list) : null;
    Iterator<Integer> iterator = list.iterator();

    for(int i = 0; i < size; i++) {
      if(i + 1 < size) {
        list.set(i + 1, -i - 2);
      }

      assertThat(iterator.next()).as("at %d", i).isEqualTo(i == 0 ? 0 : -i - 1);
    }

    if(clone != null) {
      for(int i = 0; i < size; i++) {
        assertThat(clone.get(i)).as("clone at %d", i).isEqualTo(i);
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8",
    "16,  64",
    "32,  4096"
  })
  void previousShouldSeeListSetAfterClone(int nodeSize, int leafSize) {
    int size = 500;
    List<Integer> list = createSequence(size, nodeSize, leafSize);
    List<Integer> clone = copy(list);
    ListIterator<Integer> iterator = list.listIterator(size / 2);

    iterator.previous();
    list.set(size / 2 - 2, 4242);

    assertThat(iterator.previous()).isEqualTo(4242);
    assertThat(clone.get(size / 2 - 2)).isEqualTo(size / 2 - 2);
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8",
    "16,  64",
    "32,  4096"
  })
  void replaceAllAndSortOnCloneShouldLeaveOriginalUntouched(int nodeSize, int leafSize) {
    int size = 500;
    List<Integer> list = createSequence(size, nodeSize, leafSize);
    List<Integer> clone = copy(list);

    clone.replaceAll(x -> x + 1000);
    clone.sort(Comparator.reverseOrder());

    for(int i = 0; i < size; i++) {
      assertThat(list.get(i)).as("original at %d", i).isEqualTo(i);
      assertThat(clone.get(i)).as("clone at %d", i).isEqualTo(size - 1 - i + 1000);
    }
  }

  @Test
  void structuralChangeShouldStillThrowConcurrentModificationAfterSets() {
    List<Integer> list = createSequence(500, 8, 8);
    Iterator<Integer> iterator = list.iterator();

    iterator.next();
    list.set(5, 1);
    list.add(9);

    assertThatThrownBy(iterator::next).isInstanceOf(ConcurrentModificationException.class);
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8",
    "16,  8",
    "8,   16",
    "32,  32"
  })
  void shouldCreateListsFromCollectionsOfAnySize(int nodeSize, int leafSize) {
    List<Integer> values = new ArrayList<>();

    for(int size = 0; size <= 600; size++) {
      List<Integer> list = createIntList(nodeSize, leafSize, values);

      assertSameContent(list, values, "created with size " + size);

      if(size % 37 == 0) {
        List<Integer> clone = copy(list);
        List<Integer> expected = new ArrayList<>(values);

        list.add(size / 2, -1);
        expected.add(size / 2, -1);

        if(size > 0) {
          assertThat(list.remove(0)).isEqualTo(expected.remove(0));
        }

        assertSameContent(list, expected, "modified after creation with size " + size);
        assertSameContent(clone, values, "clone of list created with size " + size);
      }

      values.add(size);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8,     100000",
    "32,  4096,  300000",
    "128, 1024,  300000"
  })
  void shouldCreateLargeListsFromCollections(int nodeSize, int leafSize, int size) {
    List<Integer> values = new ArrayList<>();

    for(int i = 0; i < size; i++) {
      values.add(i);
    }

    List<Integer> list = createIntList(nodeSize, leafSize, values);

    assertThat(list).as("created").isEqualTo(values);

    list.add(size / 2, -1);
    list.remove(0);
    list.remove(list.size() - 1);

    values.add(size / 2, -1);
    values.remove(0);
    values.remove(values.size() - 1);

    assertThat(list).as("modified").isEqualTo(values);
  }

  @Test
  void shouldNotBeAffectedByLaterChangesToTheCollectionItWasCreatedFrom() {
    List<Integer> values = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
    List<Integer> list = createIntList(8, 8, values);

    values.set(0, 100);
    values.add(13);
    list.add(0, 99);

    assertThat(list).containsExactly(99, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    assertThat(values).hasSize(13).startsWith(100, 2);
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8,   0",
    "8,   8,   1",
    "8,   8,   2",
    "8,   8,   3",
    "8,   16,  0",
    "16,  8,   1",
    "16,  8,   2",
    "16,  16,  3",
    "32,  32,  0",
    "32,  32,  1"
  })
  void shouldMatchReferenceListUnderRandomOperationsIncludingClones(int nodeSize, int leafSize, int mode) {
    for(long seed = 1; seed <= 3; seed++) {
      runRandomOperations(seed, nodeSize, leafSize, mode, 2000);
    }
  }

  /*
   * Applies random operations to a list and an ArrayList and compares them. Modes: 0 is
   * uniformly random positions, 1 alternates between growing and shrinking phases, 2 only
   * touches the ends (stack and queue behavior), 3 only touches the middle. Roughly 8% of the
   * operations are special: cloning, mutating a clone, set, subList clear, addAll and a burst
   * of ListIterator operations.
   */
  private void runRandomOperations(long seed, int nodeSize, int leafSize, int mode, int operations) {
    Random random = new Random(seed);
    List<Integer> list = createIntList(nodeSize, leafSize);
    List<Integer> reference = new ArrayList<>();
    List<List<Integer>> clones = new ArrayList<>();
    List<List<Integer>> cloneReferences = new ArrayList<>();
    int counter = 0;

    for(int i = 0; i < operations; i++) {
      int op = random.nextInt(100);
      int size = reference.size();
      boolean grow = mode == 1 ? (i / (operations / 6)) % 2 == 0 : op < 52;

      if(op >= 92) {
        if(op < 94 && clones.size() < 3) {
          clones.add(copy(list));
          cloneReferences.add(new ArrayList<>(reference));
        }
        else if(op < 96 && !clones.isEmpty()) {
          int k = random.nextInt(clones.size());
          List<Integer> clone = clones.get(k);
          List<Integer> cloneReference = cloneReferences.get(k);

          if(random.nextBoolean() || cloneReference.isEmpty()) {
            int index = random.nextInt(cloneReference.size() + 1);

            clone.add(index, ++counter);
            cloneReference.add(index, counter);
          }
          else {
            int index = random.nextInt(cloneReference.size());

            assertThat(clone.remove(index)).isEqualTo(cloneReference.remove(index));
          }

          assertSameContent(clone, cloneReference, "clone, seed " + seed + " step " + i);
        }
        else if(op < 97 && size > 0) {
          int index = random.nextInt(size);

          assertThat(list.set(index, ++counter)).isEqualTo(reference.set(index, counter));
        }
        else if(op < 98 && size > 1) {
          int from = random.nextInt(size);
          int to = from + random.nextInt(size - from + 1);

          list.subList(from, to).clear();
          reference.subList(from, to).clear();
        }
        else if(op < 99) {
          int index = random.nextInt(size + 1);
          List<Integer> values = new ArrayList<>();

          for(int k = random.nextInt(3 * leafSize); k > 0; k--) {
            values.add(++counter);
          }

          list.addAll(index, values);
          reference.addAll(index, values);
        }
        else {
          counter = randomIteratorOperations(random, list, reference, counter);
        }
      }
      else if(grow || size == 0) {
        int index = switch(mode) {
          case 2 -> random.nextBoolean() ? 0 : size;
          case 3 -> size / 2;
          default -> random.nextInt(size + 1);
        };

        list.add(index, ++counter);
        reference.add(index, counter);
      }
      else {
        int index = switch(mode) {
          case 2 -> random.nextBoolean() ? 0 : size - 1;
          case 3 -> size / 2;
          default -> random.nextInt(size);
        };

        assertThat(list.remove(index)).as("seed %d step %d", seed, i).isEqualTo(reference.remove(index));
      }

      if(i % 25 == 0 || reference.size() < 40) {
        assertSameContent(list, reference, "list, seed " + seed + " step " + i);
      }
    }

    assertSameContent(list, reference, "list, seed " + seed + " at the end");

    for(int k = 0; k < clones.size(); k++) {
      assertSameContent(clones.get(k), cloneReferences.get(k), "clone " + k + ", seed " + seed + " at the end");
    }

    assertThat(list).isEqualTo(reference);
    assertThat(list.hashCode()).isEqualTo(reference.hashCode());
  }

  private static int randomIteratorOperations(Random random, List<Integer> list, List<Integer> reference, int counter) {
    ListIterator<Integer> actual = list.listIterator(random.nextInt(reference.size() + 1));
    ListIterator<Integer> expected = reference.listIterator(actual.nextIndex());
    int value = counter;

    for(int k = 0; k < 40; k++) {
      switch(random.nextInt(6)) {
        case 0 -> {
          if(expected.hasNext()) {
            assertThat(actual.next()).isEqualTo(expected.next());
          }
        }
        case 1 -> {
          if(expected.hasPrevious()) {
            assertThat(actual.previous()).isEqualTo(expected.previous());
          }
        }
        case 2 -> assertSameOutcome(expected::remove, actual::remove);
        case 3 -> {
          actual.add(++value);
          expected.add(value);
        }
        case 4 -> {
          int newValue = ++value;

          assertSameOutcome(() -> expected.set(newValue), () -> actual.set(newValue));
        }
        default -> assertThat(actual.nextIndex()).isEqualTo(expected.nextIndex());
      }
    }

    return value;
  }

  /*
   * Runs the same operation on the reference and the tested list, which must either both
   * succeed or both throw IllegalStateException (an illegal remove/set after add/remove).
   */
  private static void assertSameOutcome(Runnable expected, Runnable actual) {
    boolean expectedFailed = false;

    try {
      expected.run();
    }
    catch(IllegalStateException e) {
      expectedFailed = true;
    }

    if(expectedFailed) {
      assertThatThrownBy(actual::run).isInstanceOf(IllegalStateException.class);
    }
    else {
      actual.run();
    }
  }

  private static void assertSameContent(List<Integer> actual, List<Integer> expected, String description) {
    assertThat(actual).as(description).hasSize(expected.size());
    assertThat(actual).as(description).containsExactlyElementsOf(expected);

    int stride = 1 + expected.size() / 50;

    for(int i = 0; i < expected.size(); i += stride) {
      assertThat(actual.get(i)).as("%s, get(%d)", description, i).isEqualTo(expected.get(i));
    }

    if(!expected.isEmpty()) {
      assertThat(actual.getFirst()).as(description).isEqualTo(expected.getFirst());
      assertThat(actual.getLast()).as(description).isEqualTo(expected.getLast());
    }
  }

  private List<Integer> createSequence(int size, int nodeSize, int leafSize) {
    List<Integer> list = createIntList(nodeSize, leafSize);

    for(int i = 0; i < size; i++) {
      list.add(i);
    }

    return list;
  }

  @Test
  void constructorShouldRejectInvalidSizes() {
    assertThatThrownBy(() -> createList(7, 8)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> createList(8, 7)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> createList(8, 1048577)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> createList(1048577, 8)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> createList(8, 13)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> createList(13, 8)).isInstanceOf(IllegalArgumentException.class);
  }
}
