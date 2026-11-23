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
import java.util.List;

import org.int4.common.test.explorer.Action;
import org.int4.common.test.explorer.Assertion;
import org.int4.common.test.explorer.Explorable;
import org.int4.common.test.explorer.ExploratoryTestRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public abstract class AbstractBTreeListExploratoryTest {

  protected abstract List<String> createList();

  @Test
  void validateStates() {
    ExploratoryTestRunner.explore(ListExplorable.class, () -> new ListExplorable(createList()), 6, 2);
  }

  record State(List<String> list) {}

  public static class ListExplorable implements Explorable {
    private final List<String> expected = new ArrayList<>();  // assume ArrayList is bugfree
    private final List<String> testList;

    public ListExplorable(List<String> list) {
      this.testList = list;
    }

    @Override
    public Object snapshot() {
      return new State(new ArrayList<>(expected));
    }

    @Assertion
    public void assertState() {
      assertThat(expected).containsExactlyElementsOf(testList);

      if(!expected.isEmpty()) {
        assertThat(testList.getFirst()).isEqualTo(expected.getFirst());
        assertThat(testList.getLast()).isEqualTo(expected.getLast());
      }
    }

    @Action
    @ValueSource(strings = {"A", "B", "C"})
    public Runnable appendElement(String value) {
      expected.add(value);

      return () -> testList.add(value);
    }

    @Action
    @ValueSource(strings = {"A", "B", "C"})
    public Runnable insertElement(String value) {
      int index = expected.size() / 2;

      expected.add(index, value);

      return () -> testList.add(index, value);
    }

    @Action
    @ValueSource(strings = {"A", "B", "C"})
    public Runnable removeElement(String value) {
      expected.remove(value);

      return () -> testList.remove(value);
    }

    @Action
    @ValueSource(ints = {-2, -1, 0, 1, 2})
    public Runnable removeIndex(int index) {
      int indexToRemove = index < 0 ? expected.size() + index : index;

      assumeTrue(indexToRemove >= 0 && indexToRemove < expected.size());

      expected.remove(indexToRemove);

      return () -> testList.remove(indexToRemove);
    }

    @Action
    @ValueSource(strings = {"A", "B", "C"})
    public Runnable setElement(String value) {
      assumeTrue(!expected.isEmpty());

      int index = expected.size() / 2;

      expected.set(index, value);

      return () -> testList.set(index, value);
    }

    @Action
    public Runnable clear() {
      expected.clear();

      return () -> testList.clear();
    }
  }
}
