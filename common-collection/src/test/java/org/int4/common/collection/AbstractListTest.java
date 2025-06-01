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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.int4.common.collection;

import java.util.ArrayList;
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

public abstract class AbstractListTest {
  private List<String> testList = createList();

  protected abstract List<String> createList();
  protected int stressTestOperations = 100000;

  static {
    Assertions.setMaxStackTraceElementsDisplayed(2000);
  }

  class InvariantTests {
    @Test
    void accessingIllegalIndicesShouldThrowIndexOutOfBoundsException() {
      assertThatThrownBy(() -> testList.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> testList.remove(-1)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> testList.add(-1, "")).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> testList.listIterator(-1)).isInstanceOf(IndexOutOfBoundsException.class);

      int size = testList.size();

      assertThatThrownBy(() -> testList.get(size)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> testList.remove(size)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> testList.add(size + 1, "")).isInstanceOf(IndexOutOfBoundsException.class);
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
      assertThat(testList.removeFirst()).isEqualTo(ELEMENT);
      assertThat(testList).isEmpty();
    }

    @Test
    void removeLastShouldReturnElement() {
      assertThat(testList.removeLast()).isEqualTo(ELEMENT);
      assertThat(testList).isEmpty();
    }

    @Test
    void remove0ShouldReturnElement() {
      assertThat(testList.remove(0)).isEqualTo(ELEMENT);
      assertThat(testList).isEmpty();
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
      int size = 10;

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
      List<String> data = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K");
      int size = 11;

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
  class IteratorStressTests {
    private static int OPERATIONS = 100000;

    private Random random = new Random(42); // Fixed seed for reproducibility
    private List<String> referenceList = new ArrayList<>();

    @Test
    void randomIteratorActions() {
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
        else if(operation < 30) {
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
        else if(operation < 50) {
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
      for(int i = 0; i < OPERATIONS; i++) {
        int index = insertRandomlyFavoringBeginning(i);

        assertThat(testList)
          .as("index = " + index + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestFavoringNearEndingInsertions() {
      for(int i = 0; i < OPERATIONS; i++) {
        int index = insertRandomlyFavoringEnding(i);

        assertThat(testList)
          .as("index = " + index + "; op = " + i)
          .isEqualTo(referenceList);
      }
    }

    @Test
    void stressTestOnlyNearBeginningInsertions() {
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
    void stressTestRandomRemovalsFavoringBeginningAndRandomInsertionsFavoringBeginning() {
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
}
