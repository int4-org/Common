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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.RandomAccess;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImmutableTest {

  @Test
  void shouldThrowExceptionWhenListIsNull() {
    assertThatThrownBy(() -> Immutable.of(null))
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
