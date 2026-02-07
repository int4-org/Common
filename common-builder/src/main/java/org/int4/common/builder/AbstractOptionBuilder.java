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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.int4.common.function.QuadConsumer;
import org.int4.common.function.TriConsumer;

/**
 * Base class for fluent builders that collect deferred configuration
 * options to be applied after object creation.
 * <p>
 * Subclasses register configuration steps via {@link #apply(Consumer)}
 * (or convenience methods built on top of it). These options are applied
 * in the order they were added when {@link #initialize(Object)} is invoked.
 * <p>
 * This class intentionally does not define object instantiation; concrete
 * subclasses are responsible for creating the target instance and then
 * calling {@code initialize(...)} to apply all collected options.
 *
 * @param <O> the type of object being configured
 * @param <B> the concrete builder type (self type)
 */
public abstract class AbstractOptionBuilder<O, B extends AbstractOptionBuilder<O, B>> {
  private final List<Consumer<? super O>> options = new ArrayList<>();

  /**
   * Constructs a new instance.
   */
  protected AbstractOptionBuilder() {
  }

  /**
   * Returns this builder cast to its concrete self type.
   * <p>
   * This method supports fluent APIs without requiring casts in subclasses.
   *
   * @return this builder instance
   */
  @SuppressWarnings("unchecked")
  protected final B self() {
    return (B)this;
  }

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
  public final B apply(Consumer<? super O> option) {
    Objects.requireNonNull(option, "option");

    options.add(option);

    return self();
  }

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
  public final <U> B apply(BiConsumer<? super O, ? super U> option, U parameter) {
    Objects.requireNonNull(option, "option");

    options.add(o -> option.accept(o, parameter));

    return self();
  }

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
  public final <U, V> B apply(TriConsumer<? super O, ? super U, ? super V> option, U parameter1, V parameter2) {
    Objects.requireNonNull(option, "option");

    options.add(o -> option.accept(o, parameter1, parameter2));

    return self();
  }

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
  public final <U, V, W> B apply(QuadConsumer<? super O, ? super U, ? super V, ? super W> option, U parameter1, V parameter2, W parameter3) {
    Objects.requireNonNull(option, "option");

    options.add(o -> option.accept(o, parameter1, parameter2, parameter3));

    return self();
  }

  /**
   * Applies all registered configuration options to the given object.
   * <p>
   * Subclasses typically call this method immediately after creating the
   * target instance.
   *
   * @param instance the object to initialize, cannot be {@code null}
   * @return the now initialized input object for fluent chaining, never {@code null}
   * @throws NullPointerException if the given object is {@code null}
   */
  protected final O initialize(O instance) {
    Objects.requireNonNull(instance, "instance");

    for(Consumer<? super O> option : options) {
      option.accept(instance);
    }

    return instance;
  }
}
