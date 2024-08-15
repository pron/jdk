/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.util.stream;

import java.util.IntSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code int}.
 *
 * @param <E_IN> type of elements in the upstream source
 * @since 1.8
 */
abstract class IntPipeline<E_IN, throws X_IN, throws X_OUT extends X_IN|X, throws X>
        extends AbstractPipeline<E_IN, X_IN, Integer, X_OUT, X, IntStream<X_OUT>>
        implements IntStream<X_OUT> {

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source {@code Supplier<Spliterator>} describing the stream source
     * @param sourceFlags The source flags for the stream source, described in
     *        {@link StreamOpFlag}
     * @param parallel {@code true} if the pipeline is parallel
     */
    IntPipeline(Supplier<? extends Spliterator<Integer, ?>> source,
                int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source {@code Spliterator} describing the stream source
     * @param sourceFlags The source flags for the stream source, described in
     *        {@link StreamOpFlag}
     * @param parallel {@code true} if the pipeline is parallel
     */
    IntPipeline(Spliterator<Integer, ?> source,
                int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }

    /**
     * Constructor for appending an intermediate operation onto an existing
     * pipeline.
     *
     * @param upstream the upstream element source
     * @param opFlags the operation flags for the new operation
     */
    IntPipeline(AbstractPipeline<?, ?, E_IN, X_IN, ?, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }

    /**
     * Adapt a {@code Sink<Integer> to an {@code IntConsumer}, ideally simply
     * by casting.
     */
    private static IntConsumer adapt(Sink<Integer> sink) {
        if (sink instanceof IntConsumer) {
            return (IntConsumer) sink;
        }
        else {
            if (Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class,
                              "using IntStream.adapt(Sink<Integer> s)");
            return sink::accept;
        }
    }

    /**
     * Adapt a {@code Spliterator<Integer>} to a {@code Spliterator.OfInt}.
     *
     * @implNote
     * The implementation attempts to cast to a Spliterator.OfInt, and throws an
     * exception if this cast is not possible.
     */
    @SuppressWarnings("unchecked")
    private static <throws X> Spliterator.OfInt<X> adapt(Spliterator<Integer, X> s) {
        if (s instanceof Spliterator.OfInt<?>) {
            return (Spliterator.OfInt<X>) s;
        }
        else {
            if (Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class,
                              "using IntStream.adapt(Spliterator<Integer, X> s)");
            throw new UnsupportedOperationException("IntStream.adapt(Spliterator<Integer, X> s)");
        }
    }


    // Shape-specific methods

    @Override
    final StreamShape getOutputShape() {
        return StreamShape.INT_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    final <P_IN, throws XIN extends X_OUT> Node<Integer> evaluateToNode(PipelineHelper<Integer, X_OUT, X> helper,
                                              Spliterator<P_IN, XIN> spliterator,
                                              boolean flattenTree,
                                              IntFunction<Integer[]> generator) {
        return Nodes.collectInt(helper, (Spliterator<P_IN>)spliterator, flattenTree);
    }

    @Override
    final <P_IN, throws XIN extends X_OUT> Spliterator<Integer, X_OUT> wrap(PipelineHelper<Integer, X_OUT, X> ph,
                                           Supplier<Spliterator<P_IN, XIN>> supplier,
                                           boolean isParallel) {
        return new StreamSpliterators.IntWrappingSpliterator<P_IN, X_OUT, X>(ph, supplier, isParallel);
    }

    @Override
    @SuppressWarnings("unchecked")
    final Spliterator.OfInt<X_OUT> lazySpliterator(Supplier<? extends Spliterator<Integer, X_OUT>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator.OfInt<>((Supplier<Spliterator.OfInt<X_OUT>>)supplier);
    }

    @Override
    final boolean forEachWithCancel(Spliterator<Integer> spliterator, Sink<Integer> sink) {
        Spliterator.OfInt spl = adapt(spliterator);
        IntConsumer adaptedSink = adapt(sink);
        boolean cancelled;
        do { } while (!(cancelled = sink.cancellationRequested()) && spl.tryAdvance(adaptedSink));
        return cancelled;
    }

    @Override
    final Node.Builder<Integer> makeNodeBuilder(long exactSizeIfKnown,
                                                IntFunction<Integer[]> generator) {
        return Nodes.intBuilder(exactSizeIfKnown);
    }

    private <U> Stream<U, X_OUT> mapToObj(IntFunction<? extends U> mapper, int opFlags) {
        return new ReferencePipeline.StatelessOp<Integer, X_OUT, U, X_OUT, RuntimeException>(this, StreamShape.INT_VALUE, opFlags) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<U> sink) {
                return new Sink.ChainedInt<U>(sink) {
                    @Override
                    public void accept(int t) {
                        try {
                            downstream.accept(mapper.apply(t));
                        } catch (Exception ex) {
                            throw CheckedExceptions.wrap(ex);
                        }
                    }
                };
            }
        };
    }

    // IntStream

    @Override
    public final PrimitiveIterator.OfInt<X_OUT> iterator() {
        return Spliterators.iterator(spliterator());
    }

    @Override
    public final Spliterator.OfInt<X_OUT> spliterator() {
        return adapt(super.spliterator());
    }

    // Stateless intermediate ops from IntStream

    @Override
    public final LongStream<X_OUT> asLongStream() {
        return new LongPipeline.StatelessOp<>(eraseException(), StreamShape.INT_VALUE, 0) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedInt<>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept((long) t);
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream<X_OUT> asDoubleStream() {
        return new DoublePipeline.StatelessOp<>(eraseException(), StreamShape.INT_VALUE, 0) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedInt<Double>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept((double) t);
                    }
                };
            }
        };
    }

    @Override
    public final Stream<Integer, X_OUT> boxed() {
        return mapToObj(Integer::valueOf, 0);
    }

    @Override
    public final IntStream<X_OUT> map(IntUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<>(this, StreamShape.INT_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<Integer>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept(mapper.applyAsInt(t));
                    }
                };
            }
        };
    }

    @Override
    public final <U> Stream<U, X_OUT> mapToObj(IntFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return mapToObj(mapper, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT);
    }

    @Override
    public final LongStream<X_OUT> mapToLong(IntToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline.StatelessOp<>(eraseException(), StreamShape.INT_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedInt<Long>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept(mapper.applyAsLong(t));
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream<X_OUT> mapToDouble(IntToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline.StatelessOp<>(eraseException(), StreamShape.INT_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedInt<>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept(mapper.applyAsDouble(t));
                    }
                };
            }
        };
    }

    @Override
    public final IntStream<X_OUT> flatMap(IntFunction<? extends IntStream> mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<>(this, StreamShape.INT_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                final IntConsumer fastPath =
                        isShortCircuitingPipeline()
                                ? null
                                : (sink instanceof IntConsumer ic)
                                ? ic
                                : sink::accept;
                final class FlatMap implements Sink.OfInt, IntPredicate {
                    boolean cancel;

                    @Override public void begin(long size) { sink.begin(-1); }
                    @Override public void end() { 
                        sink.end();
                    }

                    @Override
                    public void accept(int e) {
                        try (IntStream result = mapper.apply(e)) {
                            if (result != null) {
                                if (fastPath == null)
                                    result.sequential().allMatch(this);
                                else
                                    result.sequential().forEach(fastPath);
                            }
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return cancel || (cancel |= sink.cancellationRequested());
                    }

                    @Override
                    public boolean test(int output) {
                        if (!cancel) {
                            sink.accept(output);
                            return !(cancel |= sink.cancellationRequested());
                        } else {
                            return false;
                        }
                    }
                }
                return new FlatMap();
            }
        };
    }

    @Override
    public final IntStream<X_OUT> mapMulti(IntMapMultiConsumer mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<>(this, StreamShape.INT_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<>(sink) {

                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void accept(int t) {
                        mapper.accept(t, (IntConsumer) downstream);
                    }
                };
            }
        };
    }

    @Override
    public IntStream<X_OUT> unordered() {
        if (!isOrdered())
            return this;
        return new StatelessOp<>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_ORDERED) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return sink;
            }
        };
    }

    @Override
    public final IntStream<X_OUT> filter(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new StatelessOp<>(this, StreamShape.INT_VALUE,
                StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<>(sink) {
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(int t) {
                        if (predicate.test(t))
                            downstream.accept(t);
                    }
                };
            }
        };
    }

    @Override
    public final IntStream<X_OUT> peek(IntConsumer action) {
        Objects.requireNonNull(action);
        return new StatelessOp<>(this, StreamShape.INT_VALUE,
                0) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<>(sink) {
                    @Override
                    public void accept(int t) {
                        action.accept(t);
                        downstream.accept(t);
                    }
                };
            }
        };
    }

    // Stateful intermediate ops from IntStream

    @Override
    public final IntStream<X_OUT> limit(long maxSize) {
        if (maxSize < 0)
            throw new IllegalArgumentException(Long.toString(maxSize));
        return SliceOps.makeInt(this, 0, maxSize);
    }

    @Override
    public final IntStream<X_OUT> skip(long n) {
        if (n < 0)
            throw new IllegalArgumentException(Long.toString(n));
        if (n == 0)
            return this;
        else
            return SliceOps.makeInt(this, n, -1);
    }

    @Override
    public final IntStream<X_OUT> takeWhile(IntPredicate predicate) {
        return WhileOps.makeTakeWhileInt(this, predicate);
    }

    @Override
    public final IntStream<X_OUT> dropWhile(IntPredicate predicate) {
        return WhileOps.makeDropWhileInt(this, predicate);
    }

    @Override
    public final IntStream<X_OUT> sorted() {
        return SortedOps.makeInt(this);
    }

    @Override
    public final IntStream<X_OUT> distinct() {
        // While functional and quick to implement, this approach is not very efficient.
        // An efficient version requires an int-specific map/set implementation.
        return boxed().distinct().mapToInt(i -> i);
    }

    // Terminal ops from IntStream

    @Override
    public void forEach(IntConsumer action) throws X_OUT {
        evaluate(ForEachOps.makeInt(action, false));
    }

    @Override
    public void forEachOrdered(IntConsumer action) throws X_OUT {
        evaluate(ForEachOps.makeInt(action, true));
    }

    @Override
    public final int sum() throws X_OUT {
        return reduce(0, Integer::sum);
    }

    @Override
    public final OptionalInt min() throws X_OUT {
        return reduce(Math::min);
    }

    @Override
    public final OptionalInt max() throws X_OUT {
        return reduce(Math::max);
    }

    @Override
    public final long count() throws X_OUT {
        return evaluate(ReduceOps.makeIntCounting());
    }

    @Override
    public final OptionalDouble average() throws X_OUT {
        long[] avg = collect(() -> new long[2],
                             (ll, i) -> {
                                 ll[0]++;
                                 ll[1] += i;
                             },
                             (ll, rr) -> {
                                 ll[0] += rr[0];
                                 ll[1] += rr[1];
                             });
        return avg[0] > 0
               ? OptionalDouble.of((double) avg[1] / avg[0])
               : OptionalDouble.empty();
    }

    @Override
    public final IntSummaryStatistics summaryStatistics() throws X_OUT {
        return collect(IntSummaryStatistics::new, IntSummaryStatistics::accept,
                       IntSummaryStatistics::combine);
    }

    @Override
    public final int reduce(int identity, IntBinaryOperator op) throws X_OUT {
        return evaluate(ReduceOps.makeInt(identity, op));
    }

    @Override
    public final OptionalInt reduce(IntBinaryOperator op) throws X_OUT {
        return evaluate(ReduceOps.makeInt(op));
    }

    @Override
    public final <R> R collect(Supplier<R> supplier,
                               ObjIntConsumer<R> accumulator,
                               BiConsumer<R, R> combiner) throws X_OUT {
        Objects.requireNonNull(combiner);
        BinaryOperator<R> operator = (left, right) -> {
            combiner.accept(left, right);
            return left;
        };
        return evaluate(ReduceOps.makeInt(supplier, accumulator, operator));
    }

    @Override
    public final boolean anyMatch(IntPredicate predicate) throws X_OUT {
        return evaluate(MatchOps.makeInt(predicate, MatchOps.MatchKind.ANY));
    }

    @Override
    public final boolean allMatch(IntPredicate predicate) throws X_OUT {
        return evaluate(MatchOps.makeInt(predicate, MatchOps.MatchKind.ALL));
    }

    @Override
    public final boolean noneMatch(IntPredicate predicate) throws X_OUT {
        return evaluate(MatchOps.makeInt(predicate, MatchOps.MatchKind.NONE));
    }

    @Override
    public final OptionalInt findFirst() throws X_OUT {
        return evaluate(FindOps.makeInt(true));
    }

    @Override
    public final OptionalInt findAny() throws X_OUT {
        return evaluate(FindOps.makeInt(false));
    }

    @Override
    public final int[] toArray() throws X_OUT {
        return Nodes.flattenInt((Node.OfInt) evaluateToArrayNode(Integer[]::new))
                        .asPrimitiveArray();
    }

    //

    /**
     * Source stage of an IntStream.
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    static class Head<E_IN, throws X_OUT> extends IntPipeline<E_IN, X_OUT, X_OUT> {
        /**
         * Constructor for the source stage of an IntStream.
         *
         * @param source {@code Supplier<Spliterator>} describing the stream
         *               source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel {@code true} if the pipeline is parallel
         */
        Head(Supplier<? extends Spliterator<Integer, X_OUT>> source,
             int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }

        /**
         * Constructor for the source stage of an IntStream.
         *
         * @param source {@code Spliterator} describing the stream source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel {@code true} if the pipeline is parallel
         */
        Head(Spliterator<Integer, X_OUT> source,
             int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }

        @Override
        final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        @Override
        final Sink<E_IN> opWrapSink(int flags, Sink<Integer> sink) {
            throw new UnsupportedOperationException();
        }

        // Optimized sequential terminal operations for the head of the pipeline

        @Override
        public void forEach(IntConsumer action) throws X_OUT {
            if (!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(action);
            }
            else {
                super.forEach(action);
            }
        }

        @Override
        public void forEachOrdered(IntConsumer action) throws X_OUT {
            if (!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(action);
            }
            else {
                super.forEachOrdered(action);
            }
        }
    }

    /**
     * Base class for a stateless intermediate stage of an IntStream
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    abstract static class StatelessOp<E_IN, throws X_IN, throws X_OUT extends X_IN|X, throws X>
        extends IntPipeline<E_IN, X_IN, X_OUT, X> {
        /**
         * Construct a new IntStream by appending a stateless intermediate
         * operation to an existing stream.
         * @param upstream The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags Operation flags for the new stage
         */
        StatelessOp(AbstractPipeline<?, ?, E_IN, X_IN, ?, ?> upstream,
                    StreamShape inputShape,
                    int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
        }

        @Override
        final boolean opIsStateful() {
            return false;
        }
    }

    /**
     * Base class for a stateful intermediate stage of an IntStream.
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    abstract static class StatefulOp<E_IN, throws X_IN, throws X_OUT extends X_IN|X, throws X>
        extends IntPipeline<E_IN, X_IN, X_OUT, X> {
        /**
         * Construct a new IntStream by appending a stateful intermediate
         * operation to an existing stream.
         * @param upstream The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags Operation flags for the new stage
         */
        StatefulOp(AbstractPipeline<?, ?, E_IN, X_IN, ?, ?> upstream,
                   StreamShape inputShape,
                   int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
        }

        @Override
        final boolean opIsStateful() {
            return true;
        }

        @Override
        abstract <P_IN, throws XX extends X_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer, XX, X> helper,
                                                         Spliterator<P_IN, XX> spliterator,
                                                         IntFunction<Integer[]> generator) throws X_OUT;
    }
}
