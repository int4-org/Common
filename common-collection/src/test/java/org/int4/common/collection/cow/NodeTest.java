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

package org.int4.common.collection.cow;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeTest {
  List<String> reference = new ArrayList<>();
  Node.Owner owner = new Node.Owner();
  Node root;
  int size;
  int nodeSize;

  @ParameterizedTest
  @CsvSource({"8,8", "8,16", "16,8", "8,32", "8,64"})
  @Retention(RetentionPolicy.RUNTIME)
  private @interface StandardTest {
  }

  @StandardTest
  void testLeftUnbalancedInserts(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 0);

    while(size < 10) {
      add(0, "" + size);
    }

    assertAgainstReference();
  }

  @StandardTest
  void testRightUnbalancedInserts(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 0);

    while(size < 1000) {
      add(size, "" + size);
    }
  }

  @StandardTest
  void testBalancedEdgeInserts(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 0);

    while(size < 1000) {
      add(size, "" + size);
      add(0, "" + size);
    }

    assertAgainstReference();
  }

  @StandardTest
  void testBalancedInserts(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 0);

    while(size < 1000) {
      add(size, "" + size);
      add(size / 2, "" + size);
      add(0, "" + size);
    }

    assertAgainstReference();
  }

  @StandardTest
  void testBalancedInsertsAt4Locations(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 0);

    while(size < 1000) {
      add(size, "" + size);
      add(size / 3, "" + size);
      add((size / 3) * 2, "" + size);
      add(0, "" + size);
    }

    assertAgainstReference();
  }

  @StandardTest
  void testLeftUnbalancedRemovals(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 200);

    while(root.getUsedSlots() != 0) {
      remove(0);
    }

    assertAgainstReference();
  }

  @StandardTest
  void testRightUnbalancedRemovals(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 200);

    while(root.getUsedSlots() != 0) {
      remove(size - 1);
    }

    assertAgainstReference();
  }

  @StandardTest
  void testBalancedEdgeRemovals(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 200);

    while(root.getUsedSlots() != 0) {
      remove(size - 1);

      if(root.getUsedSlots() > 0) {
        remove(0);
      }
    }

    assertAgainstReference();
  }

  @StandardTest
  void testBalancedRemovals(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 200);

    while(root.getUsedSlots() != 0) {
      remove(size - 1);

      if(root.getUsedSlots() > 0) {
        remove(size / 2);
      }

      if(root.getUsedSlots() > 0) {
        remove(0);
      }
    }

    assertAgainstReference();
  }

  @StandardTest
  void testBalancedRemovals2(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 200);

    while(root.getUsedSlots() != 0) {
      remove(size / 2);

      if(root.getUsedSlots() > 0) {
        remove(0);
      }
    }

    assertAgainstReference();
  }

  @StandardTest
  void testBalancedRemovals3(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 200);

    while(root.getUsedSlots() != 0) {
      remove(size - 1);

      if(root.getUsedSlots() > 0) {
        remove(size / 2);
      }
    }

    assertAgainstReference();
  }

  @StandardTest
  void testBalancedRemovalsAt4Locations(int nodeSize, int leafSize) {
    constructTree(nodeSize, leafSize, 200);

    while(root.getUsedSlots() != 0) {
      remove(size - 1);

      if(root.getUsedSlots() > 0) {
        remove(size / 3);
      }

      if(root.getUsedSlots() > 0) {
        remove((size / 3) * 2);
      }

      if(root.getUsedSlots() > 0) {
        remove(0);
      }
    }

    assertAgainstReference();
  }

  @Test
  void rebalanceShouldWork() {
    Node root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C", "D"}),
      Node.leaf(owner, 8, new Object[] {"E", "F", "G", "H"}),
      Node.leaf(owner, 8, new Object[] {"I", "J", "K", "L"})
    );

    assertThat(root.getUsedSlots()).isEqualTo(3);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("E", "F", "G", "H"));
    assertThat(toList(root.getNode(2))).isEqualTo(List.of("I", "J", "K", "L"));

    root.rebalance(0, 3);

    assertThat(root.getUsedSlots()).isEqualTo(3);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("E", "F", "G", "H"));
    assertThat(toList(root.getNode(2))).isEqualTo(List.of("I", "J", "K", "L"));

    root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C", "D", "E"}),
      Node.leaf(owner, 8, new Object[] {"F", "G", "H", "I"}),
      Node.leaf(owner, 8, new Object[] {"J", "K", "L", "M"})
    );

    assertThat(root.getUsedSlots()).isEqualTo(3);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D", "E"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("F", "G", "H", "I"));
    assertThat(toList(root.getNode(2))).isEqualTo(List.of("J", "K", "L", "M"));

    root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C", "D"}),
      Node.leaf(owner, 8, new Object[] {"E", "F", "G", "H"}),
      Node.leaf(owner, 8, new Object[] {"I", "J", "K", "L", "M"})
    );

    root.rebalance(0, 3);

    assertThat(root.getUsedSlots()).isEqualTo(3);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D", "E"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("F", "G", "H", "I"));
    assertThat(toList(root.getNode(2))).isEqualTo(List.of("J", "K", "L", "M"));

    root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C", "D", "E", "F", "G", "H"}),
      Node.leaf(owner, 8, new Object[] {}),
      Node.leaf(owner, 8, new Object[] {"I", "J", "K", "L", "M"})
    );

    root.rebalance(0, 3);

    assertThat(root.getUsedSlots()).isEqualTo(3);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D", "E"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("F", "G", "H", "I"));
    assertThat(toList(root.getNode(2))).isEqualTo(List.of("J", "K", "L", "M"));

    root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C", "D", "E", "F", "G", "H"}),
      Node.leaf(owner, 8, new Object[] {"I"}),
      Node.leaf(owner, 8, new Object[] {"J"})
    );

    root.rebalance(0, 3);

    assertThat(root.getUsedSlots()).isEqualTo(3);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("E", "F", "G"));
    assertThat(toList(root.getNode(2))).isEqualTo(List.of("H", "I", "J"));
  }

  @Test
  void emptyOutShouldWork() {
    Node root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C"}),
      Node.leaf(owner, 8, new Object[] {"D", "E", "F"}),
      Node.leaf(owner, 8, new Object[] {"G", "H", "I"})
    );

    assertThat(root.emptyOut(0, 3, 0)).isTrue();

    assertThat(root.getUsedSlots()).isEqualTo(2);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D", "E"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("F", "G", "H", "I"));

    root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C"}),
      Node.leaf(owner, 8, new Object[] {"D", "E", "F"}),
      Node.leaf(owner, 8, new Object[] {"G", "H", "I"})
    );

    assertThat(root.emptyOut(0, 3, 1)).isTrue();

    assertThat(root.getUsedSlots()).isEqualTo(2);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D", "E"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("F", "G", "H", "I"));

    root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C"}),
      Node.leaf(owner, 8, new Object[] {"D", "E", "F"}),
      Node.leaf(owner, 8, new Object[] {"G", "H", "I"})
    );

    assertThat(root.emptyOut(0, 3, 2)).isTrue();

    assertThat(root.getUsedSlots()).isEqualTo(2);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D", "E"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("F", "G", "H", "I"));

    root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C"}),
      Node.leaf(owner, 8, new Object[] {"D"}),
      Node.leaf(owner, 8, new Object[] {"E", "F", "G", "H", "I"})
    );

    assertThat(root.emptyOut(0, 3, 1)).isTrue();

    assertThat(root.getUsedSlots()).isEqualTo(2);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D", "E"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("F", "G", "H", "I"));

    root = Node.nodes(
      owner,
      8,
      Node.leaf(owner, 8, new Object[] {"A", "B", "C", "D", "E", "F"}),
      Node.leaf(owner, 8, new Object[] {"G"}),
      Node.leaf(owner, 8, new Object[] {"H", "I"})
    );

    assertThat(root.emptyOut(0, 3, 1)).isTrue();

    assertThat(root.getUsedSlots()).isEqualTo(2);
    assertThat(toList(root)).isEqualTo(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"));
    assertThat(toList(root.getNode(0))).isEqualTo(List.of("A", "B", "C", "D", "E"));
    assertThat(toList(root.getNode(1))).isEqualTo(List.of("F", "G", "H", "I"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 36, 54, 81, 121, 181, 271, 406, 609})
  void shouldCreateCompleteTrees(int size) {
    List<String> data = new ArrayList<>();

    for(int i = 0; i < size; i++) {
      data.add("" + i);
    }

    Node root = Node.create(owner, 8, 16, data.iterator());

    assertThat(toList(root)).isEqualTo(data);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 36, 54, 81, 121, 181, 271, 406, 609, 1000, 2000, 5000, 20000})
  void shouldCopyCompleteTrees(int size) {
    List<String> data = new ArrayList<>();

    for(int i = 0; i < size; i++) {
      data.add("" + i);
    }

    Node root = Node.create(owner, 8, 16, data.iterator());
    Node copy = root.copy(owner);

    assertThat(toList(root)).isEqualTo(data);
    assertThat(toList(copy)).isEqualTo(data);

    root.remove(owner, 0);

    assertThat(toList(root)).isNotEqualTo(data);
    assertThat(toList(copy)).isEqualTo(data);
  }

  @Test
  void findLeafShouldMatchFindSlot() {
    List<String> data = new ArrayList<>();

    for(int i = 0; i < 200; i++) {
      data.add("" + i);
    }

    Node root = Node.create(owner, 8, 8, data.iterator());

    assertLeafSlotsAreConsistent(root);

    for(int i = 0; i < 50; i++) {
      root.remove(owner, 0);
    }

    assertLeafSlotsAreConsistent(root);
  }

  /*
   * Moving the origin of a node back to its starting value is only needed after about 2^29
   * size changes in the same direction, which is not practical to do, so this test puts all
   * origins near the edge of their band first.
   */
  @ParameterizedTest
  @CsvSource({"8,8,1", "8,8,-1", "8,16,1", "16,8,-1", "16,16,1", "32,64,-1"})
  void shouldKeepWorkingWhenOriginsStartNearTheEdgeOfTheirBand(int nodeSize, int leafSize, int direction) throws IllegalAccessException {
    constructTree(nodeSize, leafSize, 3000);

    setOrigins(root, Node.ORIGIN + direction * (Node.ORIGIN_BAND - 6));

    assertAgainstReference();

    /*
     * Changes at the front update the origin of each node on the leftmost path by one each time,
     * which moves them over the edge of their band, upwards for removals and downwards for additions.
     */

    for(int i = 0; i < 1000; i++) {
      if(direction > 0) {
        remove(0);
      }
      else {
        add(0, "f" + i);
      }
    }

    assertAgainstReference();

    Random random = new Random(1);

    for(int i = 0; i < 4000; i++) {
      int index = switch(random.nextInt(4)) {
        case 0 -> 0;
        case 1 -> Math.min(size, 1 + random.nextInt(3));
        case 2 -> size;
        default -> random.nextInt(size + 1);
      };

      if(random.nextInt(100) < 55 || size < 50) {
        add(index, "x" + i);
      }
      else {
        remove(Math.min(index, size - 1));
      }

      if(i % 10 == 0) {
        assertAgainstReference();
      }
    }

    assertAgainstReference();

    /*
     * The origins started 6 away from the edge, which is far away from their starting value, so
     * an origin that is near its starting value now can only have been moved back.
     */

    assertThat(countOriginsNearStart(root)).as("nodes with an origin that was moved back").isGreaterThan(0);
    assertThat(countOriginsOutsideBand(root)).as("nodes with an origin outside the band").isZero();
  }

  /*
   * Sizes of whole subtrees are only ever passed to adjustSize when the child is last or still
   * empty, which never moves the origin, so no real operation reaches the branches for large
   * amounts. This calls adjustSize directly, with amounts at and around the size of the band, and
   * checks that undoing the change restores everything.
   */
  @ParameterizedTest
  @CsvSource({
    "1,           first,   0",
    "-1,          first,   0",
    "1,           middle,  0",
    "1,           last,    0",
    "536870911,   first,   0",
    "536870912,   first,   0",
    "536870913,   first,   0",
    "1073741824,  first,   0",
    "536870912,   middle,  0",
    "536870913,   middle,  0",
    "536870912,   last,    0",
    "1073741824,  last,    0",
    "1,           first,   1",
    "1,           first,  -1",
    "536870912,   first,   1",
    "536870912,   first,  -1",
    "536870913,   first,   1",
    "536870913,   first,  -1",
    "1073741824,  first,   1",
    "1073741824,  first,  -1"
  })
  void shouldRestoreOffsetsAfterAdjustingSizeBackAndForth(int amount, String position, int edge) throws Exception {
    constructTree(8, 8, 20000);

    if(edge != 0) {
      setOrigins(root, Node.ORIGIN + edge * (Node.ORIGIN_BAND - 6));
    }

    int used = USED_SLOTS.getInt(root);

    assertThat(used).isGreaterThan(3);

    int index = switch(position) {
      case "first" -> 0;
      case "middle" -> used / 2;
      default -> used - 1;
    };

    int[] offsetsBefore = offsets(root);
    int totalBefore = TOTAL.getInt(root);

    ADJUST_SIZE.invoke(root, index, amount);

    int[] offsets = offsets(root);

    for(int i = 0; i < used; i++) {
      assertThat(offsets[i]).as("offset of child %d after adding %d to child %d", i, amount, index).isEqualTo(offsetsBefore[i] + (i > index ? amount : 0));
    }

    assertThat(TOTAL.getInt(root)).as("total").isEqualTo(totalBefore + amount);
    assertOriginIsConsistent(root);

    ADJUST_SIZE.invoke(root, index, -amount);

    assertThat(offsets(root)).as("offsets after undoing").isEqualTo(offsetsBefore);
    assertThat(TOTAL.getInt(root)).as("total after undoing").isEqualTo(totalBefore);
    assertOriginIsConsistent(root);
    assertAgainstReference();
  }

  /* Returns the offset of each used child, which is its entry in childStarts without the origin. */
  private static int[] offsets(Node node) throws IllegalAccessException {
    int[] starts = (int[])CHILD_STARTS.get(node);
    int rotation = ROTATION.getInt(node);
    int[] offsets = new int[USED_SLOTS.getInt(node)];

    for(int i = 0; i < offsets.length; i++) {
      offsets[i] = starts[i] - rotation;
    }

    return offsets;
  }

  private static void assertOriginIsConsistent(Node node) throws IllegalAccessException {
    int[] starts = (int[])CHILD_STARTS.get(node);
    int rotation = ROTATION.getInt(node);
    int used = USED_SLOTS.getInt(node);

    assertThat(Math.abs((long)rotation - Node.ORIGIN)).as("distance of origin to its starting value").isLessThanOrEqualTo(Node.ORIGIN_BAND);
    assertThat(starts[0]).as("first entry").isEqualTo(rotation);
    assertThat(starts[used]).as("terminator").isEqualTo(Integer.MAX_VALUE);
  }

  private static final Method ADJUST_SIZE = method("adjustSize", int.class, int.class);
  private static final Field TOTAL = field("total");
  private static final Field CHILD_STARTS = field("childStarts");
  private static final Field SLOTS = field("slots");
  private static final Field USED_SLOTS = field("usedSlots");
  private static final Field ROTATION = field("rotation");

  private static Method method(String name, Class<?>... parameterTypes) {
    try {
      Method method = Node.class.getDeclaredMethod(name, parameterTypes);

      method.setAccessible(true);

      return method;
    }
    catch(NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  private static Field field(String name) {
    try {
      Field field = Node.class.getDeclaredField(name);

      field.setAccessible(true);

      return field;
    }
    catch(NoSuchFieldException e) {
      throw new AssertionError(e);
    }
  }

  /* Gives all non-leaf nodes in the tree the given origin, keeping every child at the same offset. */
  private static void setOrigins(Node node, int origin) throws IllegalAccessException {
    int[] starts = (int[])CHILD_STARTS.get(node);

    if(starts == null) {  // a leaf
      return;
    }

    int used = USED_SLOTS.getInt(node);
    Object[] slots = (Object[])SLOTS.get(node);
    int delta = origin - ROTATION.getInt(node);

    for(int i = 0; i < used; i++) {
      starts[i] += delta;
    }

    ROTATION.setInt(node, origin);

    for(int i = 0; i < used; i++) {
      setOrigins((Node)slots[i], origin);
    }
  }

  private static int countOriginsNearStart(Node node) throws IllegalAccessException {
    return countOrigins(node, false);
  }

  private static int countOriginsOutsideBand(Node node) throws IllegalAccessException {
    return countOrigins(node, true);
  }

  private static int countOrigins(Node node, boolean outsideBand) throws IllegalAccessException {
    if(CHILD_STARTS.get(node) == null) {  // a leaf
      return 0;
    }

    int origin = ROTATION.getInt(node);
    int distance = Math.abs(origin - Node.ORIGIN);
    int count = (outsideBand ? distance > Node.ORIGIN_BAND : distance < 100000) ? 1 : 0;
    Object[] slots = (Object[])SLOTS.get(node);

    for(int i = 0, used = USED_SLOTS.getInt(node); i < used; i++) {
      count += countOrigins((Node)slots[i], outsideBand);
    }

    return count;
  }

  private static void assertLeafSlotsAreConsistent(Node root) {
    int size = root.sumChildSizes();

    for(int i = 0; i <= size; i++) {
      assertThat(root.findLeaf(i)).isSameAs(root.findSlot(i).node());
    }
  }

  private static List<Object> toList(Node root) {
    List<Object> data = new ArrayList<>();

    for(int i = 0, max = root.sumChildSizes(); i < max; i++) {
      data.add(root.get(i));
    }

    return data;
  }

  void assertAgainstReference() {
    List<Object> data = toList(root);

    assertThat(data).isEqualTo(reference);
  }

  void add(int index, String element) {
    reference.add(index, element);
    root.add(owner, index, element);

    if(root.isFull()) {  // single node only and not leaf already?
      root = root.splitFullRoot(owner, nodeSize);
    }

    size++;
  }

  Object remove(int index) {
    Object element = root.remove(owner, index);
    Object referenceElement = reference.remove(index);

    if(root.getUsedSlots() == 1 && !root.isLeaf()) {  // single node only and not leaf already?
      root = root.promoteOnlyChild();
    }

    size--;

    assertThat(referenceElement).isEqualTo(element);

    return element;
  }

  void constructTree(int nodeSize, int leafSize, int elementCount) {
    Node root = Node.empty(owner, leafSize);
    int c = elementCount;

    while(c-- > 0) {
      root.add(owner, 0, "" + c);
      reference.add(0, "" + c);
      size++;

      if(root.isFull()) {
        root = root.splitFullRoot(owner, nodeSize);
      }
    }

    this.root = root;
    this.nodeSize = nodeSize;
  }
}
