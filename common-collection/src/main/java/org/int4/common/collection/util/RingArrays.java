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

import java.util.Arrays;
import java.util.Objects;

/**
 * Tools for arrays that wrap around like ring buffers.
 * <p>
 * A ring array is an ordinary array that is used as a circular buffer: its sequence of elements
 * starts at some index and continues to the end of the array, after which it wraps around to
 * index 0. This allows a sequence to be shifted by moving only the elements on one side of a
 * position (the shorter side), and by moving where the sequence starts, instead of always
 * moving everything after the position.
 * <p>
 * The methods in this class work on physical array positions and know nothing about where a
 * sequence starts or how many elements are in use; the caller keeps track of that. A range of
 * positions that reaches the end of an array continues at index 0 of that array. Operations
 * are performed with a few {@link System#arraycopy} or {@link Arrays#fill} calls on contiguous
 * parts.
 */
public class RingArrays {

  /**
   * Copies elements from the source array to the destination array, treating both
   * arrays as circular (ring) buffers. The two arrays must be distinct. A range that
   * reaches the end of either array continues at index 0 of that array.
   *
   * @param <T> the component type of the arrays
   * @param src the source array, cannot be {@code null}, or the same as the destination array
   * @param srcPos the starting index in the source array, cannot be negative, and must be less than the source array's length
   * @param dest the destination array, cannot be {@code null}, or the same as the source array
   * @param destPos the starting index in the destination array, cannot be negative, and must be less than the destination array's length
   * @param length the number of elements to copy, cannot be negative, or exceed either array's length
   * @throws IndexOutOfBoundsException if {@code srcPos} or {@code destPos} are out of range
   * @throws IllegalArgumentException if the source array equals the destination array, if the length is negative, or the length
   *   exceeds either array's length
   * @throws NullPointerException if any argument is {@code null}
   */
  public static <T> void copy(T[] src, int srcPos, T[] dest, int destPos, int length) {
    Objects.checkIndex(srcPos, src.length);
    Objects.checkIndex(destPos, dest.length);

    if(src == dest) {
      throw new IllegalArgumentException("src cannot be the same as dest");
    }
    if(length < 0 || length > src.length || length > dest.length) {
      throw new IllegalArgumentException("length cannot be negative, and must not exceed the length of either array: " + length);
    }

    // max that can be copied before we reach the end of either array or the required number of elements:
    int max = Math.min(length, Math.min(src.length - srcPos, dest.length - destPos));

    // the amount that remains uncopied (just recurse if greater than zero):
    int overflow = length - max;

    if(overflow > 0) {
      int newSrcPos = srcPos + max;
      int newDestPos = destPos + max;

      if(newSrcPos >= src.length) {
        newSrcPos -= src.length;
      }
      if(newDestPos >= dest.length) {
        newDestPos -= dest.length;
      }

      copy(src, newSrcPos, dest, newDestPos, overflow);
    }

    System.arraycopy(src, srcPos, dest, destPos, max);
  }

  /**
   * Copies elements within an array, treating it as a circular (ring) buffer. The
   * maximum number of elements that can be copied is half the array's length rounded
   * down (as otherwise a temporary array would need to be created). A range that
   * reaches the end of the array continues at index 0.
   * <p>
   * The source and destination ranges may overlap; the result is as if the source
   * elements were first copied to a temporary array. Elements outside the destination
   * range are not changed.
   *
   * @param <T> the component type of the arrays
   * @param array the array, cannot be {@code null}
   * @param srcPos the source index, cannot be negative, and must be less than the array's length
   * @param destPos the destination index, cannot be negative, and must be less than the array's length
   * @param length the number of elements to copy, cannot be negative, or exceed half the array's length rounded down
   * @throws IndexOutOfBoundsException if {@code srcPos} or {@code destPos} are out of range
   * @throws IllegalArgumentException if the length is negative, or the length exceeds half the array's length rounded down
   * @throws NullPointerException if any argument is {@code null}
   */
  public static <T> void copy(T[] array, int srcPos, int destPos, int length) {
    final int arrayLength = array.length;

    Objects.checkIndex(srcPos, arrayLength);
    Objects.checkIndex(destPos, arrayLength);

    if(length < 0 || length > arrayLength >> 1) {
      throw new IllegalArgumentException("length cannot be negative, and cannot span more than half the array's length: " + length);
    }

    // max that can be copied before we reach the end of the array or the required number of elements:
    int max = Math.min(length, Math.min(arrayLength - srcPos, arrayLength - destPos));

    // checks if ranges overlap, and if so determines forward or backward copy direction:
    int d = destPos - srcPos;
    boolean shiftRight = (d < 0 ? d + arrayLength : d) < length;

    if(!shiftRight) {
      System.arraycopy(array, srcPos, array, destPos, max);
    }

    // the amount that remains uncopied (just recurse if greater than zero):
    int overflow = length - max;

    if(overflow > 0) {
      int newSrcPos = srcPos + max;
      int newDestPos = destPos + max;

      if(newSrcPos >= arrayLength) {
        newSrcPos -= arrayLength;
      }
      if(newDestPos >= arrayLength) {
        newDestPos -= arrayLength;
      }

      copy(array, newSrcPos, newDestPos, overflow);
    }

    if(shiftRight) {
      System.arraycopy(array, srcPos, array, destPos, max);
    }
  }

  /**
   * Sets elements of an array to {@code null}, treating it as a circular (ring) buffer: the
   * given number of elements is cleared, starting at the given position and continuing at
   * index 0 when the end of the array is reached. This is typically used to release references
   * to elements that were moved elsewhere.
   * <p>
   * The length of the array must be a power of two. Unlike the copy methods, the arguments
   * are only validated when assertions are enabled; the result is undefined if they are out
   * of range.
   *
   * @param <T> the component type of the array
   * @param array the array, cannot be {@code null}, and its length must be a power of two
   * @param pos the index of the first element to clear, cannot be negative, and must be less than the array's length
   * @param length the number of elements to clear, cannot be negative, or exceed the array's length
   * @throws NullPointerException if {@code array} is {@code null}
   */
  public static <T> void clear(T[] array, int pos, int length) {
    assert Integer.highestOneBit(array.length) == array.length;
    assert pos >= 0 && pos < array.length : "pos out of range: " + pos;
    assert length <= array.length;

    int max = Math.min(length, array.length - pos);

    Arrays.fill(array, pos, pos + max, null);

    int overflow = length - max;

    if(overflow > 0) {
      int mask = array.length - 1;
      int fromIndex = (pos + max) & mask;

      Arrays.fill(array, fromIndex, fromIndex + overflow, null);
    }
  }
}
