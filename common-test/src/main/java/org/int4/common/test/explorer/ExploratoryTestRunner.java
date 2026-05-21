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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.int4.common.function.ThrowingConsumer;
import org.int4.common.function.ThrowingFunction;
import org.junit.jupiter.api.extension.ExecutableInvoker;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.junit.platform.commons.support.ReflectionSupport;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;
import org.opentest4j.TestAbortedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A test runner that performs exhaustive exploration of an {@link Explorable} model's state space.
 * <p>
 * The runner discovers methods annotated with {@link Action} and {@link Assertion} in the
 * provided {@link Explorable} class. It then systematically executes all possible sequences
 * of actions, using the {@link Explorable#snapshot()} to identify and prune already visited states.
 * <p>
 * This approach is particularly effective for testing complex state machines or UI controls
 * where many different interaction sequences could lead to an inconsistent state.
 *
 * <h2>The Hidden State Problem</h2>
 * Pruning is essential for performance but relies on the {@link Explorable#snapshot()} being
 * a complete representation of the system's state. If the system has "hidden state", internal
 * variables (like memory pointers, hash buckets, or caches) that aren't reflected in the
 * snapshot, the runner may prune a "corrupted" state because it is logically identical to a
 * previously seen "healthy" state.
 * <p>
 * There are two primary solutions to this problem:
 * <ol>
 *   <li><b>Deep Snapshots:</b> Include internal structural metadata in the snapshot so that
 *       two states are only considered equal if their internal representations also match.</li>
 *   <li><b>History-Based Pruning:</b> Use the optional {@code historyDepth} parameter to
 *       include the path taken to reach a state in its identity. This prevents the runner
 *       from taking "clean shortcuts" that might mask a bug reached through a "dirty"
 *       transition.</li>
 * </ol>
 */
public final class ExploratoryTestRunner {

  /**
   * Explores all reachable states of the specified {@link Explorable} class with
   * infinite depth and standard state-based pruning.
   *
   * @param <E> the explorable class type
   * @param explorableClass the class to explore, must implement {@link Explorable} and
   *   have a public no-arg constructor
   * @param instantiator a supplier for the explorable instance
   * @throws AssertionError if any assertion fails during exploration
   * @throws MultipleFailuresError if multiple assertions fail for a single state
   * @throws IllegalStateException if an unexpected error occurs during exploration
   */
  public static <E extends Explorable> void explore(Class<E> explorableClass, Supplier<E> instantiator) {
    explore(explorableClass, instantiator, Integer.MAX_VALUE, 1);
  }

  /**
   * Explores all reachable states of the specified {@link Explorable} class up to
   * a maximum depth.
   *
   * @param <E> the explorable class type
   * @param explorableClass the class to explore, must implement {@link Explorable} and
   *   have a public no-arg constructor
   * @param instantiator a supplier for the explorable instance
   * @param maxDepth the maximum depth of actions to apply from the initial state
   * @throws AssertionError if any assertion fails during exploration
   * @throws MultipleFailuresError if multiple assertions fail for a single state
   * @throws IllegalStateException if an unexpected error occurs during exploration
   */
  public static <E extends Explorable> void explore(Class<E> explorableClass, Supplier<E> instantiator, int maxDepth) {
    explore(explorableClass, instantiator, maxDepth, 1);
  }

  /**
   * Explores all reachable states of the specified {@link Explorable} class up to
   * a maximum depth, using history-based pruning.
   *
   * @param <E> the explorable class type
   * @param explorableClass the class to explore, must implement {@link Explorable} and
   *   have a public no-arg constructor
   * @param instantiator a supplier for the explorable instance
   * @param maxDepth the maximum depth of actions to apply from the initial state
   * @param historyDepth the number of consecutive states to use as the pruning key.
   *   A depth of 1 prunes based on the current state only. A depth of 2 prunes
   *   based on the transition (previous state + current state), which can
   *   unmask bugs hidden by logical state equality.
   * @throws AssertionError if any assertion fails during exploration
   * @throws MultipleFailuresError if multiple assertions fail for a single state
   * @throws IllegalStateException if an unexpected error occurs during exploration
   */
  public static <E extends Explorable> void explore(Class<E> explorableClass, Supplier<E> instantiator, int maxDepth, int historyDepth) {
    doAll(explorableClass, instantiator, maxDepth, historyDepth);
  }

  record NamedAction(String description, ThrowingFunction<Explorable, Runnable, Exception> executor) {}

  record RecordedStep(RecordedStep previous, NamedAction action, Object resultingState) {

    public void apply(Explorable instance) throws Exception {
      if(previous != null) {
        previous.apply(instance);
      }

      action.executor().apply(instance).run();
    }

    @Override
    public final String toString() {
      return "RecordedStep[" + action.description + " -> " + resultingState + "]";
    }
  }

  record Path(RecordedStep previousSteps, NamedAction action, int depth) {

    public List<RecordedStep> steps() {  // not called on critical path
      if(previousSteps == null) {
        return List.of();
      }

      List<RecordedStep> list = new ArrayList<>(depth);
      RecordedStep step = previousSteps;

      for(int i = 1; i < depth; i++) {
        list.add(step);

        step = step.previous;
      }

      return list.reversed();
    }

    @Override
    public String toString() {
      return "Path[steps=" + steps() + " -> " + action.description + "]";
    }
  }

  static class FailureAggregatingException extends Exception {
    private List<Throwable> failures;

    public FailureAggregatingException(List<Throwable> failures) {
      this.failures = failures;
    }

    public List<Throwable> getFailures() {
      return failures;
    }
  }

  static <E extends Explorable> void doAll(Class<E> testClass, Supplier<E> instantiator, int maxDepth, int historyDepth) {
    List<NamedAction> actions = new ArrayList<>();
    List<Method> assertions = new ArrayList<>();

    for(Method method : testClass.getDeclaredMethods()) {
      if(method.getAnnotation(Action.class) != null) {
        if(Runnable.class.isAssignableFrom(method.getReturnType())) {
          List<Object[]> argumentSets = new ArrayList<>();

          for(Annotation annotation : method.getAnnotations()) {
            ArgumentsSource source = annotation.annotationType().getAnnotation(ArgumentsSource.class);

            if(source != null) {
              try {
                ArgumentsProvider provider = ReflectionSupport.newInstance(source.value());

                if(provider instanceof AnnotationConsumer<?> consumer) {
                  @SuppressWarnings("unchecked")
                  AnnotationConsumer<Annotation> castedConsumer = (AnnotationConsumer<Annotation>)consumer;

                  castedConsumer.accept(annotation);
                }

                provider.provideArguments(createExtensionContext(method)).forEach(args -> argumentSets.add(args.get()));
              }
              catch(Exception e) {
                throw new IllegalStateException("Failed to provide arguments for method: " + method, e);
              }
            }
          }

          if(argumentSets.isEmpty()) {
            if(method.getParameterCount() == 0) {
              actions.add(new NamedAction(method.getName(), instance -> (Runnable)method.invoke(instance)));
            }
            else {
              throw new IllegalStateException("Action method has parameters but no @ArgumentsSource: " + method);
            }
          }
          else {
            for(Object[] args : argumentSets) {
              String description = method.getName() + "(" + Stream.of(args).map(Objects::toString).collect(Collectors.joining(", ")) + ")";

              actions.add(new NamedAction(description, instance -> (Runnable)method.invoke(instance, args)));
            }
          }
        }
        else {
          throw new IllegalStateException("Method annotated with @Action must return a Runnable with the effect to execute: " + method);
        }
      }

      if(method.getAnnotation(Assertion.class) != null) {
        assertions.add(method);
      }
    }

    // Ensure actions are in a deterministic order for test reproducability:
    actions.sort(Comparator.comparing(NamedAction::description));

    if(actions.isEmpty() || assertions.isEmpty()) {
      fail("The test class must define at least one action and assertion: " + testClass);
    }

    Set<Object> seenStates = new HashSet<>();
    Deque<Path> unexploredPaths = new ArrayDeque<>();

    for(NamedAction action : actions) {
      unexploredPaths.add(new Path(null, action, 1));
    }

    ThrowingConsumer<Object, FailureAggregatingException> asserter = instance -> {
      List<Throwable> failures = new ArrayList<>();

      for(Method assertion : assertions) {
        try {
          assertion.invoke(instance);
        }
        catch(InvocationTargetException e) {
          if(e.getCause() instanceof MultipleFailuresError mfe) {
            failures.addAll(mfe.getFailures());
          }
          else {
            failures.add(e.getCause());
          }
        }
        catch(Throwable t) {
          failures.add(t);
        }
      }

      if(!failures.isEmpty()) {
        throw new FailureAggregatingException(failures);
      }
    };

    int pathsExplored = 0;
    int maxPathLength = 0;
    long lastUpdate = System.currentTimeMillis();

    while(!unexploredPaths.isEmpty()) {
      Path path = unexploredPaths.pop();
      Object finalState = null;

      try {
        Explorable instance = instantiator.get();

        finalState = executePath(instance, path, asserter);

        asserter.accept(instance);

        pathsExplored++;
        maxPathLength = Math.max(maxPathLength, path.depth);

        if(System.currentTimeMillis() - lastUpdate > 500) {
          System.out.printf("\rPaths: %-7d | Queue: %-7d | Depth: %-7d   ", pathsExplored, unexploredPaths.size(), maxPathLength);

          lastUpdate = System.currentTimeMillis();
        }

        Object pruningKey = historyDepth == 1 ? finalState : createPruningKey(historyDepth, path, finalState);

        if(seenStates.add(pruningKey) && path.depth < maxDepth) {
          // not seen before state found
          RecordedStep previousSteps = new RecordedStep(path.previousSteps, path.action, finalState);

          /*
           * Using BFS here to find the shortest error paths first.
           * Be very careful with DFS while specifying a max depth, as
           * it hard cut-off entire state spaces for exploration when
           * a cut-off was similar to a starting state.
           *
           * In fact, DFS is probably never a good idea as it won't
           * produce the shortest (easiest) to analyze path.
           */

          for(int i = 0; i < actions.size(); i++) {
            NamedAction action = actions.get(i);

            unexploredPaths.add(new Path(previousSteps, action, path.depth + 1));
          }
        }
      }
      catch(TestAbortedException e) {
        // assumption failure, skip
      }
      catch(FailureAggregatingException fae) {  // user assert failure path

        /*
         * Construct a message with each individual assert's message and relevant
         * stacktrace + the base trace that they all share:
         */

        String msg = "Path " + pathsExplored + " failed: " + constructPath(path, finalState);
        StackTraceElement[] baseTrace = Thread.currentThread().getStackTrace();

        for(Throwable failure : fae.getFailures()) {
          msg += failure.getMessage() + "\n";

          StackTraceElement[] innerTrace = failure.getStackTrace();
          int commonSuffixLength = commonSuffixLength(innerTrace, baseTrace);

          for(int j = 0; j < innerTrace.length - commonSuffixLength; j++) {
            if(!isNoise(innerTrace[j])) {
              msg += "\tat " + innerTrace[j] + "\n";
            }
          }
        }

        msg += "While running:";

        throw new AssertionFailedError(msg);
      }
      catch(Throwable e) {  // user action failure path (shouldn't happen, actions shouldn't throw exceptions)
        fail("Path " + pathsExplored + ": " + constructPath(path, finalState == null ? e.getClass().getName() + ": " + e.getMessage() : finalState), e);
      }
    }

    System.out.print("\r" + " ".repeat(80) + "\r");
    System.out.println(ExploratoryTestRunner.class.getSimpleName() + ": " + testClass + " -- Explored " + pathsExplored + " paths, longest path: " + maxPathLength + (maxDepth == Integer.MAX_VALUE ? "" : " (max depth: " + maxDepth + ")"));
  }

  private static List<Object> createPruningKey(int historyDepth, Path path, Object finalState) {
    List<Object> pruningKey = new ArrayList<>(historyDepth);

    pruningKey.add(finalState);

    RecordedStep step = path.previousSteps;

    while(step != null && pruningKey.size() < historyDepth) {
      pruningKey.add(step.resultingState());

      step = step.previous;
    }

    return pruningKey;
  }

  private static String constructPath(Path path, Object finalState) {
    StringBuilder builder = new StringBuilder();

    for(RecordedStep step : path.steps()) {
      builder.append("\n - " + step.action.description + " -> " + step.resultingState);
    }

    builder.append("\n - " + path.action.description + " -> " + finalState + "\n");

    return builder.toString();
  }

  private static Object executePath(Explorable instance, Path path, ThrowingConsumer<Object, FailureAggregatingException> asserter) throws Throwable {
    if(path.previousSteps != null) {
      path.previousSteps.apply(instance);

      assertThat(path.previousSteps.resultingState).isEqualTo(instance.snapshot());

      asserter.accept(instance);
    }

    try {
      return runAction(instance, path.action);
    }
    catch(InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private static Object runAction(Explorable instance, NamedAction action) throws Exception {
    Runnable runnable = action.executor().apply(instance);

    Object stateBeforeEffect = instance.snapshot();

    runnable.run();

    Object stateAfterEffect = instance.snapshot();

    if(!stateBeforeEffect.equals(stateAfterEffect)) {
      throw new IllegalStateException("Action " + action.description + " violates strict seperation of expectation modification and effect execution");
    }

    return stateAfterEffect;
  }

  private static int commonSuffixLength(Object[] a, Object[] b) {
    int i = 0;
    int max = Math.min(a.length, b.length);

    while(i < max && a[a.length - 1 - i].equals(b[b.length - 1 - i])) {
      i++;
    }

    return i;
  }

  private static boolean isNoise(StackTraceElement e) {
    String c = e.getClassName();

    return c.startsWith("java.")
      || c.startsWith("jdk.")
      || c.startsWith("org.junit.")
      || c.startsWith("org.opentest4j.")
      || c.contains("org.int4.common.test.");
  }

  private static ExtensionContext createExtensionContext(Method testMethod) {
    return (ExtensionContext)Proxy.newProxyInstance(
      ExploratoryTestRunner.class.getClassLoader(),
      new Class<?>[] {ExtensionContext.class},
      (proxy, method, args) -> {
        return switch(method.getName()) {
          case "getElement" -> Optional.of(testMethod);
          case "getTestMethod" -> Optional.of(testMethod);
          case "getTestClass" -> Optional.of(testMethod.getDeclaringClass());
          case "getRequiredElement" -> testMethod;
          case "getRequiredTestMethod" -> testMethod;
          case "getRequiredTestClass" -> testMethod.getDeclaringClass();
          case "getDisplayName", "getUniqueId" -> testMethod.getName();
          case "getTags" -> Collections.emptySet();
          case "getConfigurationParameter" -> Optional.empty();
          case "getTestInstance" -> Optional.empty();
          case "getTestInstances" -> Optional.empty();
          case "getExecutableInvoker" -> createExecutableInvoker();
          default -> throw new UnsupportedOperationException("Method not implemented in proxied ExtensionContext: " + method.getName());
        };
      }
    );
  }

  private static ExecutableInvoker createExecutableInvoker() {
    return (ExecutableInvoker)Proxy.newProxyInstance(
      ExploratoryTestRunner.class.getClassLoader(),
      new Class<?>[] {ExecutableInvoker.class},
      (proxy, method, args) -> {
        if(method.getName().equals("invoke")) {
          Method targetMethod = (Method)args[0];
          Object targetInstance = args[1];

          return ReflectionSupport.invokeMethod(targetMethod, targetInstance);
        }

        throw new UnsupportedOperationException("Method not implemented in proxied ExecutableInvoker: " + method.getName());
      }
    );
  }

  private ExploratoryTestRunner() {}
}
