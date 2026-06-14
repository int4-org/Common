/**
 * Provides a framework for exhaustive state-space exploration.
 * <p>
 * This package allows for testing complex state machines by systematically applying
 * sequences of {@link org.int4.common.test.explorer.Action Actions} and verifying
 * results with {@link org.int4.common.test.explorer.Assertion Assertions}.
 * <p>
 * The main entry point is {@link org.int4.common.test.explorer.ExploratoryTestRunner},
 * which explores an {@link org.int4.common.test.explorer.Explorable} model to find
 * inconsistent or illegal states.
 */
package org.int4.common.test.explorer;
