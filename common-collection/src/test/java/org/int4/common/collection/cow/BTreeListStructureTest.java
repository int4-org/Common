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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the internal structure of the tree (offsets, origins, terminators, totals, leaf rings
 * and depth) after random operations, which the {@code List} based tests can't see.
 */
public class BTreeListStructureTest {
  private static final Field ROOT = field(BTreeList.class, "root");
  private static final Field HEIGHT = field(BTreeList.class, "height");
  private static final Field CHILD_STARTS = field(Node.class, "childStarts");
  private static final Field SLOTS = field(Node.class, "slots");
  private static final Field USED_SLOTS = field(Node.class, "usedSlots");
  private static final Field ROTATION = field(Node.class, "rotation");
  private static final Field TOTAL = field(Node.class, "total");

  @ParameterizedTest
  @CsvSource({
    "8,   8,   0",
    "8,   8,   1",
    "8,   8,   2",
    "8,   8,   3",
    "8,   16,  0",
    "16,  8,   1",
    "16,  8,   2",
    "16,  16,  3",
    "16,  64,  0",
    "32,  32,  1",
    "32,  8,   2",
    "64,  8,   0"
  })
  void shouldKeepTreeStructureValidUnderRandomOperations(int nodeSize, int leafSize, int mode) throws IllegalAccessException {
    for(long seed = 1; seed <= 2; seed++) {
      runRandomOperations(seed, nodeSize, leafSize, mode, 1500);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "8,   8",
    "16,  8",
    "8,   16",
    "32,  32"
  })
  void shouldCreateValidTreesFromCollectionsOfAnySize(int nodeSize, int leafSize) throws IllegalAccessException {
    List<Integer> values = new ArrayList<>();

    for(int size = 0; size <= 600; size++) {
      BTreeList<Integer> list = new BTreeList<>(nodeSize, leafSize, values);

      assertStructure(list, values, "created with size " + size);

      if(size % 37 == 0) {
        list.add(size / 2, -1);
        list.remove(0);

        List<Integer> expected = new ArrayList<>(values);

        expected.add(size / 2, -1);
        expected.remove(0);

        assertStructure(list, expected, "modified after creation with size " + size);
      }

      values.add(size);
    }
  }

  @Test
  void shouldKeepTreeStructureValidWhenGrowingAndShrinkingAtBothEnds() throws IllegalAccessException {
    BTreeList<Integer> list = new BTreeList<>(8, 8);
    List<Integer> reference = new ArrayList<>();

    for(int i = 0; i < 1200; i++) {
      list.add(0, i);
      reference.add(0, i);

      list.add(i);
      reference.add(i);

      if(i % 10 == 0) {
        assertStructure(list, reference, "growing, step " + i);
      }
    }

    while(!reference.isEmpty()) {
      list.removeFirst();
      reference.removeFirst();

      if(!reference.isEmpty()) {
        list.removeLast();
        reference.removeLast();
      }

      if(reference.size() % 20 == 0 || reference.size() < 50) {
        assertStructure(list, reference, "shrinking, size " + reference.size());
      }
    }
  }

  private static void runRandomOperations(long seed, int nodeSize, int leafSize, int mode, int operations) throws IllegalAccessException {
    Random random = new Random(seed);
    BTreeList<Integer> list = new BTreeList<>(nodeSize, leafSize);
    List<Integer> reference = new ArrayList<>();
    List<BTreeList<Integer>> clones = new ArrayList<>();
    List<List<Integer>> cloneReferences = new ArrayList<>();
    int counter = 0;

    for(int i = 0; i < operations; i++) {
      int op = random.nextInt(100);
      int size = reference.size();
      boolean grow = mode == 1 ? (i / (operations / 6)) % 2 == 0 : op < 52;
      String description = "seed " + seed + ", nodeSize " + nodeSize + ", leafSize " + leafSize + ", mode " + mode + ", step " + i;

      if(op >= 94) {
        if(op < 95 && clones.size() < 3) {
          clones.add(list.clone());
          cloneReferences.add(new ArrayList<>(reference));
        }
        else if(op < 97 && !clones.isEmpty()) {
          int k = random.nextInt(clones.size());
          BTreeList<Integer> clone = clones.get(k);
          List<Integer> cloneReference = cloneReferences.get(k);
          int index = random.nextInt(cloneReference.size() + 1);

          if(random.nextBoolean() || cloneReference.isEmpty()) {
            clone.add(index, ++counter);
            cloneReference.add(index, counter);
          }
          else {
            clone.remove(index % cloneReference.size());
            cloneReference.remove(index % cloneReference.size());
          }

          assertStructure(clone, cloneReference, "clone, " + description);
        }
        else if(op < 98 && size > 0) {
          int index = random.nextInt(size);

          list.set(index, ++counter);
          reference.set(index, counter);
        }
        else if(op < 99 && size > 1) {
          int from = random.nextInt(size);
          int to = from + random.nextInt(size - from + 1);

          list.subList(from, to).clear();
          reference.subList(from, to).clear();
        }
        else {
          int index = random.nextInt(size + 1);
          List<Integer> values = new ArrayList<>();

          for(int k = random.nextInt(3 * leafSize); k > 0; k--) {
            values.add(++counter);
          }

          list.addAll(index, values);
          reference.addAll(index, values);
        }
      }
      else if(grow || size == 0) {
        int index = switch(mode) {
          case 2 -> random.nextBoolean() ? 0 : size;
          case 3 -> size / 2;
          default -> random.nextInt(size + 1);
        };

        list.add(index, ++counter);
        reference.add(index, counter);
      }
      else {
        int index = switch(mode) {
          case 2 -> random.nextBoolean() ? 0 : size - 1;
          case 3 -> size / 2;
          default -> random.nextInt(size);
        };

        list.remove(index);
        reference.remove(index);
      }

      if(i % 5 == 0 || reference.size() < 100) {
        assertStructure(list, reference, description);
      }
    }

    assertStructure(list, reference, "seed " + seed + " at the end");

    for(int k = 0; k < clones.size(); k++) {
      assertStructure(clones.get(k), cloneReferences.get(k), "clone " + k + ", seed " + seed + " at the end");
    }
  }

  private static void assertStructure(BTreeList<Integer> list, List<Integer> expected, String description) throws IllegalAccessException {
    Node root = (Node)ROOT.get(list);
    int[] leafDepth = {-1};
    int size = check(root, true, 0, leafDepth, description);

    assertThat(HEIGHT.getInt(list)).as("%s: height", description).isEqualTo(leafDepth[0]);

    assertThat(size).as("%s: tree total", description).isEqualTo(expected.size());
    assertThat(list).as(description).containsExactlyElementsOf(expected);
  }

  /*
   * Verifies a node and everything below it, and returns the number of elements it holds. Besides
   * the structure itself this checks that all leaves are at the same depth, that no node besides
   * the root is empty, and that a root is never a non-leaf with a single child.
   */
  private static int check(Node node, boolean isRoot, int depth, int[] leafDepth, String description) throws IllegalAccessException {
    Object[] slots = (Object[])SLOTS.get(node);
    int[] starts = (int[])CHILD_STARTS.get(node);
    int used = USED_SLOTS.getInt(node);
    int rotation = ROTATION.getInt(node);

    assertThat(used).as("%s: used slots", description).isBetween(0, slots.length);

    if(starts == null) {
      return checkLeaf(slots, used, rotation, isRoot, depth, leafDepth, description);
    }

    int total = TOTAL.getInt(node);
    int running = 0;

    assertThat(used).as("%s: used slots of non-leaf", description).isGreaterThanOrEqualTo(isRoot ? 2 : 1);
    assertThat(Math.abs((long)rotation - Node.ORIGIN)).as("%s: origin distance", description).isLessThanOrEqualTo(Node.ORIGIN_BAND);

    for(int i = 0; i < used; i++) {
      assertThat(starts[i]).as("%s: entry %d", description, i).isEqualTo(running + rotation);

      running += check((Node)slots[i], false, depth + 1, leafDepth, description);
    }

    assertThat(total).as("%s: total", description).isEqualTo(running);
    assertThat(starts[0]).as("%s: first entry equals origin", description).isEqualTo(rotation);

    if(used < starts.length) {
      assertThat(starts[used]).as("%s: terminator", description).isEqualTo(Integer.MAX_VALUE);
    }

    for(int i = used; i < slots.length; i++) {
      assertThat(slots[i]).as("%s: stale child reference at %d", description, i).isNull();
    }

    return running;
  }

  private static int checkLeaf(Object[] slots, int used, int rotation, boolean isRoot, int depth, int[] leafDepth, String description) {
    for(int i = 0; i < slots.length; i++) {
      boolean inUse = ((i - rotation) & (slots.length - 1)) < used;

      if(inUse) {
        assertThat(slots[i]).as("%s: missing element at slot %d", description, i).isNotNull();
      }
      else {
        assertThat(slots[i]).as("%s: stale reference at slot %d", description, i).isNull();
      }
    }

    if(leafDepth[0] == -1) {
      leafDepth[0] = depth;
    }

    assertThat(depth).as("%s: leaf depth", description).isEqualTo(leafDepth[0]);

    if(!isRoot) {
      assertThat(used).as("%s: non-root leaf is empty", description).isGreaterThan(0);
    }

    return used;
  }

  private static Field field(Class<?> type, String name) {
    try {
      Field field = type.getDeclaredField(name);

      field.setAccessible(true);

      return field;
    }
    catch(NoSuchFieldException e) {
      throw new AssertionError(e);
    }
  }
}
