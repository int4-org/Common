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

/**
 * Optional capabilities that collections can implement to tell other code, such as
 * {@link org.int4.common.collection.Immutable}, what they can do cheaply.
 * <ul>
 *   <li>{@link org.int4.common.collection.capability.ImmutabilityAware}: can tell whether
 *   it is immutable.</li>
 *   <li>{@link org.int4.common.collection.capability.Freezable}: can be made permanently
 *   immutable in place.</li>
 *   <li>{@link org.int4.common.collection.capability.Forkable}: can create a cheap, independent,
 *   modifiable copy.</li>
 *   <li>{@link org.int4.common.collection.capability.Snapshottable}: can create a cheap,
 *   independent, immutable copy.</li>
 * </ul>
 * Each capability is independent; a collection implements only those it can support. A promise
 * of being cheap or independent is part of each contract, and cannot be verified by callers.
 */
package org.int4.common.collection.capability;
