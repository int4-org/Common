/*
 * MIT License
 *
 * Copyright (C) 2025 John Hendrikx
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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * THE SOFTWARE.
 */

package org.int4.common.function;

import java.util.Objects;

/**
 * Represents a function that accepts one argument and produces a result,
 * potentially throwing a checked exception during its execution.
 *
 * <p>This is similar to {@link java.util.function.Function}, but allows
 * for checked exceptions to be thrown.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <X> the type of checked exception that may be thrown
 */
public interface ThrowingFunction<T, R, X extends Throwable> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws X if a checked exception occurs during function execution
     */
    R apply(T t) throws X;

    /**
     * Returns a composed {@code ThrowingFunction} that first applies this
     * function to its input, and then applies the {@code after} function
     * to the result.
     *
     * <p>If either function throws an exception, it is propagated to the
     * caller.
     *
     * @param <V> the type of output of the {@code after} function, and of
     *   the composed function
     * @param after the function to apply after this function, cannot be {@code null}
     * @return a composed {@code ThrowingFunction} that applies this
     *   function and then the {@code after} function
     * @throws NullPointerException if {@code after} is null
     */
    default <V> ThrowingFunction<T, V, X> andThen(ThrowingFunction<? super R, ? extends V, X> after) {
        Objects.requireNonNull(after);

        return t -> after.apply(apply(t));
    }
}
