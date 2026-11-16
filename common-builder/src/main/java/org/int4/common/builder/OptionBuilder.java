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

package org.int4.common.builder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.int4.common.function.QuadConsumer;
import org.int4.common.function.TriConsumer;

/**
 * A builder that accepts deferred configuration options.
 * <p>
 * Options are registered through the {@code apply} methods and are applied, in
 * registration order, when the builder initializes the object it produces.
 * <p>
 * This interface captures only the option registration part of a fluent
 * builder. Implementations remain responsible for creating the target object,
 * applying the registered options, and returning the concrete builder type.
 * See {@link AbstractOptionBuilder} for a reusable implementation.
 *
 * @param <O> the type of object being configured
 * @param <B> the concrete builder type (self type)
 */
public interface OptionBuilder<O, B extends OptionBuilder<O, B>> {

  /**
   * Registers a configuration option to be applied to the target object
   * during initialization.
   * <p>
   * Options are executed in the order they are registered.
   *
   * @param option a configuration option to apply, cannot be {@code null}
   * @return the fluent builder, never {@code null}
   * @throws NullPointerException if the option is {@code null}
   */
  B apply(Consumer<? super O> option);

  /**
   * Registers a configuration option with a parameter to be applied to the
   * target object during initialization.
   * <p>
   * Options are executed in the order they are registered.
   *
   * @param <U> the type of the parameter to supply to the option
   * @param option a configuration option to apply, cannot be {@code null}
   * @param parameter a parameter to supply to the option
   * @return the fluent builder, never {@code null}
   * @throws NullPointerException if the option is {@code null}
   */
  <U> B apply(BiConsumer<? super O, ? super U> option, U parameter);

  /**
   * Registers a configuration option with two parameters to be applied to the
   * target object during initialization.
   * <p>
   * Options are executed in the order they are registered.
   *
   * @param <U> the type of the first parameter to supply to the option
   * @param <V> the type of the second parameter to supply to the option
   * @param option a configuration option to apply, cannot be {@code null}
   * @param parameter1 the first parameter to supply to the option
   * @param parameter2 the second parameter to supply to the option
   * @return the fluent builder, never {@code null}
   * @throws NullPointerException if the option is {@code null}
   */
  <U, V> B apply(TriConsumer<? super O, ? super U, ? super V> option, U parameter1, V parameter2);

  /**
   * Registers a configuration option with three parameters to be applied to the
   * target object during initialization.
   * <p>
   * Options are executed in the order they are registered.
   *
   * @param <U> the type of the first parameter to supply to the option
   * @param <V> the type of the second parameter to supply to the option
   * @param <W> the type of the third parameter to supply to the option
   * @param option a configuration option to apply, cannot be {@code null}
   * @param parameter1 the first parameter to supply to the option
   * @param parameter2 the second parameter to supply to the option
   * @param parameter3 the third parameter to supply to the option
   * @return the fluent builder, never {@code null}
   * @throws NullPointerException if the option is {@code null}
   */
  <U, V, W> B apply(QuadConsumer<? super O, ? super U, ? super V, ? super W> option, U parameter1, V parameter2, W parameter3);
}
