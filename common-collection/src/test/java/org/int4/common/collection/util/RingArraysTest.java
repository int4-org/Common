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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RingArraysTest {
  String[] src = {"A", "B", "C", "D", "E", "F", "G", "H"};

  @Nested
  class CopyWithinSameArray {

    @Test
    void shouldRejectInvalidLengthParameter() {
      assertThatThrownBy(() -> RingArrays.copy(src, 1, 2, -1)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> RingArrays.copy(src, 1, 2, 5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidPositionParameters() {
      assertThatThrownBy(() -> RingArrays.copy(src, -1, 2, 3)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> RingArrays.copy(src, 1, -1, 3)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> RingArrays.copy(src, 8, 2, 3)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> RingArrays.copy(src, 1, 8, 3)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void shouldWorkForAllValidInputs() {
      for(int srcPos = 0; srcPos < src.length; srcPos++) {
        for(int destPos = 0; destPos < src.length; destPos++) {
          for(int length = 0; length <= (src.length / 2); length++) {
            String[] copy = src.clone();
            String[] expected = src.clone();
            int s = srcPos;
            int d = destPos;
            int i = length;

            while(i-- > 0) {
              expected[d++] = src[s++];

              if(s >= src.length) {
                s = 0;
              }
              if(d >= src.length) {
                d = 0;
              }
            }

            RingArrays.copy(copy, srcPos, destPos, length);
            int sp = srcPos;
            int dp = destPos;
            int l = length;

            assertThat(copy)
              .as(() -> "copy from " + sp + " to " + dp + " of length " + l)
              .isEqualTo(expected);
          }
        }
      }
    }
  }

  @Nested
  class CopyUsingDistinctArrays {
    String[] dest = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    @Test
    void shouldRejectInvalidLengthParameter() {
      assertThatThrownBy(() -> RingArrays.copy(src, 1, dest, 2, -1)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> RingArrays.copy(src, 1, dest, 2, 9)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> RingArrays.copy(src, 1, new String[6], 2, 7)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidPositionParameters() {
      assertThatThrownBy(() -> RingArrays.copy(src, -1, dest, 2, 3)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> RingArrays.copy(src, 1, dest, -1, 3)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> RingArrays.copy(src, 8, dest, 8, 3)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> RingArrays.copy(src, 1, dest, 10, 3)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void shouldRejectSameArray() {
      assertThatThrownBy(() -> RingArrays.copy(src, 1, src, 2, 2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldWorkForAllValidInputs() {
      for(int srcPos = 0; srcPos < src.length; srcPos++) {
        for(int destPos = 0; destPos < dest.length; destPos++) {
          for(int length = 0; length <= 8; length++) {
            String[] expected = dest.clone();
            String[] target = dest.clone();
            int s = srcPos;
            int d = destPos;
            int i = length;

            while(i-- > 0) {
              expected[d++] = src[s++];

              if(s >= src.length) {
                s = 0;
              }
              if(d >= dest.length) {
                d = 0;
              }
            }

            RingArrays.copy(src, srcPos, target, destPos, length);
            int sp = srcPos;
            int dp = destPos;
            int l = length;

            assertThat(target)
              .as(() -> "copy from " + sp + " to " + dp + " of length " + l)
              .isEqualTo(expected);
          }
        }
      }
    }
  }
}
