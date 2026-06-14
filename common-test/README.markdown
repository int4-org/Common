# Exploratory Test Runner

The `ExploratoryTestRunner` is a tool for performing **Exhaustive State-Space Exploration** on Java models. It systematically explores every possible sequence of actions up to a specified depth to ensure your implementation behaves correctly in all scenarios.

For example, this framework is particularly effective for testing:
- **Complex State Machines**: Ensuring no sequence of events leads to an illegal state.
- **UI Controls**: Testing interactions between models, views, and controllers (e.g., JavaFX or Swing components).
- **Concurrent Primitives**: Verifying correctness under different interleavings (if made deterministic).
- **Custom Collections**: Finding subtle edge cases in data structure logic.

## Why Exploratory Testing?

Traditional testing methods often miss subtle edge cases:
- **Unit Tests**: Only test specific paths manually defined by the developer.
- **Property-Based Testing (PBT)**: Uses random sequences of actions. While powerful, it is not exhaustive and might miss rare "needle in a haystack" bugs.

`ExploratoryTestRunner` fills this gap with:
- **Exhaustiveness**: It explores *all* possible combinations of actions. If a bug exists within the search depth, it *will* be found.
- **Determinism**: Given the same initial state and actions, it always explores the space in the same order and reports consistent results.
- **Shortest Path Guarantee**: It uses Breadth-First Search (BFS). The first failure found is guaranteed to be the shortest possible sequence to reproduce the bug.
- **State Deduplication**: By using logical snapshots, it avoids re-exploring the same state reached via different paths, allowing it to cover millions of scenarios efficiently.

## Core Concepts

To test a component, you implement the `Explorable` interface. The runner interacts with your model via three main components:

### 1. State Snapshot (`snapshot()`)
A method that returns an immutable representation of the *logical* state. The runner uses this to prune the search tree. 
> **Tip**: Use a Java **`record`** for your snapshot. Records provide built-in, value-based `equals()` and `hashCode()` implementations, which are critical for correct pruning.

### 2. The Dual-State Pattern (`@Action`)
Methods annotated with `@Action` define what can happen in your system. They follow a strict two-phase pattern:
1.  **Phase 1 (Update Expectation)**: The method body updates the internal "expected" state of your model.
2.  **Phase 2 (Return Effect)**: The method returns a `Runnable` that performs the actual side-effect on the System Under Test (SUT).

**Strict Separation**: The runner enforces that the `Runnable` **must not** change the logical state further. It takes a snapshot before and after the `Runnable` runs; if they differ, it throws an `IllegalStateException`. This ensures your model accurately tracks every change in the SUT.

### 3. Assertions (`@Assertion`)
Methods that verify the SUT matches the expected state. These are run after every action. If any assertion throws an exception, the runner reports a failure.

## Advanced Usage

### Conditional Actions (Pruning)
You can use JUnit's `Assumptions.assumeTrue()` inside an `@Action` method to skip it if it doesn't make sense in the current state (e.g., "cannot disconnect if not connected"). The runner will simply abort that path and continue with other actions.

### Managing the State Space
- **Full Exploration**: If the state space is finite (like a queue with a limited set of possible values), call `explore()` without a depth limit. The runner will stop once all reachable states are visited.
- **Max Depth (`maxDepth`)**: For infinite or very large spaces, use `maxDepth` to bound the search.
- **History Depth (`historyDepth`)**: Use this to solve the "Hidden State" problem. If your `snapshot()` is incomplete (missing internal state like cache headers), setting a `historyDepth > 1` treats states as distinct if the path taken to reach them is different.

## User Guide: Testing a Queue

Let's look at how to test a custom `Queue` implementation. You can find the full code in the [`examples`](examples/) folder.

### 1. Implement `Explorable`
Create a class that maintains both your SUT (`MyQueue`) and a reference implementation (like `ArrayList` or `List` of expected values).

```java
public class QueueExplorable implements Explorable {
    private final Queue<String> queue = new MyQueue<>();
    private final List<String> expectedQueue = new ArrayList<>();

    public record State(List<String> queue) {}

    @Override
    public Object snapshot() {
        // Return a representation of the logical state
        return new State(List.copyOf(expectedQueue));
    }
}
```

### 2. Define Actions and Assertions
Use `@ValueSource` to branch the state space with different inputs.

```java
@Action
@ValueSource(strings = {"A", "B", "C"})
public Runnable enqueue(String value) {
    // 1. Update the 'expected' model immediately
    if (!expectedQueue.contains(value)) {
        expectedQueue.add(value);
    }

    // 2. Return the action to perform on the SUT
    return () -> queue.offer(value);
}

@Action
public Runnable dequeue() {
    if (!expectedQueue.isEmpty()) {
        expectedQueue.removeFirst();
    }
    return () -> queue.poll();
}

@Assertion
public void assertQueueContents() {
    assertThat(List.copyOf(queue))
        .describedAs("Queue contents")
        .isEqualTo(expectedQueue);
}
```

### 3. Run the Test

Use the `ExploratoryTestRunner` in a standard JUnit test.

```java
@Test
void exploreQueue() {
    // For finite state spaces, no depth limit is needed
    ExploratoryTestRunner.explore(QueueExplorable.class, QueueExplorable::new);
}
```

## Interpreting Errors

When a failure occurs, the runner provides a path report and the specific failing assertion:

```text
org.opentest4j.AssertionFailedError: Path 61 failed: 
 - enqueue(A) -> State[queue=[A]]
 - enqueue(B) -> State[queue=[A, B]]
 - dequeue -> State[queue=[B]]
[Queue contents] 
expected: ["B"]
 but was: ["A"]
  at org.int4.common.test/examples.QueueExplorable.assertQueueContents(QueueExplorable.java:42)
```

### How to read this report:

1.  **The Path**: The report shows the exact sequence of actions that led to the failure.
    - `enqueue(A)` was called.
    - `enqueue(B)` was called.
    - `dequeue` was called.
2.  **The States**: Each line shows the expected state resulting from `snapshot()`.
3.  **The Failure**: The assertion failed after the `dequeue` action.
    - It expected the queue to contain `["B"]`.
    - It actually contained `["A"]`.
4.  **The Source**: The stack trace line `at ...QueueExplorable.assertQueueContents(...)` points directly to the `@Assertion` method that failed. This allows you to quickly identify *which* property was violated.
5.  **The Root Cause**: This tells us that `dequeue` (the `poll()` method) likely removed the wrong element (B instead of A), which is exactly the bug in our example implementation.

## Important Note: Determinism
The `ExploratoryTestRunner` relies on the system being deterministic. If your SUT uses `Random`, `System.currentTimeMillis()`, or asynchronous callbacks that aren't waited for, the runner will produce flaky results or fail to deduplicate states correctly.
