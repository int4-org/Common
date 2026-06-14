package examples;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;

import org.int4.common.test.explorer.Action;
import org.int4.common.test.explorer.Assertion;
import org.int4.common.test.explorer.Explorable;
import org.int4.common.test.explorer.ExploratoryTestRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedHashSetAsDeduplicatingQueueExploratoryTest {

  @Test
  void exploreQueueSemantics() {
    ExploratoryTestRunner.explore(QueueExplorable.class, QueueExplorable::new);
  }

  public static class QueueExplorable implements Explorable {

    private final Queue<String> queue = new MyQueue<>();
    private final List<String> expectedQueue = new ArrayList<>();

    public record State(List<String> queue) {}

    @Override
    public Object snapshot() {
      return new State(List.copyOf(expectedQueue));
    }

    @Assertion
    public void assertQueueContents() {
      assertThat(List.copyOf(queue))
        .describedAs("Queue contents")
        .isEqualTo(expectedQueue);
    }

    @Assertion
    public void assertPeekedElement() {
      assertThat(queue.peek())
        .describedAs("Peeked element")
        .isEqualTo(expectedQueue.isEmpty() ? null : expectedQueue.getFirst());
    }

    @Action
    @ValueSource(strings = {"A", "B", "C", "D"})
    public Runnable enqueue(String value) {
      if(!expectedQueue.contains(value)) {
        expectedQueue.add(value);
      }

      return () -> queue.offer(value);
    }

    @Action
    public Runnable dequeue() {
      if(!expectedQueue.isEmpty()) {
        expectedQueue.removeFirst();
      }

      return () -> queue.poll();
    }

    @Action
    @ValueSource(strings = {"A", "B", "C", "D"})
    public Runnable remove(String value) {
      expectedQueue.remove(value);

      return () -> queue.remove(value);
    }

    @Action
    public Runnable clear() {
      expectedQueue.clear();

      return () -> queue.clear();
    }
  }

  static class MyQueue<T> extends AbstractQueue<T> {
    private final LinkedHashSet<T> elements = new LinkedHashSet<>();

    @Override
    public boolean offer(T e) {
      return elements.add(e);
    }

    @Override
    public T poll() {
      if(elements.isEmpty()) {
        return null;
      }

      T poll = elements.getLast();  // subtle bug here!

      elements.remove(poll);

      return poll;
    }

    @Override
    public T peek() {
      return elements.isEmpty() ? null : elements.getFirst();
    }

    @Override
    public Iterator<T> iterator() {
      return elements.iterator();
    }

    @Override
    public int size() {
      return elements.size();
    }
  }
}