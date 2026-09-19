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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.int4.common.test.explorer.Action;
import org.int4.common.test.explorer.Assertion;
import org.int4.common.test.explorer.Explorable;
import org.int4.common.test.explorer.ExploratoryTestRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class HashListExploratoryTest {

  @Test
  void validateStates() {
    ExploratoryTestRunner.explore(ListExplorable.class, () -> new ListExplorable(new HashList<>()), 6, 2);
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

      for(String s : expected) {
        assertThat(testList.contains(s)).isTrue();
      }
    }

    @Action
    @ValueSource(strings = {"A", "AaAa", "AaBB", "BBAa", "H"})
    public Runnable appendElement(String value) {
      expected.add(value);

      return () -> testList.add(value);
    }

    @Action
    @ValueSource(strings = {
      "T,H,E,Q,U,I,C,K,B,R,O,W,N,F,O,X,J,U,M,P,S,O,V,E,R,T,H,E,L,A,Z,Y,D,O,G"
    })
    public Runnable appendMultipleElements(String value) {
      assumeTrue(expected.size() < 30);  // don't make list too big via this method

      String[] split = value.split(",");
      Stream.of(split).forEach(expected::add);

      return () -> Stream.of(split).forEach(testList::add);
    }

    @Action
    @ValueSource(strings = {"A", "AaAa", "AaBB", "BBAa", "H"})
    public Runnable removeElement(String value) {
      expected.remove(value);

      return () -> testList.remove(value);
    }

    @Action
    @ValueSource(ints = {-2, -1, 0, 1, 7, 11})
    public Runnable removeIndex(int index) {
      int indexToRemove = index < 0 ? expected.size() + index : index;

      assumeTrue(indexToRemove >= 0 && indexToRemove < expected.size());

      expected.remove(indexToRemove);

      return () -> testList.remove(indexToRemove);
    }

    @Action
    @ValueSource(strings = {
      "T,H,E,Q,U,I,C,K,B,R,O,W,N,F,O,X,J,U,M,P,S,O,V,E,R,T,H,E,L,A,Z,Y,D,O,G"
    })
    public Runnable removeMultipleElements(String value) {
      String[] split = value.split(",");
      Stream.of(split).forEach(expected::remove);

      return () -> Stream.of(split).forEach(testList::remove);
    }

    @Action
    public Runnable clear() {
      expected.clear();

      return () -> testList.clear();
    }
  }
}
