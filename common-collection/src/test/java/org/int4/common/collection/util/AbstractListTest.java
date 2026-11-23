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
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public abstract class AbstractListTest {
  private List<String> testList = createList();

  protected abstract List<String> createList();
  protected int stressTestOperations = 100000;
  protected int exhaustiveTestRange = 10;

  static {
    Assertions.setMaxStackTraceElementsDisplayed(2000);
  }

  protected boolean supportsRemoveIndex() {
    return true;
  }

  protected boolean supportsAddIndexT() {
    return true;
  }

  protected boolean supportsSetIndexT() {
    return true;
  }

  protected boolean supportsListIterator() {
    return true;
  }

  protected boolean supportsListIteratorAdd() {
    return true;
  }

  protected boolean supportsListIteratorSet() {
    return true;
  }

  protected boolean supportsNullElements() {
    return true;
  }

  class InvariantTests {
    Class<? extends Exception> expectedRemoveException = supportsRemoveIndex() ? IndexOutOfBoundsException.class : UnsupportedOperationException.class;
    Class<? extends Exception> expectedAddIndexTException = supportsAddIndexT() ? IndexOutOfBoundsException.class : UnsupportedOperationException.class;

    @Test
    void accessingIllegalIndicesShouldThrowIndexOutOfBoundsException() {
      assertThatThrownBy(() -> testList.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> testList.remove(-1)).isInstanceOf(expectedRemoveException);
      assertThatThrownBy(() -> testList.add(-1, "")).isInstanceOf(expectedAddIndexTException);
      assertThatThrownBy(() -> testList.listIterator(-1)).isInstanceOf(IndexOutOfBoundsException.class);

      int size = testList.size();

      assertThatThrownBy(() -> testList.get(size)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> testList.remove(size)).isInstanceOf(expectedRemoveException);
      assertThatThrownBy(() -> testList.add(size + 1, "")).isInstanceOf(expectedAddIndexTException);
      assertThatThrownBy(() -> testList.listIterator(size + 1)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void listIteratorShouldReturnExpectedNumberOfElements() {
      ListIterator<String> iterator = testList.listIterator();
      int i = 0;

      assertThat(iterator.hasPrevious()).isFalse();
      assertThatThrownBy(() -> iterator.previous()).isInstanceOf(NoSuchElementException.class);

      while(iterator.hasNext()) {
        String element = iterator.next();

        assertThat(element).isEqualTo(testList.get(i++));
      }

      assertThat(i).isEqualTo(testList.size());
      assertThatThrownBy(() -> iterator.next()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void listIteratorShouldReturnExpectedNumberOfElementsInReverse() {
      ListIterator<String> iterator = testList.listIterator(testList.size());
      int i = testList.size();

      assertThat(iterator.hasNext()).isFalse();
      assertThatThrownBy(() -> iterator.next()).isInstanceOf(NoSuchElementException.class);

      while(iterator.hasPrevious()) {
        String element = iterator.previous();

        assertThat(element).isEqualTo(testList.get(--i));
      }

      assertThat(i).isEqualTo(0);
      assertThatThrownBy(() -> iterator.previous()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void indexOfShouldNotFindMissingElement() {
      assertThat(testList.indexOf("missing")).isEqualTo(-1);
    }

    @Test
    void clearShouldMakeListEmpty() {
      testList.clear();

      assertThat(testList).isEmpty();
    }

    @Test
    void containsShouldBeFalseForMissingElement() {
      assertThat(testList.contains("Z")).isFalse();
    }
  }

  @Nested
  class WhenEmpty extends InvariantTests {

    @Test
    void sizeShouldBe0() {
      assertThat(testList.size()).isEqualTo(0);
    }

    @Test
    void isEmptyShouldReturnTrue() {
      assertThat(testList.isEmpty()).isTrue();
    }

    @Test
    void getFirstShouldThrowNoSuchElementException() {
      assertThatThrownBy(() -> testList.getFirst()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getLastShouldThrowNoSuchElementException() {
      assertThatThrownBy(() -> testList.getLast()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void removeFirstShouldThrowNoSuchElementException() {
      assertThatThrownBy(() -> testList.removeFirst()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void removeLastShouldThrowNoSuchElementException() {
      assertThatThrownBy(() -> testList.removeLast()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void indexOfShouldNotFindAnything() {
      assertThat(testList.indexOf("")).isEqualTo(-1);
    }

    @Test
    void iteratorsShouldHaveNoElementsToIterate() {
      assertThat(testList.iterator().hasNext()).isFalse();
      assertThatThrownBy(() -> testList.iterator().next()).isInstanceOf(NoSuchElementException.class);

      assertThat(testList.listIterator().hasNext()).isFalse();
      assertThatThrownBy(() -> testList.listIterator().next()).isInstanceOf(NoSuchElementException.class);

      assertThat(testList.listIterator().hasPrevious()).isFalse();
      assertThatThrownBy(() -> testList.listIterator().previous()).isInstanceOf(NoSuchElementException.class);
    }
  }

  @Nested
  class WhenHasOneElement extends InvariantTests {
    private static String ELEMENT = "S";
    private static String ANOTHER_ELEMENT = "T";

    {
      testList.add(ELEMENT);
    }

    @Test
    void sizeShouldBe1() {
      assertThat(testList.size()).isEqualTo(1);
    }

    @Test
    void isEmptyShouldReturnFalse() {
      assertThat(testList.isEmpty()).isFalse();
    }

    @Test
    void indexOfShouldFindTheElement() {
      assertThat(testList.indexOf(ELEMENT)).isEqualTo(0);
    }

    @Test
    void get0ShouldReturnElement() {
      assertThat(testList.get(0)).isEqualTo(ELEMENT);
    }

    @Test
    void set0ShouldReplaceElement() {
      assumeTrue(supportsSetIndexT());

      assertThat(testList.set(0, ANOTHER_ELEMENT)).isEqualTo(ELEMENT);
      assertThat(testList.get(0)).isEqualTo(ANOTHER_ELEMENT);
    }

    @Test
    void getFirstShouldReturnElement() {
      assertThat(testList.getFirst()).isEqualTo(ELEMENT);
    }

    @Test
    void getLastShouldReturnElement() {
      assertThat(testList.getLast()).isEqualTo(ELEMENT);
    }

    @Test
    void removeFirstShouldReturnElement() {
      assumeTrue(supportsRemoveIndex());

      assertThat(testList.removeFirst()).isEqualTo(ELEMENT);
      assertThat(testList).isEmpty();
    }

    @Test
    void removeLastShouldReturnElement() {
      assumeTrue(supportsRemoveIndex());

      assertThat(testList.removeLast()).isEqualTo(ELEMENT);
      assertThat(testList).isEmpty();
    }

    @Test
    void remove0ShouldReturnElement() {
      assumeTrue(supportsRemoveIndex());

      assertThat(testList.remove(0)).isEqualTo(ELEMENT);
      assertThat(testList).isEmpty();
    }

    @Test
    void containsShouldBeTrueForExistingElement() {
      assertThat(testList.contains(ELEMENT)).isTrue();
    }
  }

  @Nested
  class ListContractTests {

    @Test
    void removeShouldRemoveOnlyFirstOccurrence() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "A", "C", "A"));

      assertThat(list.remove("A")).isTrue();
      assertThat(list).containsExactly("B", "A", "C", "A");

      assertThat(list.remove("A")).isTrue();
      assertThat(list).containsExactly("B", "C", "A");

      assertThat(list.remove("A")).isTrue();
      assertThat(list).containsExactly("B", "C");

      assertThat(list.remove("A")).isFalse();
      assertThat(list).containsExactly("B", "C");
    }

    @Test
    void indexOfAndLastIndexOfShouldReportFirstAndLastOccurrence() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "A", "C", "A"));

      assertThat(list.indexOf("A")).isEqualTo(0);
      assertThat(list.lastIndexOf("A")).isEqualTo(4);
      assertThat(list.indexOf("B")).isEqualTo(1);
      assertThat(list.lastIndexOf("B")).isEqualTo(1);
      assertThat(list.indexOf("Z")).isEqualTo(-1);
      assertThat(list.lastIndexOf("Z")).isEqualTo(-1);
    }

    @Test
    void listIteratorShouldPositionAtEveryIndex() {
      assumeTrue(supportsListIterator());

      List<String> list = createList();
      List<String> reference = new ArrayList<>();

      list.addAll(List.of("A", "B", "C", "D", "E"));
      reference.addAll(List.of("A", "B", "C", "D", "E"));

      list.remove("B");
      reference.remove("B");
      list.remove("D");
      reference.remove("D");

      for(int i = 0; i <= reference.size(); i++) {
        ListIterator<String> iterator = list.listIterator(i);
        ListIterator<String> ref = reference.listIterator(i);

        assertThat(iterator.nextIndex()).isEqualTo(ref.nextIndex());
        assertThat(iterator.previousIndex()).isEqualTo(ref.previousIndex());
        assertThat(iterator.hasNext()).isEqualTo(ref.hasNext());
        assertThat(iterator.hasPrevious()).isEqualTo(ref.hasPrevious());

        while(ref.hasNext()) {
          assertThat(iterator.next()).isEqualTo(ref.next());
        }

        assertThat(iterator.hasNext()).isFalse();

        while(ref.hasPrevious()) {
          assertThat(iterator.previous()).isEqualTo(ref.previous());
        }

        assertThat(iterator.hasPrevious()).isFalse();
      }
    }

    @Test
    void listIteratorRemoveShouldRemoveLastReturnedElement() {
      assumeTrue(supportsListIterator());

      // after next()
      {
        List<String> list = createList();
        list.addAll(List.of("A", "B", "C", "D"));

        ListIterator<String> iterator = list.listIterator();
        assertThat(iterator.next()).isEqualTo("A");

        iterator.remove();

        assertThat(list).containsExactly("B", "C", "D");
      }

      // after previous()
      {
        List<String> list = createList();
        list.addAll(List.of("A", "B", "C", "D"));

        ListIterator<String> iterator = list.listIterator(list.size());
        assertThat(iterator.previous()).isEqualTo("D");

        iterator.remove();

        assertThat(list).containsExactly("A", "B", "C");
      }

      // remove without next()/previous()
      {
        List<String> list = createList();
        list.addAll(List.of("A", "B"));

        assertThatThrownBy(() -> list.listIterator().remove()).isInstanceOf(IllegalStateException.class);
      }

      // two removes in a row
      {
        List<String> list = createList();
        list.addAll(List.of("A", "B"));

        ListIterator<String> iterator = list.listIterator();
        iterator.next();
        iterator.remove();

        assertThatThrownBy(iterator::remove).isInstanceOf(IllegalStateException.class);
      }

      // removing while iterating keeps the remaining order intact
      {
        List<String> list = createList();
        List<String> reference = new ArrayList<>();

        for(int i = 0; i < 100; i++) {
          list.add("" + i);
          reference.add("" + i);
        }

        ListIterator<String> iterator = list.listIterator();
        ListIterator<String> ref = reference.listIterator();

        while(ref.hasNext()) {
          assertThat(iterator.next()).isEqualTo(ref.next());

          if(ref.nextIndex() % 3 == 0) {
            iterator.remove();
            ref.remove();
          }
        }

        assertThat(list).containsExactlyElementsOf(reference);
      }

      // removing backwards keeps the cursor consistent
      {
        List<String> list = createList();
        list.addAll(List.of("A", "B", "C", "D", "E"));

        ListIterator<String> iterator = list.listIterator(list.size());
        assertThat(iterator.previous()).isEqualTo("E");

        iterator.remove();

        assertThat(iterator.previous()).isEqualTo("D");
        assertThat(list).containsExactly("A", "B", "C", "D");
      }
    }

    @Test
    void nullElementsShouldBeHandledAsConfigured() {
      List<String> list = createList();

      if(supportsNullElements()) {
        list.add(null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isNull();
        assertThat(list.contains(null)).isTrue();
        assertThat(list.indexOf(null)).isEqualTo(0);
        assertThat(list.remove(null)).isTrue();
        assertThat(list).isEmpty();
      }
      else {
        assertThatThrownBy(() -> list.add(null)).isInstanceOf(NullPointerException.class);
        assertThat(list.contains(null)).isFalse();
        assertThat(list.remove(null)).isFalse();
      }
    }
  }

  @Nested
  class ExhaustiveTests {
    private static final String[] STRINGS = new String[100];

    static {
      for(int i = 0; i < STRINGS.length; i++) {
        STRINGS[i] = "" + i;
      }
    }

    @Test
    void allAddRemoveOrders() {
      assumeTrue(supportsAddIndexT());
      assumeTrue(supportsRemoveIndex());

      int size = Math.min(exhaustiveTestRange, STRINGS.length);

      generateRemoveAndInsertOrders(size, new ArrayList<>(), list -> {
        List<String> built = createList();
        List<String> reference = new ArrayList<>();

        for(int i = 0; i < list.size(); i++) {
          int index = list.get(i);

          if(index < 0) {
            reference.remove(~index);
            built.remove(~index);
          }
          else {
            String s = STRINGS[i];

            reference.add(index, s);
            built.add(index, s);
          }
        }

        assertThat(built)
          .as(() -> "Add/Removes: " + list)
          .isEqualTo(reference);
      });
    }

    @Test
    void allRemoveOrders() {
      List<String> data = new ArrayList<>();
      int size = Math.min(exhaustiveTestRange, STRINGS.length);

      for(int i = 0; i < size; i++) {
        data.add(STRINGS[i]);
      }

      generateRemoveOrders(size, new ArrayList<>(), list -> {
        List<String> built = createList();
        List<String> reference = new ArrayList<>(data);

        built.addAll(data);

        for(int i = 0; i < list.size(); i++) {
          int index = list.get(i);

          assertThat(reference.remove(index)).isEqualTo(built.remove(index));
        }

        assertThat(built.size()).isEqualTo(0);
        assertThat(built)
          .as(() -> "Add/Removes: " + list)
          .isEqualTo(reference);
      });
    }

    static void generateRemoveAndInsertOrders(int n, List<Integer> current, Consumer<List<Integer>> callback) {
      callback.accept(current);

      if(current.size() == n) {
        return;
      }

      for(int i = 0; i <= current.size(); i++) {
        int expectedSize = current.stream().mapToInt(e -> e >= 0 ? 1 : -1).sum();

        if(i <= expectedSize) {
          current.add(i);
          generateRemoveAndInsertOrders(n, current, callback);
          current.removeLast();
        }

        if(i < expectedSize) {
          current.add(~i);
          generateRemoveAndInsertOrders(n, current, callback);
          current.removeLast();
        }
      }
    }

    static void generateRemoveOrders(int n, List<Integer> current, Consumer<List<Integer>> callback) {
      if(current.size() == n) {
        callback.accept(current);
        return;
      }

      for(int i = 0; i < n - current.size(); i++) {
        current.add(i);
        generateRemoveOrders(n, current, callback);
        current.removeLast();
      }
    }

    enum Action { NEXT, PREVIOUS }

    @Test
    void allIteratorNextPreviousOrders() {
      List<String> list = createList();
      List<String> reference = new ArrayList<>(List.of("1", "2", "3", "4"));

      list.addAll(reference);

      generateIteratorActions(15, new ArrayList<>(), actions -> {
        ListIterator<String> refIter = reference.listIterator();
        ListIterator<String> listIter = list.listIterator();

        for(int i = 0; i < actions.size(); i++) {
          Action action = actions.get(i);

          switch(action) {
            case NEXT -> {
              try {
                String s = refIter.next();

                assertThat(s).isEqualTo(listIter.next());
              }
              catch(NoSuchElementException e) {
                assertThatThrownBy(() -> listIter.next()).isInstanceOf(NoSuchElementException.class);
              }
            }
            case PREVIOUS -> {
              try {
                String s = refIter.previous();

                assertThat(s).isEqualTo(listIter.previous());
              }
              catch(NoSuchElementException e) {
                assertThatThrownBy(() -> listIter.previous()).isInstanceOf(NoSuchElementException.class);
              }
            }
          }
        }
      });
    }

    static void generateIteratorActions(int n, List<Action> current, Consumer<List<Action>> callback) {
      if(current.size() == n) {
        callback.accept(current);

        return;
      }

      for(int i = 0; i < Action.values().length; i++) {
        current.add(Action.values()[i]);
        generateIteratorActions(n, current, callback);
        current.removeLast();
      }
    }
  }

  @Nested
  class IteratorConcurrencyTests {
    {
      testList.addAll(List.of("A", "B", "C", "D", "E"));
    }

    @Test
    void nextShouldThrowConcurrentModificationExceptionWhenModifiedDuringIteration() {
      Iterator<String> iterator = testList.iterator();

      assertThat(iterator.next()).isEqualTo("A");

      testList.add("F");

      assertThatThrownBy(() -> iterator.next()).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void previousShouldThrowConcurrentModificationExceptionWhenModifiedDuringIteration() {
      ListIterator<String> iterator = testList.listIterator(5);

      assertThat(iterator.previous()).isEqualTo("E");

      testList.clear();

      assertThatThrownBy(() -> iterator.previous()).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void setShouldThrowConcurrentModificationExceptionWhenModifiedDuringIteration() {
      assumeTrue(supportsListIteratorSet());

      ListIterator<String> iterator = testList.listIterator();

      assertThat(iterator.next()).isEqualTo("A");

      testList.remove("E");

      assertThatThrownBy(() -> iterator.set("a")).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void secondIteratorRemoveShouldInvalidateFirstIterator() {
      Iterator<String> first = testList.iterator();
      Iterator<String> second = testList.iterator();

      assertThat(first.next()).isEqualTo("A");

      second.next();
      second.remove();

      assertThatThrownBy(first::next).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void setShouldNotInvalidateExistingIterator() {
      assumeTrue(supportsSetIndexT());

      Iterator<String> iterator = testList.iterator();

      assertThat(iterator.next()).isEqualTo("A");

      testList.set(0, "Z");

      assertThat(iterator.next()).isEqualTo("B");
    }
  }

  @Nested
  class IteratorStressTests {
    private static int OPERATIONS = 100000;

    private Random random = new Random(42); // Fixed seed for reproducibility
    private List<String> referenceList = new ArrayList<>();

    @Test
    void randomIteratorActions() {
      assumeTrue(supportsListIterator());

      if(!supportsListIteratorAdd()) {
        // seed list if list iterator add is unsupported
        for(int i = 0; i < OPERATIONS / 50; i++) {
          testList.add("" + i);
          referenceList.add("" + i);
        }
      }

      ListIterator<String> refIter = referenceList.listIterator();
      ListIterator<String> iter = testList.listIterator();

      for(int i = 0; i < OPERATIONS; i++) {
        assertThat(referenceList).isEqualTo(testList);
        assertThat(refIter.hasNext()).isEqualTo(iter.hasNext());
        assertThat(refIter.hasPrevious()).isEqualTo(iter.hasPrevious());
        assertThat(refIter.nextIndex()).isEqualTo(iter.nextIndex());
        assertThat(refIter.previousIndex()).isEqualTo(iter.previousIndex());

        int operation = random.nextInt(50);

        if(operation < 10) {
          try {
            String s = refIter.next();

            assertThat(s).isEqualTo(iter.next());
          }
          catch(NoSuchElementException e) {
            assertThatThrownBy(() -> iter.next()).isInstanceOf(NoSuchElementException.class);
          }
        }
        else if(operation < 20) {
          try {
            String s = refIter.previous();

            assertThatNoException().isThrownBy(() -> assertThat(iter.previous()).isEqualTo(s));
          }
          catch(NoSuchElementException e) {
            assertThatThrownBy(() -> iter.previous()).isInstanceOf(NoSuchElementException.class);
          }
        }
        else if(operation < 30 && supportsListIteratorAdd()) {
          refIter.add("" + i);
          iter.add("" + i);
        }
        else if(operation < 40) {
          try {
            refIter.remove();
            assertThatNoException().isThrownBy(() -> iter.remove());
          }
          catch(IllegalStateException e) {
            assertThatThrownBy(() -> iter.remove()).isInstanceOf(IllegalStateException.class);
          }
        }
        else if(operation < 50 && supportsListIteratorSet()) {
          try {
            refIter.set("" + operation);
            assertThatNoException().isThrownBy(() -> iter.set("" + operation));
          }
          catch(IllegalStateException e) {
            assertThatThrownBy(() -> iter.set("" + operation)).isInstanceOf(IllegalStateException.class);
          }
        }
      }
    }
  }

  @Nested
  class StressTests {
    private final int OPERATIONS = stressTestOperations;
    private final String[] STRINGS = new String[OPERATIONS];

    private Random random = new Random(42); // Fixed seed for reproducibility
    private List<String> referenceList = new ArrayList<>();

    {
      for(int i = 0; i < OPERATIONS; i++) {
        STRINGS[i] = "" + i;
      }
    }

    @Test
    void stressTestRandomInsertions() {
      assumeTrue(supportsAddIndexT());
      assumeTrue(supportsRemoveIndex());

      for(int i = 0; i < OPERATIONS; i++) {
        int index = random.nextInt(referenceList.size() + 1);
        double d = random.nextDouble();
        String s = STRINGS[i];

        if(d < 0.05) {
          testList.addLast(s);
          referenceList.addLast(s);
        }
        else if(d < 0.10) {
          testList.addFirst(s);
          referenceList.addFirst(s);
        }
        else {
          testList.add(index, s);
          referenceList.add(index, s);
        }

        assertThat(testList)
          .as("index = " + index + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestFavoringNearBeginningInsertions() {
      assumeTrue(supportsAddIndexT());

      for(int i = 0; i < OPERATIONS; i++) {
        int index = insertRandomlyFavoringBeginning(i);

        assertThat(testList)
          .as("index = " + index + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestFavoringNearEndingInsertions() {
      assumeTrue(supportsAddIndexT());

      for(int i = 0; i < OPERATIONS; i++) {
        int index = insertRandomlyFavoringEnding(i);

        assertThat(testList)
          .as("index = " + index + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestOnlyNearBeginningInsertions() {
      assumeTrue(supportsAddIndexT());

      for(int i = 0; i < OPERATIONS; i++) {
        int index = random.nextInt(referenceList.size() / 3 + 1);
        double d = random.nextDouble();
        String s = STRINGS[i];

        if(d < 0.05) {
          testList.addFirst(s);
          referenceList.addFirst(s);
        }
        else {
          testList.add(index, s);
          referenceList.add(index, s);
        }

        assertThat(testList)
          .as("index = " + index + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestOnlyNearEndingInsertions() {
      assumeTrue(supportsAddIndexT());

      for(int i = 0; i < OPERATIONS; i++) {
        int index = random.nextInt(referenceList.size() / 3 + 1) + referenceList.size() * 2 / 3;
        double d = random.nextDouble();
        String s = STRINGS[i];

        if(d < 0.05) {
          testList.addLast(s);
          referenceList.addLast(s);
        }
        else {
          testList.add(index, s);
          referenceList.add(index, s);
        }

        assertThat(testList)
          .as("index = " + index + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestRandomRemovals() {
      assumeTrue(supportsRemoveIndex());

      for(int i = 0; i < OPERATIONS; i++) {
        String s = STRINGS[i];

        testList.add(s);
        referenceList.add(s);
        assertThat(testList).isEqualTo(referenceList);
      }

      for(int i = 0; i < OPERATIONS; i++) {
        double d = random.nextDouble();

        if(d < 0.05) {
          testList.removeLast();
          referenceList.removeLast();
        }
        else if(d < 0.10) {
          testList.removeFirst();
          referenceList.removeFirst();
        }
        else {
          int index = random.nextInt(referenceList.size());

          testList.remove(index);
          referenceList.remove(index);
        }

        assertThat(testList).isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestRandomRemovalsAndRandomInsertionsFavoringBeginning() {
      assumeTrue(supportsAddIndexT());
      assumeTrue(supportsRemoveIndex());

      for(int i = 0; i < OPERATIONS; i++) {
        String type = referenceList.isEmpty() || random.nextDouble() < 0.6 ? "add" : "remove";
        int index;

        if(type.equals("add")) {
          index = insertRandomlyFavoringBeginning(i);
        }
        else {
          index = removeRandomly();
        }

        assertThat(testList)
          .as("index = " + index + "; type = " + type + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestRandomRemovalsAndInsertionsAtEnd() {
      assumeTrue(supportsRemoveIndex());

      for(int i = 0; i < OPERATIONS; i++) {
        String type = referenceList.isEmpty() || random.nextDouble() < 0.6 ? "add" : "remove";
        int index;

        if(type.equals("add")) {
          index = append(i);
        }
        else {
          index = removeRandomly();
        }

        assertThat(testList)
          .as("index = " + index + "; type = " + type + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestRandomRemovalsFavoringBeginningAndRandomInsertionsFavoringBeginning() {
      assumeTrue(supportsAddIndexT());
      assumeTrue(supportsRemoveIndex());

      for(int i = 0; i < OPERATIONS; i++) {
        String type = referenceList.isEmpty() || random.nextDouble() < 0.6 ? "add" : "remove";
        int index;

        if(type.equals("add")) {
          index = insertRandomlyFavoringBeginning(i);
        }
        else {
          index = removeRandomlyFavoringBeginning();
        }

        assertThat(testList)
          .as("index = " + index + "; type = " + type + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    private int append(int i) {
      String s = STRINGS[i];
      int index = referenceList.size();

      testList.addLast(s);
      referenceList.addLast(s);

      return index;
    }

    private int insertRandomlyFavoringBeginning(int i) {
      double d = random.nextDouble();
      String s = STRINGS[i];
      int index;

      if(d < 0.05) {
        index = referenceList.size();

        testList.addLast(s);
        referenceList.addLast(s);
      }
      else if(d < 0.10) {
        index = 0;

        testList.addFirst(s);
        referenceList.addFirst(s);
      }
      else {
        index = random.nextInt(referenceList.size() + 1);

        if(index > referenceList.size() / 2) {  // slightly favor lower indices by rolling again
          index = random.nextInt(referenceList.size() + 1);
        }

        testList.add(index, s);
        referenceList.add(index, s);
      }

      return index;
    }

    private int insertRandomlyFavoringEnding(int i) {
      double d = random.nextDouble();
      String s = STRINGS[i];
      int index;

      if(d < 0.05) {
        index = referenceList.size();

        testList.addLast(s);
        referenceList.addLast(s);
      }
      else if(d < 0.10) {
        index = 0;

        testList.addFirst(s);
        referenceList.addFirst(s);
      }
      else {
        index = random.nextInt(referenceList.size() + 1);

        if(index < referenceList.size() / 2) {  // slightly favor higher indices by rolling again
          index = random.nextInt(referenceList.size() + 1);
        }

        testList.add(index, s);
        referenceList.add(index, s);
      }

      return index;
    }

    private int removeRandomly() {
      double d = random.nextDouble();
      int index;

      if(d < 0.05) {
        index = referenceList.size() - 1;

        testList.removeLast();
        referenceList.removeLast();
      }
      else if(d < 0.10) {
        index = 0;

        testList.removeFirst();
        referenceList.removeFirst();
      }
      else {
        index = random.nextInt(referenceList.size());

        testList.remove(index);
        referenceList.remove(index);
      }

      return index;
    }

    private int removeRandomlyFavoringBeginning() {
      double d = random.nextDouble();
      int index;

      if(d < 0.05) {
        index = referenceList.size() - 1;

        testList.removeLast();
        referenceList.removeLast();
      }
      else if(d < 0.10) {
        index = 0;

        testList.removeFirst();
        referenceList.removeFirst();
      }
      else {
        index = random.nextInt(referenceList.size());

        if(index > referenceList.size() / 2) {  // slightly favor lower indices by rolling again
          index = random.nextInt(referenceList.size());
        }

        testList.remove(index);
        referenceList.remove(index);
      }

      return index;
    }
  }

  @Nested
  class BulkOperationTests {
    @Test
    void addAllAtIndexShouldInsertInOrder() {
      assumeTrue(supportsAddIndexT());

      List<String> list = createList();

      list.addAll(List.of("A", "B", "C"));
      list.addAll(1, List.of("X", "Y"));

      assertThat(list).containsExactly("A", "X", "Y", "B", "C");
    }

    @Test
    void removeAllShouldRemoveAllMatchingElements() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "A", "C", "A"));
      list.removeAll(List.of("A"));

      assertThat(list).containsExactly("B", "C");
    }

    @Test
    void retainAllShouldKeepOnlyMatchingElements() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "A", "C", "A"));
      list.retainAll(List.of("A", "C"));

      assertThat(list).containsExactly("A", "A", "C", "A");
    }

    @Test
    void removeIfShouldRemoveMatchingElements() {
      List<String> list = createList();

      list.addAll(List.of("A", "BB", "CCC", "D"));
      list.removeIf(s -> s.length() > 1);

      assertThat(list).containsExactly("A", "D");
    }

    @Test
    void replaceAllShouldTransformAllElements() {
      assumeTrue(supportsListIteratorSet());

      List<String> list = createList();

      list.addAll(List.of("a", "b", "c"));
      list.replaceAll(String::toUpperCase);

      assertThat(list).containsExactly("A", "B", "C");
    }

    @Test
    void sortShouldOrderElements() {
      assumeTrue(supportsListIteratorSet());

      List<String> list = createList();

      list.addAll(List.of("C", "A", "B"));
      list.sort(Comparator.naturalOrder());

      assertThat(list).containsExactly("A", "B", "C");
    }
  }

  @Nested
  class DerivedOperationTests {
    @Test
    void toArrayShouldReturnAllElements() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "C"));

      assertThat(list.toArray()).containsExactly("A", "B", "C");
    }

    @Test
    void toArrayShouldFillProvidedArray() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "C"));

      assertThat(list.toArray(new String[0])).containsExactly("A", "B", "C");

      String[] larger = new String[5];
      String[] result = list.toArray(larger);

      assertThat(result).isSameAs(larger);
      assertThat(result).containsExactly("A", "B", "C", null, null);
    }

    @Test
    void equalsAndHashCodeShouldFollowListContract() {
      List<String> list = createList();
      List<String> other = createList();

      list.addAll(List.of("A", "B", "C"));
      other.addAll(List.of("A", "B", "C"));

      assertThat(list).isEqualTo(other);
      assertThat(list.hashCode()).isEqualTo(other.hashCode());
      assertThat(list).isEqualTo(List.of("A", "B", "C"));
      assertThat(list).isNotEqualTo(List.of("A", "B"));
      assertThat(list).isNotEqualTo(List.of("A", "B", "D"));
    }

    @Test
    void forEachShouldVisitAllElementsInOrder() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "C"));

      List<String> visited = new ArrayList<>();

      list.forEach(visited::add);

      assertThat(visited).containsExactlyElementsOf(list);
    }

    @Test
    void spliteratorShouldVisitAllElements() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "C"));

      List<String> visited = new ArrayList<>();

      list.spliterator().forEachRemaining(visited::add);

      assertThat(visited).containsExactlyElementsOf(list);
    }

    @Test
    void iteratorForEachRemainingShouldVisitRemainingElements() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "C"));

      Iterator<String> iterator = list.iterator();

      assertThat(iterator.next()).isEqualTo("A");

      List<String> visited = new ArrayList<>();

      iterator.forEachRemaining(visited::add);

      assertThat(visited).containsExactly("B", "C");
    }
  }

  @Nested
  class ClearAndReuseTests {
    @Test
    void clearThenReuseShouldWork() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "C"));
      list.clear();

      assertThat(list).isEmpty();

      list.addAll(List.of("X", "Y"));

      assertThat(list).containsExactly("X", "Y");
    }

    @Test
    void clearingEmptyListShouldBeNoOp() {
      List<String> list = createList();

      list.clear();
      list.clear();

      assertThat(list).isEmpty();
    }

    @Test
    void clearDuringIterationShouldThrowConcurrentModificationException() {
      List<String> list = createList();

      list.addAll(List.of("A", "B", "C"));

      Iterator<String> iterator = list.iterator();

      assertThat(iterator.next()).isEqualTo("A");

      list.clear();

      assertThatThrownBy(iterator::next).isInstanceOf(ConcurrentModificationException.class);
    }
  }
}
