package org.int4.common.collection;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

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
  private static final SequencedMap<?, ?> EMPTY_SEQUENCED_MAP = Immutable.of(new LinkedHashMap<>());

  /**
   * Creates an empty immutable sequenced map.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @return an immutable sequenced map, never {@code null}
   */
  @SuppressWarnings("unchecked")
  public static <K, V> SequencedMap<K, V> sequencedMap() {
    return (SequencedMap<K, V>)EMPTY_SEQUENCED_MAP;
  }

  /**
   * Creates an immutable sequenced map with one entry.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param k the key
   * @param v the value
   * @return an immutable sequenced map, never {@code null}
   */
  public static <K, V> SequencedMap<K, V> sequencedMap(K k, V v) {
    SequencedMap<K, V> map = new LinkedHashMap<>();

    map.put(k, v);

    return of(map);
  }

  /**
   * Creates an immutable sequenced map with two entries.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param k1 the first key
   * @param v1 the first value
   * @param k2 the second key
   * @param v2 the second value
   * @return an immutable sequenced map, never {@code null}
   */
  public static <K, V> SequencedMap<K, V> sequencedMap(K k1, V v1, K k2, V v2) {
    SequencedMap<K, V> map = new LinkedHashMap<>();

    map.put(k1, v1);
    map.put(k2, v2);

    return of(map);
  }

  /**
   * Creates an immutable sequenced map with three entries.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param k1 the first key
   * @param v1 the first value
   * @param k2 the second key
   * @param v2 the second value
   * @param k3 the third key
   * @param v3 the third value
   * @return an immutable sequenced map, never {@code null}
   */
  public static <K, V> SequencedMap<K, V> sequencedMap(K k1, V v1, K k2, V v2, K k3, V v3) {
    SequencedMap<K, V> map = new LinkedHashMap<>();

    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);

    return of(map);
  }

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

  /**
   * Returns an immutable representation of the given map.
   * <p>
   * If the provided map is already a known immutable implementation,
   * it is returned as-is without copying.
   * <p>
   * Otherwise, a defensive copy of the input is created and wrapped in an
   * immutable view to prevent external mutation.
   * <p>
   * The returned map does not permit structural modification operations
   * and will throw {@link UnsupportedOperationException} if such methods are
   * invoked.
   * <p>
   * Null keys and values are preserved.
   *
   * @param <K> key type
   * @param <V> value type
   * @param map the source map, must not be {@code null}
   * @return an immutable map backed by a defensive copy, or the original map
   *   if it is already immutable, never {@code null}
   * @throws NullPointerException if {@code map} is {@code null}
   */
  public static <K, V> SequencedMap<K, V> of(NavigableMap<K, V> map) {
    Objects.requireNonNull(map, "map");

    if(isKnownImmutable(map)) {
      return map;
    }

    return new ImmutableNavigableMap<>(new TreeMap<>(map));
  }

  /**
   * Returns an immutable representation of the given map.
   * <p>
   * If the provided map is already a known immutable implementation,
   * it is returned as-is without copying.
   * <p>
   * Otherwise, a defensive copy of the input is created and wrapped in an
   * immutable view to prevent external mutation.
   * <p>
   * The returned map does not permit structural modification operations
   * and will throw {@link UnsupportedOperationException} if such methods are
   * invoked.
   * <p>
   * Null keys and values are preserved.
   *
   * @param <K> key type
   * @param <V> value type
   * @param map the source map, must not be {@code null}
   * @return an immutable map backed by a defensive copy, or the original map
   *   if it is already immutable, never {@code null}
   * @throws NullPointerException if {@code map} is {@code null}
   */
  public static <K, V> SortedMap<K, V> of(SortedMap<K, V> map) {
    Objects.requireNonNull(map, "map");

    if(isKnownImmutable(map)) {
      return map;
    }

    return new ImmutableSortedMap<>(new TreeMap<>(map));
  }

  /**
   * Returns an immutable representation of the given map.
   * <p>
   * If the provided map is already a known immutable implementation,
   * it is returned as-is without copying.
   * <p>
   * Otherwise, a defensive copy of the input is created and wrapped in an
   * immutable view to prevent external mutation.
   * <p>
   * The returned map does not permit structural modification operations
   * and will throw {@link UnsupportedOperationException} if such methods are
   * invoked.
   * <p>
   * Null keys and values are preserved.
   *
   * @param <K> key type
   * @param <V> value type
   * @param map the source map, must not be {@code null}
   * @return an immutable map backed by a defensive copy, or the original map
   *   if it is already immutable, never {@code null}
   * @throws NullPointerException if {@code map} is {@code null}
   */
  public static <K, V> SequencedMap<K, V> of(SequencedMap<K, V> map) {
    Objects.requireNonNull(map, "map");

    if(isKnownImmutable(map)) {
      return map;
    }

    return new ImmutableSequencedMap<>(new LinkedHashMap<>(map));
  }

  /**
   * Returns an immutable representation of the given map.
   * <p>
   * If the provided map is already a known immutable implementation,
   * it is returned as-is without copying.
   * <p>
   * Otherwise, a defensive copy of the input is created and wrapped in an
   * immutable view to prevent external mutation.
   * <p>
   * The returned map does not permit structural modification operations
   * and will throw {@link UnsupportedOperationException} if such methods are
   * invoked.
   * <p>
   * Null keys and values are preserved.
   *
   * @param <K> key type
   * @param <V> value type
   * @param map the source map, must not be {@code null}
   * @return an immutable map backed by a defensive copy, or the original map
   *   if it is already immutable, never {@code null}
   * @throws NullPointerException if {@code map} is {@code null}
   */
  public static <K, V> Map<K, V> of(Map<K, V> map) {
    Objects.requireNonNull(map, "map");

    if(isKnownImmutable(map)) {
      return map;
    }

    return new ImmutableMap<>(new HashMap<>(map));
  }

  /**
   * Returns an immutable representation of the given set.
   * <p>
   * If the provided set is already a known immutable implementation,
   * it is returned as-is without copying.
   * <p>
   * Otherwise, a defensive copy of the input is created and wrapped in an
   * immutable view to prevent external mutation.
   * <p>
   * The returned set does not permit structural modification operations
   * and will throw {@link UnsupportedOperationException} if such methods are
   * invoked.
   * <p>
   * Null elements are preserved.
   *
   * @param <E> element type
   * @param set the source set, must not be {@code null}
   * @return an immutable set backed by a defensive copy, or the original set
   *   if it is already immutable, never {@code null}
   * @throws NullPointerException if {@code map} is {@code null}
   */
  public static <E> NavigableSet<E> of(NavigableSet<E> set) {
    Objects.requireNonNull(set, "set");

    if(isKnownImmutable(set)) {
      return set;
    }

    return new ImmutableNavigableSet<>(new TreeSet<>(set));
  }

  /**
   * Returns an immutable representation of the given set.
   * <p>
   * If the provided set is already a known immutable implementation,
   * it is returned as-is without copying.
   * <p>
   * Otherwise, a defensive copy of the input is created and wrapped in an
   * immutable view to prevent external mutation.
   * <p>
   * The returned set does not permit structural modification operations
   * and will throw {@link UnsupportedOperationException} if such methods are
   * invoked.
   * <p>
   * Null elements are preserved.
   *
   * @param <E> element type
   * @param set the source set, must not be {@code null}
   * @return an immutable set backed by a defensive copy, or the original set
   *   if it is already immutable, never {@code null}
   * @throws NullPointerException if {@code map} is {@code null}
   */
  public static <E> SortedSet<E> of(SortedSet<E> set) {
    Objects.requireNonNull(set, "set");

    if(isKnownImmutable(set)) {
      return set;
    }

    return new ImmutableSortedSet<>(new TreeSet<>(set));
  }

  /**
   * Returns an immutable representation of the given set.
   * <p>
   * If the provided set is already a known immutable implementation,
   * it is returned as-is without copying.
   * <p>
   * Otherwise, a defensive copy of the input is created and wrapped in an
   * immutable view to prevent external mutation.
   * <p>
   * The returned set does not permit structural modification operations
   * and will throw {@link UnsupportedOperationException} if such methods are
   * invoked.
   * <p>
   * Null elements are preserved.
   *
   * @param <E> element type
   * @param set the source set, must not be {@code null}
   * @return an immutable set backed by a defensive copy, or the original set
   *   if it is already immutable, never {@code null}
   * @throws NullPointerException if {@code map} is {@code null}
   */
  public static <E> SequencedSet<E> of(SequencedSet<E> set) {
    Objects.requireNonNull(set, "set");

    if(isKnownImmutable(set)) {
      return set;
    }

    return new ImmutableSequencedSet<>(new LinkedHashSet<>(set));
  }

  /**
   * Returns an immutable representation of the given set.
   * <p>
   * If the provided set is already a known immutable implementation,
   * it is returned as-is without copying.
   * <p>
   * Otherwise, a defensive copy of the input is created and wrapped in an
   * immutable view to prevent external mutation.
   * <p>
   * The returned set does not permit structural modification operations
   * and will throw {@link UnsupportedOperationException} if such methods are
   * invoked.
   * <p>
   * Null elements are preserved.
   *
   * @param <E> element type
   * @param set the source set, must not be {@code null}
   * @return an immutable set backed by a defensive copy, or the original set
   *   if it is already immutable, never {@code null}
   * @throws NullPointerException if {@code map} is {@code null}
   */
  public static <E> Set<E> of(Set<E> set) {
    Objects.requireNonNull(set, "set");

    if(isKnownImmutable(set)) {
      return set;
    }

    return new ImmutableSet<>(new HashSet<>(set));
  }

  private static boolean isKnownImmutable(List<?> list) {
    return list instanceof RandomAccessImmutableList<?>
      || list instanceof ImmutableList<?>
      || list.getClass().getName().startsWith("java.util.ImmutableCollections$");
  }

  private static boolean isKnownImmutable(Map<?, ?> map) {
    return map instanceof AbstractImmutableMap<?, ?, ?>
      || map.getClass().getName().startsWith("java.util.ImmutableCollections$");
  }

  private static boolean isKnownImmutable(Set<?> set) {
    return set instanceof AbstractImmutableSet<?, ?>
      || set.getClass().getName().startsWith("java.util.ImmutableCollections$");
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

  private static final class ImmutableNavigableMap<K, V> extends AbstractImmutableSortedMap<K, V, NavigableMap<K, V>> implements NavigableMap<K, V> {

    ImmutableNavigableMap(NavigableMap<K, V> map) {
      super(map);
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
      Entry<K, V> entry = delegate.lowerEntry(key);

      return entry == null ? null : new AbstractMap.SimpleImmutableEntry<>(entry);
    }

    @Override
    public K lowerKey(K key) {
      return delegate.lowerKey(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
      Entry<K, V> entry = delegate.floorEntry(key);

      return entry == null ? null : new AbstractMap.SimpleImmutableEntry<>(entry);
    }

    @Override
    public K floorKey(K key) {
      return delegate.floorKey(key);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
      Entry<K, V> entry = delegate.ceilingEntry(key);

      return entry == null ? null : new AbstractMap.SimpleImmutableEntry<>(entry);
    }

    @Override
    public K ceilingKey(K key) {
      return delegate.ceilingKey(key);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
      Entry<K, V> entry = delegate.higherEntry(key);

      return entry == null ? null : new AbstractMap.SimpleImmutableEntry<>(entry);
    }

    @Override
    public K higherKey(K key) {
      return delegate.higherKey(key);
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
      return new ImmutableNavigableMap<>(delegate.descendingMap());
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
      return new ImmutableNavigableSet<>(delegate.navigableKeySet());
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
      return new ImmutableNavigableSet<>(delegate.descendingKeySet());
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return new ImmutableNavigableMap<>(delegate.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return new ImmutableNavigableMap<>(delegate.headMap(toKey, inclusive));
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return new ImmutableNavigableMap<>(delegate.tailMap(fromKey, inclusive));
    }

    @Override
    protected NavigableMap<K, V> internalReversed() {
      return new ImmutableNavigableMap<>(delegate.reversed());
    }
  }

  private static final class ImmutableSortedMap<K, V> extends AbstractImmutableSortedMap<K, V, SortedMap<K, V>> {

    ImmutableSortedMap(SortedMap<K, V> delegate) {
      super(delegate);
    }

    @Override
    protected SortedMap<K, V> internalReversed() {
      return new ImmutableSortedMap<>(delegate.reversed());
    }
  }

  private static abstract class AbstractImmutableSortedMap<K, V, D extends SortedMap<K, V>> extends AbstractImmutableSequencedMap<K, V, D> implements SortedMap<K, V> {

    AbstractImmutableSortedMap(D delegate) {
      super(delegate);
    }

    @Override
    public final Comparator<? super K> comparator() {
      return delegate.comparator();
    }

    @Override
    public final SortedMap<K, V> subMap(K fromKey, K toKey) {
      return new ImmutableSortedMap<>(delegate.subMap(fromKey, toKey));
    }

    @Override
    public final SortedMap<K, V> headMap(K toKey) {
      return new ImmutableSortedMap<>(delegate.headMap(toKey));
    }

    @Override
    public final SortedMap<K, V> tailMap(K fromKey) {
      return new ImmutableSortedMap<>(delegate.tailMap(fromKey));
    }

    @Override
    public final K firstKey() {
      return delegate.firstKey();
    }

    @Override
    public final K lastKey() {
      return delegate.lastKey();
    }
  }

  private static final class ImmutableSequencedMap<K, V> extends AbstractImmutableSequencedMap<K, V, SequencedMap<K, V>> {

    ImmutableSequencedMap(SequencedMap<K, V> delegate) {
      super(delegate);
    }

    @Override
    protected SequencedMap<K, V> internalReversed() {
      return new ImmutableSequencedMap<>(delegate.reversed());
    }
  }

  private abstract static class AbstractImmutableSequencedMap<K, V, D extends SequencedMap<K, V>> extends AbstractImmutableMap<K, V, D> implements SequencedMap<K, V> {

    AbstractImmutableSequencedMap(D delegate) {
      super(delegate);
    }

    @Override
    public final Entry<K, V> firstEntry() {
      Entry<K, V> entry = delegate.firstEntry();

      return entry == null ? null : new AbstractMap.SimpleImmutableEntry<>(entry);
    }

    @Override
    public final Entry<K, V> lastEntry() {
      Entry<K, V> entry = delegate.lastEntry();

      return entry == null ? null : new AbstractMap.SimpleImmutableEntry<>(entry);
    }

    @Override
    public final Entry<K, V> pollFirstEntry() {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final Entry<K, V> pollLastEntry() {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V putFirst(K k, V v) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V putLast(K k, V v) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final SequencedSet<K> sequencedKeySet() {
      return new ImmutableSequencedSet<>(delegate.sequencedKeySet());
    }

    @Override
    public final SequencedCollection<V> sequencedValues() {
      return Collections.unmodifiableSequencedCollection(delegate.sequencedValues());
    }

    @Override
    public final SequencedSet<Entry<K, V>> sequencedEntrySet() {
      return new ImmutableSequencedSet<>(Collections.unmodifiableSequencedSet(delegate.sequencedEntrySet()));  // entries must be immutable as well
    }

    @Override
    public final D reversed() {
      return internalReversed();
    }

    protected abstract D internalReversed();
  }

  private static final class ImmutableMap<K, V> extends AbstractImmutableMap<K, V, Map<K, V>> {

    ImmutableMap(Map<K, V> delegate) {
      super(delegate);
    }
  }

  private abstract static class AbstractImmutableMap<K, V, D extends Map<K, V>> implements Map<K, V> {
    protected final D delegate;

    AbstractImmutableMap(D delegate) {
      this.delegate = delegate;
    }

    @Override
    public final boolean equals(Object o) {
      return delegate.equals(o);
    }

    @Override
    public final int hashCode() {
      return delegate.hashCode();
    }

    @Override
    public final String toString() {
      return delegate.toString();
    }

    @Override
    public final int size() {
      return delegate.size();
    }

    @Override
    public final boolean isEmpty() {
      return delegate.isEmpty();
    }

    @Override
    public final boolean containsKey(Object key) {
      return delegate.containsKey(key);
    }

    @Override
    public final boolean containsValue(Object value) {
      return delegate.containsValue(value);
    }

    @Override
    public final V get(Object key) {
      return delegate.get(key);
    }

    @Override
    public final V getOrDefault(Object key, V defaultValue) {
      return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public final void forEach(BiConsumer<? super K, ? super V> action) {
      delegate.forEach(action);
    }

    @Override
    public final void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V putIfAbsent(K key, V value) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final boolean remove(Object key, Object value) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final boolean replace(K key, V oldValue, V newValue) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V replace(K key, V value) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V put(K key, V value) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final V remove(Object key) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final void putAll(Map<? extends K, ? extends V> m) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final void clear() {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final Set<K> keySet() {
      return new ImmutableSet<>(delegate.keySet());
    }

    @Override
    public final Collection<V> values() {
      return Collections.unmodifiableCollection(delegate.values());
    }

    @Override
    public final Set<Entry<K, V>> entrySet() {
      return new ImmutableSet<>(Collections.unmodifiableSet(delegate.entrySet()));  // entries must be immutable as well
    }
  }

  private static final class ImmutableNavigableSet<E> extends AbstractImmutableNavigableSet<E, NavigableSet<E>> {

    ImmutableNavigableSet(NavigableSet<E> delegate) {
      super(delegate);
    }

    @Override
    public NavigableSet<E> internalReversed() {
      return new ImmutableNavigableSet<>(delegate.reversed());
    }
  }

  private static abstract class AbstractImmutableNavigableSet<E, D extends NavigableSet<E>> extends AbstractImmutableSortedSet<E, D> implements NavigableSet<E> {

    AbstractImmutableNavigableSet(D delegate) {
      super(delegate);
    }

    @Override
    public final E lower(E e) {
      return delegate.lower(e);
    }

    @Override
    public final E floor(E e) {
      return delegate.floor(e);
    }

    @Override
    public final E ceiling(E e) {
      return delegate.ceiling(e);
    }

    @Override
    public final E higher(E e) {
      return delegate.higher(e);
    }

    @Override
    public final E pollFirst() {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final E pollLast() {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final NavigableSet<E> descendingSet() {
      return new ImmutableNavigableSet<>(delegate.descendingSet());
    }

    @Override
    public final Iterator<E> descendingIterator() {
      Iterator<E> iterator = delegate.descendingIterator();

      return new Iterator<>() {
        @Override
        public boolean hasNext() {
          return iterator.hasNext();
        }

        @Override
        public E next() {
          return iterator.next();
        }
      };
    }

    @Override
    public final NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
      return new ImmutableNavigableSet<>(delegate.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override
    public final NavigableSet<E> headSet(E toElement, boolean inclusive) {
      return new ImmutableNavigableSet<>(delegate.headSet(toElement, inclusive));
    }

    @Override
    public final NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      return new ImmutableNavigableSet<>(delegate.tailSet(fromElement, inclusive));
    }
  }

  private static final class ImmutableSortedSet<E> extends AbstractImmutableSortedSet<E, SortedSet<E>> {

    ImmutableSortedSet(SortedSet<E> delegate) {
      super(delegate);
    }

    @Override
    public SortedSet<E> internalReversed() {
      return new ImmutableSortedSet<>(delegate.reversed());
    }
  }

  private static abstract class AbstractImmutableSortedSet<E, D extends SortedSet<E>> extends AbstractImmutableSequencedSet<E, D> implements SortedSet<E> {

    AbstractImmutableSortedSet(D delegate) {
      super(delegate);
    }

    @Override
    public final Comparator<? super E> comparator() {
      return delegate.comparator();
    }

    @Override
    public final SortedSet<E> subSet(E fromElement, E toElement) {
      return new ImmutableSortedSet<>(delegate.subSet(fromElement, toElement));
    }

    @Override
    public final SortedSet<E> headSet(E toElement) {
      return new ImmutableSortedSet<>(delegate.headSet(toElement));
    }

    @Override
    public final SortedSet<E> tailSet(E fromElement) {
      return new ImmutableSortedSet<>(delegate.tailSet(fromElement));
    }

    @Override
    public final E first() {
      return delegate.first();
    }

    @Override
    public final E last() {
      return delegate.last();
    }
  }

  private static final class ImmutableSequencedSet<E> extends AbstractImmutableSequencedSet<E, SequencedSet<E>> {

    ImmutableSequencedSet(SequencedSet<E> delegate) {
      super(delegate);
    }

    @Override
    public SequencedSet<E> internalReversed() {
      return new ImmutableSequencedSet<>(delegate.reversed());
    }
  }

  private static abstract class AbstractImmutableSequencedSet<E, D extends SequencedSet<E>> extends AbstractImmutableSet<E, D> implements SequencedSet<E> {

    AbstractImmutableSequencedSet(D delegate) {
      super(delegate);
    }

    @Override
    public final D reversed() {
      return internalReversed();
    }

    protected abstract D internalReversed();
  }

  private static final class ImmutableSet<E> extends AbstractImmutableSet<E, Set<E>> {

    ImmutableSet(Set<E> delegate) {
      super(delegate);
    }
  }

  private abstract static class AbstractImmutableSet<E, D extends Set<E>> implements Set<E> {
    protected final D delegate;

    AbstractImmutableSet(D delegate) {
      this.delegate = delegate;
    }

    @Override
    public final boolean equals(Object o) {
      return delegate.equals(o);
    }

    @Override
    public final int hashCode() {
      return delegate.hashCode();
    }

    @Override
    public final String toString() {
      return delegate.toString();
    }

    @Override
    public final int size() {
      return delegate.size();
    }

    @Override
    public final boolean isEmpty() {
      return delegate.isEmpty();
    }

    @Override
    public final boolean contains(Object o) {
      return delegate.contains(o);
    }

    @Override
    public final boolean containsAll(Collection<?> c) {
      return delegate.containsAll(c);
    }

    @Override
    public final Iterator<E> iterator() {
      Iterator<E> iter = delegate.iterator();

      return new Iterator<>() {
        @Override
        public boolean hasNext() {
          return iter.hasNext();
        }

        @Override
        public E next() {
          return iter.next();
        }
      };
    }

    @Override
    public final Object[] toArray() {
      return delegate.toArray();
    }

    @Override
    public final <T> T[] toArray(T[] a) {
      return delegate.toArray(a);
    }

    @Override
    public final boolean add(E e) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final boolean remove(Object o) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final boolean addAll(Collection<? extends E> c) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException("immutable");
    }

    @Override
    public final void clear() {
      throw new UnsupportedOperationException("immutable");
    }
  }

  private Immutable() {
  }
}
