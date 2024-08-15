/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.DoubleSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code double}.
 *
 * @param <E_IN> type of elements in the upstream source
 *
 * @since 1.8
 */
abstract class DoublePipeline<E_IN, throws X_IN, throws X_OUT extends X_IN|X, throws X>
        extends AbstractPipeline<E_IN, X_IN, Double, X_OUT, X, DoubleStream<X_OUT>>
        implements DoubleStream<X_OUT> {

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source {@code Supplier<Spliterator>} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in
     * {@link StreamOpFlag}
     */
    DoublePipeline(Supplier<? extends Spliterator<Double, ?>> source,
                   int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source {@code Spliterator} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in
     * {@link StreamOpFlag}
     */
    DoublePipeline(Spliterator<Double, ?> source,
                   int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }

    /**
     * Constructor for appending an intermediate operation onto an existing
     * pipeline.
     *
     * @param upstream the upstream element source.
     * @param opFlags the operation flags
     */
    DoublePipeline(AbstractPipeline<?, ?, E_IN, X_IN, ?, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }

    /**
     * Adapt a {@code Sink<Double> to a {@code DoubleConsumer}, ideally simply
     * by casting.
     */
    private static DoubleConsumer adapt(Sink<Double> sink) {
        if (sink instanceof DoubleConsumer) {
            return (DoubleConsumer) sink;
        } else {
            if (Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class,
                              "using DoubleStream.adapt(Sink<Double> s)");
            return sink::accept;
        }
    }

    /**
     * Adapt a {@code Spliterator<Double>} to a {@code Spliterator.OfDouble}.
     *
     * @implNote
     * The implementation attempts to cast to a Spliterator.OfDouble, and throws
     * an exception if this cast is not possible.
     */
    @SuppressWarnings("unchecked")
    private static <throws X> Spliterator.OfDouble<X> adapt(Spliterator<Double, X> s) {
        if (s instanceof Spliterator.OfDouble<X>) {
            return (Spliterator.OfDouble<X>) s;
        } else {
            if (Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class,
                              "using DoubleStream.adapt(Spliterator<Double, X> s)");
            throw new UnsupportedOperationException("DoubleStream.adapt(Spliterator<Double, X> s)");
        }
    }


    // Shape-specific methods

    @Override
    final StreamShape getOutputShape() {
        return StreamShape.DOUBLE_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    final <P_IN, throws XIN extends X_OUT> Node<Double> evaluateToNode(PipelineHelper<Double, X_OUT, X> helper,
                                             Spliterator<P_IN, XIN> spliterator,
                                             boolean flattenTree,
                                             IntFunction<Double[]> generator) {
        return Nodes.collectDouble(helper, (Spliterator<P_IN>)spliterator, flattenTree);
    }

    @Override
    final <P_IN, throws XIN extends X_OUT> Spliterator<Double, X_OUT> wrap(PipelineHelper<Double, X_OUT, X> ph,
                                          Supplier<Spliterator<P_IN, XIN>> supplier,
                                          boolean isParallel) {
        return new StreamSpliterators.DoubleWrappingSpliterator<P_IN, X_OUT, X>(ph, supplier, isParallel);
    }

    @Override
    @SuppressWarnings("unchecked")
    final Spliterator.OfDouble<X_OUT> lazySpliterator(Supplier<? extends Spliterator<Double, X_OUT>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator.OfDouble<>((Supplier<Spliterator.OfDouble<X_OUT>>) supplier);
    }

    @Override
    final boolean forEachWithCancel(Spliterator<Double> spliterator, Sink<Double> sink) {
        Spliterator.OfDouble spl = adapt(spliterator);
        DoubleConsumer adaptedSink = adapt(sink);
        boolean cancelled;
        do { } while (!(cancelled = sink.cancellationRequested()) && spl.tryAdvance(adaptedSink));
        return cancelled;
    }

    @Override
    final  Node.Builder<Double> makeNodeBuilder(long exactSizeIfKnown, IntFunction<Double[]> generator) {
        return Nodes.doubleBuilder(exactSizeIfKnown);
    }

    private <U> Stream<U, X_OUT> mapToObj(DoubleFunction<? extends U> mapper, int opFlags) {
        return new ReferencePipeline.StatelessOp<Double, X_OUT, U, X_OUT, RuntimeException>(this, StreamShape.DOUBLE_VALUE, opFlags) {
            @Override
            Sink<Double> opWrapSink(int flags, Sink<U> sink) {
                return new Sink.ChainedDouble<>(sink) {
                    @Override
                    public void accept(double t) {
                        downstream.accept(mapper.apply(t));
                    }
                };
            }
        };
    }

    // DoubleStream

    @Override
    public final PrimitiveIterator.OfDouble<X_OUT> iterator() {
        return Spliterators.iterator(spliterator());
    }

    @Override
    public final Spliterator.OfDouble<X_OUT> spliterator() {
        return adapt(super.spliterator());
    }

    // Stateless intermediate ops from DoubleStream

    @Override
    public final Stream<Double, X_OUT> boxed() {
        return mapToObj(Double::valueOf, 0);
    }

    @Override
    public final DoubleStream<X_OUT> map(DoubleUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<>(this, StreamShape.DOUBLE_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<>(sink) {
                    @Override
                    public void accept(double t) {
                        downstream.accept(mapper.applyAsDouble(t));
                    }
                };
            }
        };
    }

    @Override
    public final <U> Stream<U, X_OUT> mapToObj(DoubleFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return mapToObj(mapper, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT);
    }

    @Override
    public final IntStream<X_OUT> mapToInt(DoubleToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline.StatelessOp<>(this, StreamShape.DOUBLE_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedDouble<Integer>(sink) {
                    @Override
                    public void accept(double t) {
                        downstream.accept(mapper.applyAsInt(t));
                    }
                };
            }
        };
    }

    @Override
    public final LongStream<X_OUT> mapToLong(DoubleToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline.StatelessOp<>(this, StreamShape.DOUBLE_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedDouble<Long>(sink) {
                    @Override
                    public void accept(double t) {
                        downstream.accept(mapper.applyAsLong(t));
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream<X_OUT> flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<>(this, StreamShape.DOUBLE_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                final DoubleConsumer fastPath =
                        isShortCircuitingPipeline()
                                ? null
                                : (sink instanceof DoubleConsumer dc)
                                ? dc
                                : sink::accept;
                final class FlatMap implements Sink.OfDouble, DoublePredicate {
                    boolean cancel;

                    @Override public void begin(long size) { sink.begin(-1); }
                    @Override public void end() { 
                        sink.end();
                    }

                    @Override
                    public void accept(double e) {
                        try (DoubleStream result = mapper.apply(e)) {
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
                    public boolean test(double output) {
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
    public final DoubleStream<X_OUT> mapMulti(DoubleMapMultiConsumer mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<>(this, StreamShape.DOUBLE_VALUE,
                StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {

            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<>(sink) {

                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void accept(double t) {
                            mapper.accept(t, (DoubleConsumer) downstream);
                    }
                };
            }
        };
    }

    @Override
    public DoubleStream<X_OUT> unordered() {
        if (!isOrdered())
            return this;
        return new StatelessOp<>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_ORDERED) {
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return sink;
            }
        };
    }

    @Override
    public final DoubleStream<X_OUT> filter(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return new StatelessOp<>(this, StreamShape.DOUBLE_VALUE,
                StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<>(sink) {
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(double t) {
                        if (predicate.test(t))
                            downstream.accept(t);
                    }
                };
            }
        };
    }

    @Override
    public final DoubleStream<X_OUT> peek(DoubleConsumer action) {
        Objects.requireNonNull(action);
        return new StatelessOp<>(this, StreamShape.DOUBLE_VALUE,
                0) {
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<>(sink) {
                    @Override
                    public void accept(double t) {
                        action.accept(t);
                        downstream.accept(t);
                    }
                };
            }
        };
    }

    // Stateful intermediate ops from DoubleStream

    @Override
    public final DoubleStream<X_OUT> limit(long maxSize) {
        if (maxSize < 0)
            throw new IllegalArgumentException(Long.toString(maxSize));
        return SliceOps.makeDouble(this, 0L, maxSize);
    }

    @Override
    public final DoubleStream<X_OUT> skip(long n) {
        if (n < 0)
            throw new IllegalArgumentException(Long.toString(n));
        if (n == 0)
            return this;
        else {
            long limit = -1;
            return SliceOps.makeDouble(this, n, limit);
        }
    }

    @Override
    public final DoubleStream<X_OUT> takeWhile(DoublePredicate predicate) {
        return WhileOps.makeTakeWhileDouble(this, predicate);
    }

    @Override
    public final DoubleStream<X_OUT> dropWhile(DoublePredicate predicate) {
        return WhileOps.makeDropWhileDouble(this, predicate);
    }

    @Override
    public final DoubleStream<X_OUT> sorted() {
        return SortedOps.makeDouble(this);
    }

    @Override
    public final DoubleStream<X_OUT> distinct() {
        // While functional and quick to implement, this approach is not very efficient.
        // An efficient version requires a double-specific map/set implementation.
        return boxed().distinct().mapToDouble(i -> i);
    }

    // Terminal ops from DoubleStream

    @Override
    public void forEach(DoubleConsumer consumer)  throws X_OUT {
        evaluate(ForEachOps.makeDouble(consumer, false));
    }

    @Override
    public void forEachOrdered(DoubleConsumer consumer) throws X_OUT {
        evaluate(ForEachOps.makeDouble(consumer, true));
    }

    @Override
    public final double sum() throws X_OUT {
        /*
         * In the arrays allocated for the collect operation, index 0
         * holds the high-order bits of the running sum, index 1 holds
         * the negated low-order bits of the sum computed via compensated
         * summation, and index 2 holds the simple sum used to compute
         * the proper result if the stream contains infinite values of
         * the same sign.
         */
        double[] summation = collect(() -> new double[3],
                               (ll, d) -> {
                                   Collectors.sumWithCompensation(ll, d);
                                   ll[2] += d;
                               },
                               (ll, rr) -> {
                                   Collectors.sumWithCompensation(ll, rr[0]);
                                   // Subtract compensation bits
                                   Collectors.sumWithCompensation(ll, -rr[1]);
                                   ll[2] += rr[2];
                               });

        return Collectors.computeFinalSum(summation);
    }

    @Override
    public final OptionalDouble min() throws X_OUT {
        return reduce(Math::min);
    }

    @Override
    public final OptionalDouble max() throws X_OUT {
        return reduce(Math::max);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote The {@code double} format can represent all
     * consecutive integers in the range -2<sup>53</sup> to
     * 2<sup>53</sup>. If the pipeline has more than 2<sup>53</sup>
     * values, the divisor in the average computation will saturate at
     * 2<sup>53</sup>, leading to additional numerical errors.
     */
    @Override
    public final OptionalDouble average() throws X_OUT {
        /*
         * In the arrays allocated for the collect operation, index 0
         * holds the high-order bits of the running sum, index 1 holds
         * the low-order bits of the sum computed via compensated
         * summation, index 2 holds the number of values seen, index 3
         * holds the simple sum.
         */
        double[] avg = collect(() -> new double[4],
                               (ll, d) -> {
                                   ll[2]++;
                                   Collectors.sumWithCompensation(ll, d);
                                   ll[3] += d;
                               },
                               (ll, rr) -> {
                                   Collectors.sumWithCompensation(ll, rr[0]);
                                   // Subtract compensation bits
                                   Collectors.sumWithCompensation(ll, -rr[1]);
                                   ll[2] += rr[2];
                                   ll[3] += rr[3];
                               });
        return avg[2] > 0
            ? OptionalDouble.of(Collectors.computeFinalSum(avg) / avg[2])
            : OptionalDouble.empty();
    }

    @Override
    public final long count() throws X_OUT {
        return evaluate(ReduceOps.makeDoubleCounting());
    }

    @Override
    public final DoubleSummaryStatistics summaryStatistics() throws X_OUT {
        return collect(DoubleSummaryStatistics::new, DoubleSummaryStatistics::accept,
                       DoubleSummaryStatistics::combine);
    }

    @Override
    public final double reduce(double identity, DoubleBinaryOperator op) throws X_OUT {
        return evaluate(ReduceOps.makeDouble(identity, op));
    }

    @Override
    public final OptionalDouble reduce(DoubleBinaryOperator op) throws X_OUT {
        return evaluate(ReduceOps.makeDouble(op));
    }

    @Override
    public final <R> R collect(Supplier<R> supplier,
                               ObjDoubleConsumer<R> accumulator,
                               BiConsumer<R, R> combiner) throws X_OUT {
        Objects.requireNonNull(combiner);
        BinaryOperator<R> operator = (left, right) -> {
            combiner.accept(left, right);
            return left;
        };
        return evaluate(ReduceOps.makeDouble(supplier, accumulator, operator));
    }

    @Override
    public final boolean anyMatch(DoublePredicate predicate) throws X_OUT {
        return evaluate(MatchOps.makeDouble(predicate, MatchOps.MatchKind.ANY));
    }

    @Override
    public final boolean allMatch(DoublePredicate predicate) throws X_OUT {
        return evaluate(MatchOps.makeDouble(predicate, MatchOps.MatchKind.ALL));
    }

    @Override
    public final boolean noneMatch(DoublePredicate predicate) throws X_OUT {
        return evaluate(MatchOps.makeDouble(predicate, MatchOps.MatchKind.NONE));
    }

    @Override
    public final OptionalDouble findFirst()  throws X_OUT {
        return evaluate(FindOps.makeDouble(true));
    }

    @Override
    public final OptionalDouble findAny() throws X_OUT {
        return evaluate(FindOps.makeDouble(false));
    }

    @Override
    public final double[] toArray() throws X_OUT {
        return Nodes.flattenDouble((Node.OfDouble) evaluateToArrayNode(Double[]::new))
                        .asPrimitiveArray();
    }

    //

    /**
     * Source stage of a DoubleStream
     *
     * @param <E_IN> type of elements in the upstream source
     */
    static class Head<E_IN, throws X_OUT> extends DoublePipeline<E_IN, X_OUT, X_OUT> {
        /**
         * Constructor for the source stage of a DoubleStream.
         *
         * @param source {@code Supplier<Spliterator>} describing the stream
         *               source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel {@code true} if the pipeline is parallel
         */
        Head(Supplier<? extends Spliterator<Double, X_OUT>> source,
             int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }

        /**
         * Constructor for the source stage of a DoubleStream.
         *
         * @param source {@code Spliterator} describing the stream source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel {@code true} if the pipeline is parallel
         */
        Head(Spliterator<Double, X_OUT> source,
             int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }

        @Override
        final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        @Override
        final Sink<E_IN> opWrapSink(int flags, Sink<Double> sink) {
            throw new UnsupportedOperationException();
        }

        // Optimized sequential terminal operations for the head of the pipeline

        @Override
        public void forEach(DoubleConsumer consumer) throws X_OUT {
            if (!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(consumer);
            }
            else {
                super.forEach(consumer);
            }
        }

        @Override
        public void forEachOrdered(DoubleConsumer consumer) throws X_OUT {
            if (!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(consumer);
            }
            else {
                super.forEachOrdered(consumer);
            }
        }

    }

    /**
     * Base class for a stateless intermediate stage of a DoubleStream.
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    abstract static class StatelessOp<E_IN, throws X_IN, throws X_OUT extends X_IN|X, throws X>
        extends DoublePipeline<E_IN, X_IN, X_OUT, X> {
        /**
         * Construct a new DoubleStream by appending a stateless intermediate
         * operation to an existing stream.
         *
         * @param upstream the upstream pipeline stage
         * @param inputShape the stream shape for the upstream pipeline stage
         * @param opFlags operation flags for the new stage
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
     * Base class for a stateful intermediate stage of a DoubleStream.
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    abstract static class StatefulOp<E_IN, throws X_IN, throws X_OUT extends X_IN|X, throws X>
        extends DoublePipeline<E_IN, X_IN, X_OUT, X> {
        /**
         * Construct a new DoubleStream by appending a stateful intermediate
         * operation to an existing stream.
         *
         * @param upstream the upstream pipeline stage
         * @param inputShape the stream shape for the upstream pipeline stage
         * @param opFlags operation flags for the new stage
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
        abstract <P_IN, throws XX extends X_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double, XX, X> helper,
                                                        Spliterator<P_IN, XX> spliterator,
                                                        IntFunction<Double[]> generator) throws X_OUT;
    }
}
