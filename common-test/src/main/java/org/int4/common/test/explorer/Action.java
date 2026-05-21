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

package org.int4.common.test.explorer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.params.provider.ValueSource;

/**
 * Marks a method as an action that can be applied to an {@link Explorable} model.
 * <p>
 * Actions are executed during exhaustive exploration of the model's state space.
 * They may modify the state and are combined with assertions to verify correct behavior.
 * <p>
 * Methods annotated with {@code @Action} may optionally take parameters if used
 * with a {@link ValueSource} or other parameterized provider.
 * <p>
 * An action may be conditionally skipped using assumptions (e.g.,
 * {@link org.junit.jupiter.api.Assumptions#assumeTrue(boolean)}). If an assumption fails,
 * the action is aborted and the path continues without treating it as a failure.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {}
