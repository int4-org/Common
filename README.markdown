# Common

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.int4.common/parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.int4.common/parent)
[![Build Status](https://github.com/int4-org/Common/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/int4-org/Common/actions)
[![Coverage](https://codecov.io/gh/int4-org/Common/branch/master/graph/badge.svg?token=QCNNRFYF98)](https://codecov.io/gh/int4-org/Common)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![javadoc](https://javadoc.io/badge2/org.int4.common/parent/javadoc.svg)](https://javadoc.io/doc/org.int4.common/parent)

Commonly usable classes without dependencies.

## `org.int4.common.collection.ShiftList`

`ShiftList` is a high-performance, general-purpose `List` and `Deque` implementation optimized for
use cases where frequent insertions and removals occur near the head or tail of the list. It combines
the performance benefits of block-based storage with a rotating offset mechanism to avoid costly
element shifts typical of `ArrayList`.

### Why `ShiftList`?

Standard Java collections like `ArrayList` and `LinkedList` trade off insertion/removal speed,
memory locality, and access time:

- `ArrayList` provides fast indexed access but performs progressively worse when inserting or removing elements
  further from the end, due to the need to shift elements.

- `LinkedList` allows efficient insertions/removals at both ends but has poor cache performance and slow random access.

- `ShiftList` is designed to support fast insertions and removals at any index (especially at the head and tail)
  while maintaining random access performance comparable to `ArrayList`.

### Key Design Concepts

- **Block-based storage**: The list is internally divided into fixed-size blocks within a contiguous array.
  Block size is dynamically determined based on capacity, and tuned for optimal performance.

- **Block rotation**: Instead of shifting elements on every insert/remove, blocks are rotated, with only overflow
  elements moved between them. This make structural modifications proportional to the number of affected blocks,
  not the total element count.

- **Global rotation**: A global rotation offset enables fast modifications near the list's start, effectively
  halving the number of affected blocks. Operations near the head or tail are close to `O(1)`, while those nearer
  to the middle degrade gradually to `O(blockSize)`.

### Benchmark Summary

All benchmarks were performed using [JMH](https://github.com/openjdk/jmh). For raw data see the benchmarks folder.

#### Insertions and removals

Measurements were done performing exactly 10,000 operations per iteration. For smaller lists, several
lists were used to not exceed `0.5 * size` elements added per list. The resuls are in nano seconds per
element.

| Operation    |     Size   |  ArrayList  | ShiftList |
|--------------|-----------:|------------:|----------:|
|Add First     |      1,000 |     54.2 ns |    2.3 ns |
|Add First     |    100,000 |  3,625.0 ns |    2.4 ns |
|Add First     | 10,000,000 |       (*) - |    3.5 ns |
|Add Last      |      1,000 |      2.5 ns |    2.2 ns |
|Add Last      |    100,000 | (**) 5.0 ns |    2.2 ns |
|Add Last      | 10,000,000 |      4.0 ns |    3.5 ns |
|Add Random    |      1,000 |     40.8 ns |   32.9 ns |
|Add Random    |    100,000 |  1,893.3 ns |   80.2 ns |
|Add Random    | 10,000,000 |425,507.3 ns | 1270.6 ns |
|Remove First  |      1,000 |     38.8 ns |    1.7 ns |
|Remove First  |    100,000 |   3205.8 ns |    1.7 ns |
|Remove First  | 10,000,000 |       (*) - |    2.1 ns |
|Remove Last   |      1,000 |      0.2 ns |    1.6 ns |
|Remove Last   |    100,000 |      0.6 ns |    1.6 ns |
|Remove Last   | 10,000,000 |      1.1 ns |    1.9 ns |
|Remove Random |      1,000 |     29.0 ns |   24.9 ns |
|Remove Random |    100,000 |  1,608.7 ns |   95.2 ns |
|Remove Random | 10,000,000 |411,580.1 ns | 1958.2 ns |

> (*) `ArrayList` timed out\
> (**) `ArrayList` does a re-allocate at 106,710 elements which skewed these results a bit

#### Reading

| Operation    |     Size   |  ArrayList  | ShiftList |
|--------------|-----------:|------------:|----------:|
|Get Random    |      1,000 |      0.2 ns |    1.0 ns |
|Get Random    |    100,000 |      0.4 ns |    1.1 ns |
|Get Random    | 10,000,000 |      2.1 ns |    2.4 ns |
|Get Sequential|      1,000 |      0.1 ns |    0.7 ns |
|Get Sequential|    100,000 |      0.1 ns |    0.7 ns |
|Get Sequential| 10,000,000 |      0.1 ns |    0.7 ns |

### Comparison with other Lists

A comparison of `ShiftList` with `ArrayList`, `LinkedList`, and Apache Commons Collections' `TreeList`:

| Operation              | ArrayList | LinkedList | TreeList  | ShiftList |
|------------------------|:---------:|:----------:|:---------:|:---------:|
| Add/remove at tail     | `O(1)`    | `O(1)`    | `O(log n)` | `O(1)`    |
| Add/remove at head     | `O(n)`    | `O(1)`    | `O(log n)` | `O(1)`    |
| Random access          | `O(1)`    | `O(n)`    | `O(log n)` | `O(1)`    |
| Add/remove at index    | `O(n)`    | `O(n)`    | `O(log n)` | `O(n/b)`  |

> Where `n` is the number of elements the list holds, and `b` is the block size of `ShiftList`

`ShiftList` provides performance close to `ArrayList` for the common `get(int)` operation due to minimal
indirection when locating an element. In contrast, `LinkedList` and `TreeList` require pointer traversal,
which incurs higher actual latency than their asymptotic complexity suggests.

| Feature                  | ArrayList | LinkedList | TreeList  | ShiftList |
|--------------------------|:---------:|:----------:|:---------:|:---------:|
| Memory locality          | ++        | --         | --        | ++        |
| Estimated memory/element | 4-6 bytes | ~24 bytes  | ~24 bytes | 4-8 bytes |

`LinkedList` and `TreeList` allocate a wrapper object per element, leading to high memory overhead
and poor memory locality due to non-contiguous storage. `ShiftList` stores elements in contiguous
blocks, improving cache performance. Its capacity is always a power of two, and in the worst case
(50% utilization), memory usage is approximately 8 bytes per element. By contrast, `ArrayList`
typically grows by 50% when resizing and can trim to fit, leading to more compact memory usage when
fully utilized.

### Limitations

- `ShiftList` has a minimum capacity of 16 elements
- `ShiftList` has a maximum capacity of 2^30 - 65536 elements (this is due to the restriction that
  its storage array must be a power of 2 in size, and that its first and last element can't be in
  the same block.)