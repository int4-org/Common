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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ExploratoryTestRunnerTest {

  @Test
  void shouldSupportValueSource() {
    ExploratoryTestRunner.explore(ValueExplorable.class, ValueExplorable::new, 2);
  }

  @Test
  void shouldSupportEnumSource() {
    ExploratoryTestRunner.explore(EnumExplorable.class, EnumExplorable::new, 2);
  }

  @Test
  void shouldSupportMethodSource() {
    ExploratoryTestRunner.explore(MethodExplorable.class, MethodExplorable::new, 2);
  }

  @Test
  void shouldSkipActionsWhenAssumptionFails() {
    ExploratoryTestRunner.explore(AssumptionExplorable.class, AssumptionExplorable::new, 2);
  }

  @Test
  void shouldFailWhenRunnableModifiesState() {
    assertThatThrownBy(() -> ExploratoryTestRunner.explore(IllegalActionExplorable.class, IllegalActionExplorable::new, 2))
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("violates strict seperation");
  }

  @Test
  void shouldFailWhenActionHasParametersButNoSource() {
    assertThatThrownBy(() -> ExploratoryTestRunner.explore(ParamNoSourceExplorable.class, ParamNoSourceExplorable::new, 2))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Action method has parameters but no @ArgumentsSource");
  }

  @Test
  void shouldFailWhenActionReturnsWrongType() {
    assertThatThrownBy(() -> ExploratoryTestRunner.explore(WrongReturnTypeExplorable.class, WrongReturnTypeExplorable::new, 2))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("must return a Runnable");
  }

  @Test
  void shouldFailWhenNoActionsFound() {
    assertThatThrownBy(() -> ExploratoryTestRunner.explore(NoActionsExplorable.class, NoActionsExplorable::new, 2))
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("must define at least one action and assertion");
  }

  @Test
  void shouldFailWhenNoAssertionsFound() {
    assertThatThrownBy(() -> ExploratoryTestRunner.explore(NoAssertionsExplorable.class, NoAssertionsExplorable::new, 2))
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("must define at least one action and assertion");
  }

  @Test
  void shouldSupportMultipleFailures() {
    assertThatThrownBy(() -> ExploratoryTestRunner.explore(MultipleFailuresExplorable.class, MultipleFailuresExplorable::new, 2))
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("Failure 1")
      .hasMessageContaining("Failure 2");
  }

  @Test
  void shouldSupportHistoryPruning() {
    ExploratoryTestRunner.explore(HistoryExplorable.class, HistoryExplorable::new, 4, 2);
  }

  @Test
  void shouldReportFailingPath() {
    assertThatThrownBy(() -> ExploratoryTestRunner.explore(FailingExplorable.class, FailingExplorable::new, 5))
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("Path 0 failed:")
      .hasMessageContaining("- triggerFailure -> [FAILED]");
  }

  @Test
  void shouldFailWhenNoActionsOrAssertionsFound() {
    assertThatThrownBy(() -> ExploratoryTestRunner.explore(EmptyExplorable.class, EmptyExplorable::new, 2))
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("must define at least one action and assertion");
  }

  public static class ValueExplorable implements Explorable {
    List<String> expected = new ArrayList<>();
    List<String> items = new ArrayList<>();

    @Action
    @ValueSource(strings = {"A", "B"})
    public Runnable add(String s) {
      expected.add(s);

      return () -> items.add(s);
    }

    @Assertion
    public void check() {
      assertThat(items).isEqualTo(expected);
    }

    @Override
    public Object snapshot() {
      return new ArrayList<>(expected);
    }
  }

  public static class EnumExplorable implements Explorable {
    public enum Color { RED, GREEN, BLUE }

    List<Color> expected = new ArrayList<>();
    List<Color> colors = new ArrayList<>();

    @Action
    @EnumSource(Color.class)
    public Runnable addColor(Color color) {
      expected.add(color);

      return () -> colors.add(color);
    }

    @Assertion
    public void check() {
      assertThat(colors).isEqualTo(expected);
    }

    @Override
    public Object snapshot() {
      return new ArrayList<>(expected);
    }
  }

  public static class MethodExplorable implements Explorable {
    List<Integer> expected = new ArrayList<>();
    List<Integer> numbers = new ArrayList<>();

    @Action
    @MethodSource("provideNumbers")
    public Runnable addNumber(int n) {
      expected.add(n);

      return () -> numbers.add(n);
    }

    static Stream<Arguments> provideNumbers() {
      return Stream.of(Arguments.of(1), Arguments.of(2));
    }

    @Assertion
    public void check() {
      assertThat(numbers).isEqualTo(expected);
    }

    @Override
    public Object snapshot() {
      return new ArrayList<>(expected);
    }
  }

  public static class AssumptionExplorable implements Explorable {
    boolean actionRun = false;

    @Action
    public Runnable skipMe() {
      assumeTrue(false);

      return () -> actionRun = true;
    }

    @Assertion
    public void check() {
      assertThat(actionRun).isFalse();
    }

    @Override
    public Object snapshot() {
      return actionRun;
    }
  }

  public static class IllegalActionExplorable implements Explorable {
    int state = 0;

    @Action
    public Runnable illegal() {
      return () -> state = 1; // Modification in Runnable!
    }

    @Assertion
    public void check() {}

    @Override
    public Object snapshot() {
      return state;
    }
  }

  public static class FailingExplorable implements Explorable {
    boolean expectedFailed = false;
    boolean actualFailed = false;

    @Action
    public Runnable triggerFailure() {
      expectedFailed = true;

      return () -> actualFailed = true;
    }

    @Assertion
    public void check() {
      if(actualFailed) {
        fail("Expected failure");
      }
    }

    @Override
    public Object snapshot() {
      return expectedFailed ? "[FAILED]" : "[OK]";
    }
  }

  public static class EmptyExplorable implements Explorable {
    @Override public Object snapshot() { return ""; }
  }

  public static class ParamNoSourceExplorable implements Explorable {
    @Action public Runnable doSomething(@SuppressWarnings("unused") int x) { return () -> {}; }
    @Assertion public void check() {}
    @Override public Object snapshot() { return ""; }
  }

  public static class WrongReturnTypeExplorable implements Explorable {
    @Action public String doSomething() { return ""; }
    @Assertion public void check() {}
    @Override public Object snapshot() { return ""; }
  }

  public static class NoActionsExplorable implements Explorable {
    @Assertion public void check() {}
    @Override public Object snapshot() { return ""; }
  }

  public static class NoAssertionsExplorable implements Explorable {
    @Action public Runnable doSomething() { return () -> {}; }
    @Override public Object snapshot() { return ""; }
  }

  public static class MultipleFailuresExplorable implements Explorable {
    @Action public Runnable failMe() { return () -> {}; }
    @Assertion
    public void check() {
      Assertions.assertAll(
        "Multiple Failures",
        () -> fail("Failure 1"),
        () -> fail("Failure 2")
      );
    }
    @Override public Object snapshot() { return ""; }
  }

  public static class HistoryExplorable implements Explorable {
    int expectedCount = 0;
    int actualCount = 0;

    @Action
    public Runnable inc() {
      expectedCount++;

      return () -> actualCount++;
    }

    @Assertion public void check() {}

    @Override
    public Object snapshot() {
      return expectedCount % 2; // State cycles 0, 1, 0, 1...
    }
  }
}
