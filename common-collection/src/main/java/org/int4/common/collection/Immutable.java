package org.int4.common.collection;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Utility for creating immutable representations of common collection types.
 * <p>
 * This class provides factory methods that accept existing mutable or immutable
 * collections and return a logically immutable view of their contents.
 * <p>
 * Where possible, known immutable implementations are detected and returned
 * without copying. Otherwise, a defensive copy is created to ensure isolation
 * from subsequent external modifications.
 * <p>
 * The immutability guarantee applies to the returned collection interface:
 * structural modification operations are not supported and will result in
 * {@link UnsupportedOperationException}.
 * <p>
 * Null elements are preserved unless otherwise specified by a particular method.
 */
public abstract class Immutable {

  /**
   * Returns an immutable representation of the given list.
   * <p>
   * If the provided list is already a known immutable implementation,
   * it is returned as-is without copying.
   * <p>
   * Otherwise, a defensive copy of the input is created and wrapped in an
   * immutable view to prevent external mutation.
   * <p>
   * The returned list does not permit structural modification operations
   * and will throw {@link UnsupportedOperationException} if such methods are
   * invoked.
   * <p>
   * Null elements are preserved.
   *
   * @param <T> element type
   * @param list the source list, must not be {@code null}
   * @return an immutable list backed by a defensive copy, or the original list
   *   if it is already immutable, never {@code null}
   * @throws NullPointerException if {@code list} is {@code null}
   */
  public static <T> List<T> of(List<T> list) {
    Objects.requireNonNull(list, "list");

    if(isKnownImmutable(list)) {
      return list;
    }

    return list instanceof RandomAccess
      ? new RandomAccessImmutableList<>(Collections.unmodifiableList(new ArrayList<>(list)))
      : new ImmutableList<>(Collections.unmodifiableList(new ArrayList<>(list)));
  }

  private static boolean isKnownImmutable(List<?> list) {
    return list instanceof RandomAccessImmutableList<?>
      || list instanceof ImmutableList<?>
      || list.getClass().getName().startsWith("java.util.ImmutableCollections$");
  }

  private static final class RandomAccessImmutableList<T> extends AbstractList<T> implements RandomAccess {
    private final List<T> delegate;

    private RandomAccessImmutableList(List<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public T get(int index) {
      return delegate.get(index);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public boolean contains(Object o) {
      return delegate.contains(o);
    }

    @Override
    public int indexOf(Object o) {
      return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
      return delegate.lastIndexOf(o);
    }

    @Override
    public Iterator<T> iterator() {
      return delegate.iterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
      return delegate.listIterator(index);
    }
  }

  private static final class ImmutableList<T> extends AbstractList<T> {
    private final List<T> delegate;

    private ImmutableList(List<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public T get(int index) {
      return delegate.get(index);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public boolean contains(Object o) {
      return delegate.contains(o);
    }

    @Override
    public int indexOf(Object o) {
      return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
      return delegate.lastIndexOf(o);
    }

    @Override
    public Iterator<T> iterator() {
      return delegate.iterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
      return delegate.listIterator(index);
    }
  }

  private Immutable() {
  }
}
