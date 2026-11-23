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

package org.int4.common.collection.capability;

/**
 * Implemented by collections that can create a cheap, independent, modifiable copy of themselves.
 * <p>
 * The copy must be:
 * <ul>
 *   <li><strong>Independent:</strong> structural modifications of the source after the call are never
 *   visible in the copy, and the reverse. Elements may be shared.</li>
 *   <li><strong>Cheap:</strong> substantially cheaper than copying the elements one by one. A type that
 *   cannot do this must not implement this interface.</li>
 *   <li><strong>Modifiable:</strong> also when the source is immutable.</li>
 * </ul>
 * A view of the source is not a fork.
 *
 * @param <T> the type of the copy
 */
public interface Forkable<T> {

  /**
   * Creates a cheap, independent, modifiable copy of this instance.
   *
   * @return a copy, never {@code null}
   */
  T fork();
}
