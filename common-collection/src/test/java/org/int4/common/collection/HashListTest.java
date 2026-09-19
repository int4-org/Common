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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HashListTest extends AbstractListTest {

  {
    // Tuned for about 10 second runtime
    stressTestOperations = 25000;
    exhaustiveTestRange = 9;
  }

  @Override
  protected List<String> createList() {
    return new HashList<>();
  }

  @Override
  protected boolean supportsAddIndexT() {
    return false;
  }

  @Override
  protected boolean supportsSetIndexT() {
    return false;
  }

  @Override
  protected boolean supportsListIteratorAdd() {
    return false;
  }

  @Override
  protected boolean supportsListIteratorSet() {
    return false;
  }

  @Override
  protected boolean supportsNullElements() {
    return false;
  }

  @Test
  void shouldUpdateTailIndexWhenTailIsRemoved() {
    HashList<Key> list = new HashList<>();

    // fill enough to enter hash mode
    for(int i = 0; i < 64; i++) {
      list.add(new Key("filler-" + i, i + 1));
    }

    Key first = new Key("first", 0);
    Key second = new Key("second", 0);
    Key third = new Key("third", 0);
    Key other = new Key("other", -1);  // hashes to a different bucket than the colliding keys

    /*
     * 'other' is placed directly before 'first' so that a stale tail pointer (pointing
     * one slot too low) would corrupt 'other's chain instead of 'first's, which makes
     * 'third' unreachable from its own bucket.
     */

    list.add(other);
    list.add(first);
    list.add(second);  // second is the tail of first's bucket chain

    assertThat(list.remove(second)).isTrue();

    list.add(third);  // must be linked after 'first', not after 'other'

    assertThat(list.subList(list.size() - 3, list.size())).containsExactly(other, first, third);
    assertThat(list.contains(third)).isTrue();

    // chain integrity: both colliding elements remain individually removable
    assertThat(list.remove(first)).isTrue();
    assertThat(list.remove(third)).isTrue();
    assertThat(list).doesNotContain(first, third);
  }

  @Test
  void removeShouldRemoveFirstOccurrenceOfCollidingElements() {
    HashList<String> list = new HashList<>();

    // enough filler to push the list into hash mode
    for(int i = 0; i < 64; i++) {
      list.add("filler-" + i);
    }

    String first = "AaAa";
    String other = "AaBB";  // collides with "AaAa"
    String last = "BBAa";   // collides with "AaAa"

    list.add(first);
    list.add(other);
    list.add(first);
    list.add(last);
    list.add(first);

    assertThat(list.remove(first)).isTrue();

    assertThat(list.subList(list.size() - 4, list.size())).containsExactly(other, first, last, first);
    assertThat(list.indexOf(first)).isEqualTo(list.size() - 3);
    assertThat(list.lastIndexOf(first)).isEqualTo(list.size() - 1);
  }

  private static final class Key {
    private final String name;
    private final int hash;

    Key(String name, int hash) {
      this.name = name;
      this.hash = hash;
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Key key && name.equals(key.name);
    }

    @Override
    public String toString() {
      return name;
    }
  }

}
