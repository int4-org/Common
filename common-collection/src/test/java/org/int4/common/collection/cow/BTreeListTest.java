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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.int4.common.collection.util.AbstractBTreeListTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BTreeListTest extends AbstractBTreeListTest {

  @Override
  protected List<String> createList() {
    return new BTreeList<>();
  }

  @Override
  protected List<String> createList(int nodeSize, int leafSize) {
    return new BTreeList<>(nodeSize, leafSize);
  }

  @Override
  protected List<Integer> createIntList(int nodeSize, int leafSize) {
    return new BTreeList<>(nodeSize, leafSize);
  }

  @Override
  protected List<Integer> createIntList(int nodeSize, int leafSize, Collection<Integer> values) {
    return new BTreeList<>(nodeSize, leafSize, values);
  }

  @Override
  protected <T> List<T> copy(List<T> list) {
    return ((BTreeList<T>)list).clone();
  }

  @Test
  void cloneShouldShareRootUntilModified() throws Exception {
    BTreeList<Integer> list = new BTreeList<>(8, 8);

    for(int i = 0; i < 100; i++) {
      list.add(i);
    }

    BTreeList<Integer> clone = list.clone();
    Field rootField = BTreeList.class.getDeclaredField("root");

    rootField.setAccessible(true);

    assertThat(rootField.get(clone)).isSameAs(rootField.get(list));

    clone.add(100);

    assertThat(rootField.get(clone)).isNotSameAs(rootField.get(list));
    assertThat(list).hasSize(100);
    assertThat(clone).hasSize(101);
    assertThat(list).doesNotContain(100);
  }

  @Test
  void shouldNotBeImmutableByDefault() {
    assertThat(new BTreeList<>().isImmutable()).isFalse();
  }

  @Test
  void freezeShouldReturnTheListAndBeIdempotent() {
    BTreeList<Integer> list = new BTreeList<>(8, 8);

    list.freeze();
    assertThat(list.isImmutable()).isTrue();
    list.freeze();
    assertThat(list.isImmutable()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 7, 8, 100, 1000})
  void frozenListShouldRejectEveryModificationAndStayUnchanged(int size) {
    List<Integer> expected = sequence(size);
    BTreeList<Integer> list = frozen(new BTreeList<>(8, 8, expected));
    Map<String, ThrowingCallable> modifications = new LinkedHashMap<>();

    modifications.put("add(E)", () -> list.add(1));
    modifications.put("add(int, E)", () -> list.add(0, 1));
    modifications.put("addFirst", () -> list.addFirst(1));
    modifications.put("addLast", () -> list.addLast(1));
    modifications.put("addAll", () -> list.addAll(List.of(1, 2)));
    modifications.put("addAll(empty)", () -> list.addAll(List.of()));
    modifications.put("addAll(int, ...)", () -> list.addAll(0, List.of(1, 2)));
    modifications.put("set", () -> list.set(0, 1));
    modifications.put("remove(int)", () -> list.remove(0));
    modifications.put("remove(Object)", () -> list.remove(Integer.valueOf(-1)));
    modifications.put("removeFirst", list::removeFirst);
    modifications.put("removeLast", list::removeLast);
    modifications.put("removeAll", () -> list.removeAll(List.of(-1)));
    modifications.put("retainAll", () -> list.retainAll(expected));
    modifications.put("removeIf", () -> list.removeIf(x -> false));
    modifications.put("replaceAll", () -> list.replaceAll(x -> x));
    modifications.put("sort", () -> list.sort(Comparator.naturalOrder()));
    modifications.put("clear", list::clear);
    modifications.put("iterator.remove", () -> list.iterator().remove());
    modifications.put("listIterator.set", () -> list.listIterator().set(1));
    modifications.put("listIterator.add", () -> list.listIterator().add(1));
    modifications.put("subList.clear", () -> list.subList(0, size / 2).clear());
    modifications.put("subList.add", () -> list.subList(0, size / 2).add(1));

    if(size > 0) {
      modifications.put("iterator.remove after next", () -> {
        ListIterator<Integer> iterator = list.listIterator();

        iterator.next();
        iterator.remove();
      });
      modifications.put("iterator.set after next", () -> {
        ListIterator<Integer> iterator = list.listIterator();

        iterator.next();
        iterator.set(1);
      });
      modifications.put("subList.set", () -> list.subList(0, size).set(0, 1));
    }

    for(Map.Entry<String, ThrowingCallable> modification : modifications.entrySet()) {
      assertThatThrownBy(modification.getValue()).as(modification.getKey()).isInstanceOf(UnsupportedOperationException.class);
    }

    assertThat(list).containsExactlyElementsOf(expected);
  }

  @Test
  void frozenListShouldStillBeReadable() {
    List<Integer> expected = sequence(500);
    BTreeList<Integer> list = frozen(new BTreeList<>(8, 8, expected));

    assertThat(list).hasSize(500).containsExactlyElementsOf(expected);
    assertThat(list.get(0)).isEqualTo(0);
    assertThat(list.get(499)).isEqualTo(499);
    assertThat(list.getFirst()).isEqualTo(0);
    assertThat(list.getLast()).isEqualTo(499);
    assertThat(list.indexOf(250)).isEqualTo(250);
    assertThat(list.contains(499)).isTrue();
    assertThat(list.subList(10, 20)).containsExactlyElementsOf(expected.subList(10, 20));
    assertThat(list.stream().mapToInt(x -> x).sum()).isEqualTo(499 * 500 / 2);
    assertThat(list).isEqualTo(expected);
    assertThat(list.hashCode()).isEqualTo(expected.hashCode());

    ListIterator<Integer> iterator = list.listIterator(500);

    for(int i = 499; i >= 0; i--) {
      assertThat(iterator.previous()).isEqualTo(i);
    }
  }

  @Test
  void cloneOfFrozenListShouldBeModifiableAndIndependent() {
    List<Integer> expected = sequence(1000);
    BTreeList<Integer> list = frozen(new BTreeList<>(8, 8, expected));
    BTreeList<Integer> clone = list.clone();

    assertThat(clone.isImmutable()).isFalse();
    assertThat(list.isImmutable()).isTrue();

    List<Integer> expectedClone = new ArrayList<>(expected);

    clone.add(0, -1);
    expectedClone.add(0, -1);
    clone.remove(500);
    expectedClone.remove(500);
    clone.set(10, 99);
    expectedClone.set(10, 99);
    clone.removeIf(x -> x % 7 == 0);
    expectedClone.removeIf(x -> x % 7 == 0);
    clone.sort(Comparator.reverseOrder());
    expectedClone.sort(Comparator.reverseOrder());

    assertThat(clone).containsExactlyElementsOf(expectedClone);
    assertThat(list).containsExactlyElementsOf(expected);
    assertThat(list.isImmutable()).isTrue();
  }

  @Test
  void cloneOfFrozenListCanBeMadeReadOnlyItself() {
    BTreeList<Integer> list = frozen(new BTreeList<>(8, 8, sequence(100)));
    BTreeList<Integer> clone = list.clone();

    clone.add(100);
    clone.freeze();

    assertThatThrownBy(() -> clone.add(101)).isInstanceOf(UnsupportedOperationException.class);
    assertThat(clone).hasSize(101);
    assertThat(list).hasSize(100);
  }

  @Test
  void snapshotShouldReturnTheSameListWhenItIsFrozen() {
    BTreeList<Integer> list = frozen(new BTreeList<>(8, 8, sequence(100)));

    assertThat(list.snapshot()).isSameAs(list);
    assertThat(list.snapshot().snapshot()).isSameAs(list);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 8, 100, 1000})
  void snapshotShouldReturnAnIsolatedFrozenSnapshotWhenNotFrozen(int size) {
    List<Integer> expected = sequence(size);
    BTreeList<Integer> list = new BTreeList<>(8, 8, expected);
    BTreeList<Integer> snapshot = list.snapshot();

    assertThat(snapshot).isNotSameAs(list);
    assertThat(snapshot.isImmutable()).isTrue();
    assertThat(list.isImmutable()).isFalse();
    assertThat(snapshot).containsExactlyElementsOf(expected);

    list.add(0, -1);
    list.add(size + 1);
    list.set(1, 99);

    if(size > 3) {
      list.remove(3);
    }

    assertThat(snapshot).containsExactlyElementsOf(expected);
    assertThatThrownBy(() -> snapshot.add(1)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> snapshot.set(0, 1)).isInstanceOf(UnsupportedOperationException.class);
    assertThat(list.isImmutable()).isFalse();
  }

  @Test
  void snapshotShouldShareTheTreeInsteadOfCopyingIt() throws Exception {
    BTreeList<Integer> list = new BTreeList<>(8, 8, sequence(1000));
    BTreeList<Integer> snapshot = list.snapshot();
    Field rootField = BTreeList.class.getDeclaredField("root");

    rootField.setAccessible(true);

    assertThat(rootField.get(snapshot)).isSameAs(rootField.get(list));

    list.add(0, -1);

    assertThat(rootField.get(snapshot)).isNotSameAs(rootField.get(list));
  }

  @Test
  void snapshotShouldKeepNullElements() {
    BTreeList<String> list = new BTreeList<>(8, 8);

    list.add("a");
    list.add(null);

    assertThat(list.snapshot()).containsExactly("a", null);
  }

  @Test
  void frozenListShouldNotBeChangedByCloningIt() throws Exception {
    BTreeList<Integer> list = frozen(new BTreeList<>(8, 8, sequence(100)));
    Field ownerField = BTreeList.class.getDeclaredField("owner");

    ownerField.setAccessible(true);

    assertThat(ownerField.get(list)).as("owner of frozen list").isNull();

    BTreeList<Integer> clone = list.clone();

    list.clone();

    assertThat(ownerField.get(list)).as("owner of frozen list after cloning it").isNull();
    assertThat(ownerField.get(clone)).as("owner of the clone").isNotNull();
    assertThat(list.isImmutable()).isTrue();
    assertThat(clone.isImmutable()).isFalse();
  }

  @Test
  void listShouldOnlyGetAnOwnerBackThroughCloning() {
    BTreeList<Integer> list = frozen(new BTreeList<>(8, 8, sequence(100)));
    BTreeList<Integer> clone = list.clone();

    clone.add(100);

    assertThat(clone).hasSize(101);
    assertThat(clone.isImmutable()).isFalse();
    assertThat(list).hasSize(100);
    assertThat(list.isImmutable()).isTrue();
  }

  @Test
  void frozenListShouldBeUsableAndCloneableOnManyThreadsAtOnce() throws Exception {
    List<Integer> expected = sequence(5000);
    BTreeList<Integer> list = frozen(new BTreeList<>(8, 8, expected));

    try(ExecutorService executor = Executors.newFixedThreadPool(8)) {
      List<Future<?>> futures = new ArrayList<>();

      for(int t = 0; t < 8; t++) {
        long seed = t;

        futures.add(executor.submit(() -> {
          Random random = new Random(seed);

          for(int i = 0; i < 100; i++) {
            BTreeList<Integer> clone = list.clone();
            List<Integer> expectedClone = new ArrayList<>(expected);

            for(int k = 0; k < 20; k++) {
              int index = random.nextInt(expectedClone.size());

              clone.add(index, -k);
              expectedClone.add(index, -k);
              clone.remove(index + 1);
              expectedClone.remove(index + 1);
            }

            assertThat(clone).containsExactlyElementsOf(expectedClone);

            int index = random.nextInt(expected.size());

            assertThat(list.get(index)).isEqualTo(expected.get(index));
          }
        }));
      }

      for(Future<?> future : futures) {
        future.get();
      }
    }

    assertThat(list).containsExactlyElementsOf(expected);
  }

  private static <E> BTreeList<E> frozen(BTreeList<E> list) {
    list.freeze();

    return list;
  }

  private static List<Integer> sequence(int size) {
    List<Integer> values = new ArrayList<>();

    for(int i = 0; i < size; i++) {
      values.add(i);
    }

    return values;
  }
}
