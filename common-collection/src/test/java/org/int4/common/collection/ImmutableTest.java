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

package org.int4.common.collection;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.int4.common.collection.cow.BTreeList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImmutableTest {

  @Nested
  class ImmutableCopyTests {

    @Nested
    class ListTests {

      @Test
      void shouldThrowExceptionWhenListIsNull() {
        assertThatThrownBy(() -> Immutable.of((List<?>)null))
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("list");
      }

      @Test
      void shouldCreateImmutableCopyOfMutableList() {
        List<String> mutableList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> immutableList = Immutable.of(mutableList);

        assertThat(immutableList).containsExactly("a", "b", "c");

        mutableList.add("d");

        assertThat(immutableList).containsExactly("a", "b", "c");
        assertThat(immutableList).hasSize(3);
      }

      @Test
      void shouldNotAllowModifications() {
        List<String> immutableList = Immutable.of(Arrays.asList("a", "b"));

        assertThatThrownBy(() -> immutableList.add("c")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> immutableList.remove(0)).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> immutableList.set(0, "z")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> immutableList.clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void shouldReturnSameInstanceIfAlreadyImmutable() {
        List<String> original = List.of("a", "b");
        List<String> immutable = Immutable.of(original);

        assertThat(immutable).isSameAs(original);

        List<String> doubleImmutable = Immutable.of(immutable);

        assertThat(doubleImmutable).isSameAs(immutable);
      }

      @Test
      void shouldReturnSameInstanceIfBTreeListIsFrozen() {
        BTreeList<String> frozen = new BTreeList<>(Arrays.asList("a", "b", "c"));

        frozen.freeze();

        assertThat(Immutable.of(frozen)).isSameAs(frozen);
      }

      @Test
      void shouldReturnFrozenSnapshotForBTreeListThatIsNotFrozen() {
        BTreeList<String> mutable = new BTreeList<>(Arrays.asList("a", null, "c"));
        List<String> immutable = Immutable.of(mutable);

        assertThat(immutable).isNotSameAs(mutable).isInstanceOf(BTreeList.class);
        assertThat(((BTreeList<String>)immutable).isImmutable()).isTrue();
        assertThat(immutable).containsExactly("a", null, "c");

        mutable.add("d");
        mutable.remove(0);

        assertThat(immutable).containsExactly("a", null, "c");
        assertThat(mutable).containsExactly(null, "c", "d");
        assertThatThrownBy(() -> immutable.add("e")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> immutable.remove(0)).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> immutable.clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void shouldPreserveRandomAccess() {
        List<String> arrayList = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> immutableArrayList = Immutable.of(arrayList);

        assertThat(immutableArrayList).isInstanceOf(RandomAccess.class);
        assertThat(immutableArrayList.getClass().getSimpleName()).contains("RandomAccess");

        List<String> linkedList = new LinkedList<>(Arrays.asList("a", "b"));
        List<String> immutableLinkedList = Immutable.of(linkedList);

        assertThat(immutableLinkedList).isNotInstanceOf(RandomAccess.class);
        assertThat(immutableLinkedList.getClass().getSimpleName()).doesNotContain("RandomAccess");
      }

      @Test
      void shouldPreserveNulls() {
        List<String> listWithNulls = Arrays.asList("a", null, "c");
        List<String> immutable = Immutable.of(listWithNulls);

        assertThat(immutable).containsExactly("a", null, "c");
        assertThat(immutable.get(1)).isNull();
        assertThat(immutable.contains(null)).isTrue();
        assertThat(immutable.indexOf(null)).isEqualTo(1);
        assertThat(immutable.lastIndexOf(null)).isEqualTo(1);
      }

      @Test
      void shouldSupportBasicListOperations() {
        List<String> list = Immutable.of(Arrays.asList("a", "b", "a"));

        assertThat(list.get(0)).isEqualTo("a");
        assertThat(list.size()).isEqualTo(3);
        assertThat(list.contains("b")).isTrue();
        assertThat(list.contains("c")).isFalse();
        assertThat(list.indexOf("a")).isEqualTo(0);
        assertThat(list.lastIndexOf("a")).isEqualTo(2);
        assertThat(list.iterator()).toIterable().containsExactly("a", "b", "a");
        assertThat(list.listIterator(1)).toIterable().containsExactly("b", "a");
      }
    }

    @Nested
    class MapTests {

      @Test
      void shouldThrowExceptionWhenMapIsNull() {
        assertThatThrownBy(() -> Immutable.of((Map<?, ?>)null))
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("map");
      }

      @Test
      void shouldCreateImmutableMapFromMutableMap() {
        Map<String, Integer> mutable = new HashMap<>();

        mutable.put("a", 1);
        mutable.put("b", 2);

        Map<String, Integer> immutable = Immutable.of(mutable);

        assertThat(immutable).hasSize(2);
        assertThat(immutable.get("a")).isEqualTo(1);
        assertThat(immutable.get("b")).isEqualTo(2);

        mutable.put("c", 3);

        assertThat(immutable).hasSize(2);
      }

      @Test
      void shouldNotAllowMapModifications() {
        Map<String, String> map = Immutable.of(Map.of());

        assertThatThrownBy(() -> map.put("k", "v")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.remove("k")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.putAll(Map.of("x", "y"))).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.replace("k", "v")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.replaceAll((k, v) -> v)).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.computeIfAbsent("k", k -> "v")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.merge("k", "v", (a, b) -> a)).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void shouldReturnSameInstanceForImmutableMap() {
        Map<String, String> original = Map.of("a", "1");
        Map<String, String> immutable = Immutable.of(original);

        assertThat(immutable).isSameAs(original);
      }

      @Test
      void shouldSupportMapBasicOperations() {
        Map<String, Integer> map = Immutable.of(new HashMap<>(Map.of("a", 1, "b", 2)));

        assertThat(map.size()).isEqualTo(2);
        assertThat(map.isEmpty()).isFalse();
        assertThat(map.containsKey("a")).isTrue();
        assertThat(map.containsKey("z")).isFalse();
        assertThat(map.containsValue(1)).isTrue();
        assertThat(map.getOrDefault("a", 0)).isEqualTo(1);
        assertThat(map.getOrDefault("z", 0)).isEqualTo(0);
        assertThat(map.keySet()).containsExactlyInAnyOrder("a", "b");
        assertThat(map.values()).containsExactlyInAnyOrder(1, 2);
      }

      @Test
      void mapEntriesShouldBeImmutable() {
        Map<String, Integer> map = Immutable.of(new HashMap<>(Map.of("a", 1, "b", 2)));

        for(Map.Entry<String, Integer> entry : map.entrySet()) {
          assertThatThrownBy(() -> entry.setValue(99))
            .isExactlyInstanceOf(UnsupportedOperationException.class);
        }
      }
    }

    @Nested
    class SequencedMapTests {

      @Test
      void shouldCreateImmutableSequencedMap() {
        SequencedMap<String, Integer> mutable = new LinkedHashMap<>();

        mutable.put("a", 1);
        mutable.put("b", 2);
        mutable.put("c", 3);

        SequencedMap<String, Integer> immutable = Immutable.of(mutable);

        assertThat(immutable).hasSize(3);
        assertThat(immutable.firstEntry()).isEqualTo(new AbstractMap.SimpleEntry<>("a", 1));
        assertThat(immutable.lastEntry()).isEqualTo(new AbstractMap.SimpleEntry<>("c", 3));

        mutable.put("d", 4);

        assertThat(immutable).hasSize(3);
      }

      @Test
      void sequencedMapReversedShouldBeImmutable() {
        SequencedMap<String, Integer> map = Immutable.of(new LinkedHashMap<>(Map.of("a", 1, "b", 2)));
        SequencedMap<String, Integer> reversed = map.reversed();

        assertThat(reversed.firstEntry()).isEqualTo(map.lastEntry());
        assertThat(reversed.lastEntry()).isEqualTo(map.firstEntry());
        assertThatThrownBy(() -> reversed.put("x", 2)).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void sequencedMapSubViewsShouldBeImmutable() {
        SequencedMap<String, Integer> map = Immutable.of(new LinkedHashMap<>(Map.of("a", 1, "b", 2, "c", 3)));

        assertThat(map.sequencedKeySet()).isInstanceOf(SequencedSet.class);
        assertThatThrownBy(() -> map.sequencedKeySet().add("x")).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void sequencedMapEntriesShouldBeImmutable() {
        SequencedMap<String, Integer> map = Immutable.of(new LinkedHashMap<>(Map.of("a", 1, "b", 2)));

        for(Map.Entry<String, Integer> entry : map.entrySet()) {
          assertThatThrownBy(() -> entry.setValue(99))
            .isExactlyInstanceOf(UnsupportedOperationException.class);
        }

        Map.Entry<String, Integer> first = map.firstEntry();

        assertThatThrownBy(() -> first.setValue(99))
          .isExactlyInstanceOf(UnsupportedOperationException.class);

        Map.Entry<String, Integer> last = map.lastEntry();

        assertThatThrownBy(() -> last.setValue(99))
          .isExactlyInstanceOf(UnsupportedOperationException.class);
      }
    }

    @Nested
    class SortedMapTests {

      @Test
      void shouldThrowExceptionWhenSortedMapIsNull() {
        assertThatThrownBy(() -> Immutable.of((SortedMap<?, ?>)null))
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("map");
      }

      @Test
      void shouldCreateImmutableSortedMap() {
        SortedMap<String, Integer> mutable = new TreeMap<>();

        mutable.put("c", 3);
        mutable.put("a", 1);
        mutable.put("b", 2);

        SortedMap<String, Integer> immutable = Immutable.of(mutable);

        assertThat(immutable).hasSize(3);
        assertThat(immutable.firstKey()).isEqualTo("a");
        assertThat(immutable.lastKey()).isEqualTo("c");

        mutable.put("d", 4);

        assertThat(immutable).hasSize(3);
      }

      @Test
      void sortedMapSubViewsShouldBeImmutable() {
        SortedMap<String, Integer> map = Immutable.of(new TreeMap<>(Map.of("a", 1, "b", 2, "c", 3)));

        SortedMap<String, Integer> sub = map.subMap("a", "c");

        assertThat(sub).hasSize(2);
        assertThatThrownBy(() -> sub.put("x", 9)).isExactlyInstanceOf(UnsupportedOperationException.class);

        SortedMap<String, Integer> head = map.headMap("b");

        assertThat(head).hasSize(1);

        SortedMap<String, Integer> tail = map.tailMap("b");

        assertThat(tail).hasSize(2);
      }

      @Test
      void sortedMapReversedShouldBeImmutable() {
        SortedMap<String, Integer> map = Immutable.of(new TreeMap<>(Map.of("a", 1, "b", 2)));
        SortedMap<String, Integer> reversed = map.reversed();

        assertThat(reversed.firstKey()).isEqualTo("b");
        assertThatThrownBy(() -> reversed.put("x", 9)).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void sortedMapEntriesShouldBeImmutable() {
        SortedMap<String, Integer> map = Immutable.of(new TreeMap<>(Map.of("a", 1, "b", 2)));

        for(Map.Entry<String, Integer> entry : map.entrySet()) {
          assertThatThrownBy(() -> entry.setValue(99))
            .isExactlyInstanceOf(UnsupportedOperationException.class);
        }

        Map.Entry<String, Integer> first = map.firstEntry();

        assertThatThrownBy(() -> first.setValue(99))
          .isExactlyInstanceOf(UnsupportedOperationException.class);

        Map.Entry<String, Integer> last = map.lastEntry();

        assertThatThrownBy(() -> last.setValue(99))
          .isExactlyInstanceOf(UnsupportedOperationException.class);
      }
    }

    @Nested
    class NavigableMapTests {

      @Test
      void shouldThrowExceptionWhenNavigableMapIsNull() {
        assertThatThrownBy(() -> Immutable.of((NavigableMap<?, ?>)null))
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("map");
      }

      @Test
      void shouldCreateImmutableNavigableMap() {
        NavigableMap<String, Integer> mutable = new TreeMap<>();

        mutable.put("c", 3);
        mutable.put("a", 1);
        mutable.put("b", 2);

        NavigableMap<String, Integer> immutable = Immutable.of(mutable);

        assertThat(immutable).hasSize(3);
        assertThat(immutable.firstKey()).isEqualTo("a");
        assertThat(immutable.lastKey()).isEqualTo("c");

        mutable.put("d", 4);

        assertThat(immutable).hasSize(3);
      }

      @Test
      void navigableMapNavigationShouldWork() {
        NavigableMap<Integer, String> map = Immutable.of(new TreeMap<>(Map.of(1, "a", 3, "c", 5, "e")));

        assertThat(map.lowerKey(3)).isEqualTo(1);
        assertThat(map.floorKey(3)).isEqualTo(3);
        assertThat(map.ceilingKey(3)).isEqualTo(3);
        assertThat(map.higherKey(3)).isEqualTo(5);

        assertThat(map.lowerEntry(3)).hasFieldOrPropertyWithValue("key", 1);
        assertThat(map.floorEntry(3)).hasFieldOrPropertyWithValue("key", 3);
        assertThat(map.ceilingEntry(3)).hasFieldOrPropertyWithValue("key", 3);
        assertThat(map.higherEntry(3)).hasFieldOrPropertyWithValue("key", 5);

        assertThat(map.lowerKey(1)).isNull();
        assertThat(map.higherKey(5)).isNull();
      }

      @Test
      void navigableMapSubViewsShouldBeImmutable() {
        NavigableMap<Integer, String> map = Immutable.of(new TreeMap<>(Map.of(1, "a", 3, "c", 5, "e")));

        NavigableMap<Integer, String> sub = map.subMap(1, true, 5, true);

        assertThat(sub).hasSize(3);
        assertThatThrownBy(() -> sub.put(9, "z")).isExactlyInstanceOf(UnsupportedOperationException.class);

        NavigableMap<Integer, String> head = map.headMap(3, false);

        assertThat(head).hasSize(1);

        NavigableMap<Integer, String> tail = map.tailMap(3, true);

        assertThat(tail).hasSize(2);
      }

      @Test
      void navigableMapDescendingShouldBeImmutable() {
        NavigableMap<String, Integer> map = Immutable.of(new TreeMap<>(Map.of("a", 1, "b", 2)));
        NavigableMap<String, Integer> desc = map.descendingMap();

        assertThat(desc.firstKey()).isEqualTo("b");
        assertThatThrownBy(() -> desc.put("x", 9)).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void navigableMapKeySetsShouldBeImmutable() {
        NavigableMap<String, Integer> map = Immutable.of(new TreeMap<>(Map.of("a", 1, "b", 2)));

        NavigableSet<String> navKeySet = map.navigableKeySet();

        assertThat(navKeySet).containsExactly("a", "b");
        assertThatThrownBy(() -> navKeySet.add("c")).isExactlyInstanceOf(UnsupportedOperationException.class);

        NavigableSet<String> descKeySet = map.descendingKeySet();

        assertThat(descKeySet).containsExactly("b", "a");
      }

      @Test
      void navigableMapEntriesShouldBeImmutable() {
        NavigableMap<String, Integer> map = Immutable.of(new TreeMap<>(Map.of("a", 1, "b", 2)));

        for(Map.Entry<String, Integer> entry : map.entrySet()) {
          assertThatThrownBy(() -> entry.setValue(99))
            .isExactlyInstanceOf(UnsupportedOperationException.class);
        }

        Map.Entry<String, Integer> first = map.firstEntry();

        assertThatThrownBy(() -> first.setValue(99))
          .isExactlyInstanceOf(UnsupportedOperationException.class);

        Map.Entry<String, Integer> last = map.lastEntry();

        assertThatThrownBy(() -> last.setValue(99))
          .isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void navigableMapNavigationEntriesShouldBeImmutable() {
        NavigableMap<Integer, String> map = Immutable.of(new TreeMap<>(Map.of(1, "a", 3, "c", 5, "e")));

        Map.Entry<Integer, String> lower = map.lowerEntry(3);

        assertThatThrownBy(() -> lower.setValue("z"))
          .isExactlyInstanceOf(UnsupportedOperationException.class);

        Map.Entry<Integer, String> floor = map.floorEntry(3);

        assertThatThrownBy(() -> floor.setValue("z"))
          .isExactlyInstanceOf(UnsupportedOperationException.class);

        Map.Entry<Integer, String> ceiling = map.ceilingEntry(3);

        assertThatThrownBy(() -> ceiling.setValue("z"))
          .isExactlyInstanceOf(UnsupportedOperationException.class);

        Map.Entry<Integer, String> higher = map.higherEntry(3);

        assertThatThrownBy(() -> higher.setValue("z"))
          .isExactlyInstanceOf(UnsupportedOperationException.class);
      }
    }

    @Nested
    class SetTests {

      @Test
      void shouldThrowExceptionWhenSetIsNull() {
        assertThatThrownBy(() -> Immutable.of((Set<?>)null))
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("set");
      }

      @Test
      void shouldCreateImmutableSetFromMutableSet() {
        Set<String> mutable = new HashSet<>(Arrays.asList("a", "b", "c"));

        Set<String> immutable = Immutable.of(mutable);

        assertThat(immutable).containsExactlyInAnyOrder("a", "b", "c");

        mutable.add("d");

        assertThat(immutable).hasSize(3);
      }

      @Test
      void shouldNotAllowSetModifications() {
        Set<String> set = Immutable.of(new HashSet<>(Arrays.asList("a")));

        assertThatThrownBy(() -> set.add("b")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> set.remove("a")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> set.clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> set.addAll(Arrays.asList("x"))).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> set.retainAll(Arrays.asList("a"))).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> set.removeAll(Arrays.asList("a"))).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void shouldReturnSameInstanceForImmutableSet() {
        Set<String> original = Set.of("a", "b");
        Set<String> immutable = Immutable.of(original);

        assertThat(immutable).isSameAs(original);
      }

      @Test
      void shouldSupportSetBasicOperations() {
        Set<String> set = Immutable.of(new HashSet<>(Arrays.asList("a", "b")));

        assertThat(set.size()).isEqualTo(2);
        assertThat(set.isEmpty()).isFalse();
        assertThat(set.contains("a")).isTrue();
        assertThat(set.contains("z")).isFalse();
        assertThat(set.isEmpty()).isFalse();
      }
    }

    @Nested
    class SequencedSetTests {

      @Test
      void shouldCreateImmutableSequencedSet() {
        SequencedSet<String> mutable = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));

        SequencedSet<String> immutable = Immutable.of(mutable);

        assertThat(immutable).containsExactly("a", "b", "c");

        mutable.add("d");

        assertThat(immutable).hasSize(3);
      }

      @Test
      void sequencedSetReversedShouldBeImmutable() {
        SequencedSet<String> set = Immutable.of(new LinkedHashSet<>(Arrays.asList("a", "b")));
        SequencedSet<String> reversed = set.reversed();

        assertThat(reversed).containsExactly("b", "a");
        assertThatThrownBy(() -> reversed.add("x")).isExactlyInstanceOf(UnsupportedOperationException.class);
      }
    }

    @Nested
    class SortedSetTests {

      @Test
      void shouldThrowExceptionWhenSortedSetIsNull() {
        assertThatThrownBy(() -> Immutable.of((SortedSet<?>)null))
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("set");
      }

      @Test
      void shouldCreateImmutableSortedSet() {
        SortedSet<String> mutable = new TreeSet<>(Arrays.asList("c", "a", "b"));

        SortedSet<String> immutable = Immutable.of(mutable);

        assertThat(immutable).containsExactly("a", "b", "c");
        assertThat(immutable.first()).isEqualTo("a");
        assertThat(immutable.last()).isEqualTo("c");

        mutable.add("d");

        assertThat(immutable).hasSize(3);
      }

      @Test
      void sortedSetSubViewsShouldBeImmutable() {
        SortedSet<String> set = Immutable.of(new TreeSet<>(Arrays.asList("a", "b", "c", "d")));

        SortedSet<String> sub = set.subSet("b", "d");

        assertThat(sub).containsExactly("b", "c");
        assertThatThrownBy(() -> sub.add("x")).isExactlyInstanceOf(UnsupportedOperationException.class);

        SortedSet<String> head = set.headSet("c");

        assertThat(head).containsExactly("a", "b");

        SortedSet<String> tail = set.tailSet("c");

        assertThat(tail).containsExactly("c", "d");
      }

      @Test
      void sortedSetReversedShouldBeImmutable() {
        SortedSet<String> set = Immutable.of(new TreeSet<>(Arrays.asList("a", "b")));
        SortedSet<String> reversed = set.reversed();

        assertThat(reversed.first()).isEqualTo("b");
        assertThatThrownBy(() -> reversed.add("x")).isExactlyInstanceOf(UnsupportedOperationException.class);
      }
    }

    @Nested
    class NavigableSetTests {

      @Test
      void shouldThrowExceptionWhenNavigableSetIsNull() {
        assertThatThrownBy(() -> Immutable.of((NavigableSet<?>)null))
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("set");
      }

      @Test
      void shouldCreateImmutableNavigableSet() {
        NavigableSet<Integer> mutable = new TreeSet<>(Arrays.asList(1, 3, 5));

        NavigableSet<Integer> immutable = Immutable.of(mutable);

        assertThat(immutable).containsExactly(1, 3, 5);
        assertThat(immutable.first()).isEqualTo(1);
        assertThat(immutable.last()).isEqualTo(5);

        mutable.add(7);

        assertThat(immutable).hasSize(3);
      }

      @Test
      void navigableSetNavigationShouldWork() {
        NavigableSet<Integer> set = Immutable.of(new TreeSet<>(Arrays.asList(1, 3, 5)));

        assertThat(set.lower(3)).isEqualTo(1);
        assertThat(set.floor(3)).isEqualTo(3);
        assertThat(set.ceiling(3)).isEqualTo(3);
        assertThat(set.higher(3)).isEqualTo(5);

        assertThat(set.lower(1)).isNull();
        assertThat(set.higher(5)).isNull();
      }

      @Test
      void navigableSetSubViewsShouldBeImmutable() {
        NavigableSet<Integer> set = Immutable.of(new TreeSet<>(Arrays.asList(1, 3, 5)));

        NavigableSet<Integer> sub = set.subSet(1, true, 5, true);

        assertThat(sub).hasSize(3);
        assertThatThrownBy(() -> sub.add(9)).isExactlyInstanceOf(UnsupportedOperationException.class);

        NavigableSet<Integer> head = set.headSet(3, false);

        assertThat(head).hasSize(1);

        NavigableSet<Integer> tail = set.tailSet(3, true);

        assertThat(tail).hasSize(2);
      }

      @Test
      void navigableSetDescendingShouldBeImmutable() {
        NavigableSet<Integer> set = Immutable.of(new TreeSet<>(Arrays.asList(1, 3)));
        NavigableSet<Integer> desc = set.descendingSet();

        assertThat(desc.first()).isEqualTo(3);
        assertThatThrownBy(() -> desc.add(9)).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void navigableSetPollShouldThrow() {
        NavigableSet<String> set = Immutable.of(new TreeSet<>(Arrays.asList("a", "b")));

        assertThatThrownBy(() -> set.pollFirst()).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> set.pollLast()).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void navigableSetDescendingIteratorShouldWork() {
        NavigableSet<Integer> set = Immutable.of(new TreeSet<>(Arrays.asList(1, 3, 5)));

        assertThat(set.descendingIterator()).toIterable().containsExactly(5, 3, 1);
      }
    }
  }

  @Nested
  class FactoryTests {

    @Nested
    class SequencedMapTests {

      @Test
      void shouldCreateEmptySequencedMap() {
        SequencedMap<String, String> map = Immutable.sequencedMap();

        assertThat(map).isEmpty();
        assertThatThrownBy(() -> map.put("k", "v")).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void shouldCreateSequencedMapWithOneEntry() {
        SequencedMap<String, Integer> map = Immutable.sequencedMap("a", 1);

        assertThat(map).hasSize(1);
        assertThat(map.get("a")).isEqualTo(1);
        assertThat(map.firstEntry()).isEqualTo(new AbstractMap.SimpleEntry<>("a", 1));
        assertThat(map.lastEntry()).isEqualTo(new AbstractMap.SimpleEntry<>("a", 1));
      }

      @Test
      void shouldCreateSequencedMapWithTwoEntries() {
        SequencedMap<String, Integer> map = Immutable.sequencedMap("a", 1, "b", 2);

        assertThat(map).hasSize(2);
        assertThat(map.get("a")).isEqualTo(1);
        assertThat(map.get("b")).isEqualTo(2);
        assertThat(map.firstEntry()).isEqualTo(new AbstractMap.SimpleEntry<>("a", 1));
        assertThat(map.lastEntry()).isEqualTo(new AbstractMap.SimpleEntry<>("b", 2));
      }

      @Test
      void shouldCreateSequencedMapWithThreeEntries() {
        SequencedMap<String, Integer> map = Immutable.sequencedMap("a", 1, "b", 2, "c", 3);

        assertThat(map).hasSize(3);
        assertThat(map.get("a")).isEqualTo(1);
        assertThat(map.get("b")).isEqualTo(2);
        assertThat(map.get("c")).isEqualTo(3);
        assertThat(map.firstEntry()).isEqualTo(new AbstractMap.SimpleEntry<>("a", 1));
        assertThat(map.lastEntry()).isEqualTo(new AbstractMap.SimpleEntry<>("c", 3));
      }

      @Test
      void sequencedMapShouldBeImmutable() {
        SequencedMap<String, String> map = Immutable.sequencedMap("k", "v");

        assertThatThrownBy(() -> map.put("x", "y")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.remove("k")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.pollFirstEntry()).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.pollLastEntry()).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.putFirst("x", "y")).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.putLast("x", "y")).isExactlyInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void sequencedMapFactoryEntriesShouldBeImmutable() {
        SequencedMap<String, Integer> map = Immutable.sequencedMap("a", 1, "b", 2);

        for(Map.Entry<String, Integer> entry : map.entrySet()) {
          assertThatThrownBy(() -> entry.setValue(99))
            .isExactlyInstanceOf(UnsupportedOperationException.class);
        }

        Map.Entry<String, Integer> first = map.firstEntry();

        assertThatThrownBy(() -> first.setValue(99))
          .isExactlyInstanceOf(UnsupportedOperationException.class);

        Map.Entry<String, Integer> last = map.lastEntry();

        assertThatThrownBy(() -> last.setValue(99))
          .isExactlyInstanceOf(UnsupportedOperationException.class);
      }
    }
  }
}
