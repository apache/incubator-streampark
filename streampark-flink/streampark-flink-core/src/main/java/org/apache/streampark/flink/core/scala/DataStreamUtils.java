/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.core.scala;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.runtime.operators.util.AssignerWithPeriodicWatermarksAdapter;
import org.apache.flink.streaming.runtime.operators.util.AssignerWithPunctuatedWatermarksAdapter;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/** Static helpers for {@link DataStream} extensions (formerly Scala implicit enrichments). */
public final class DataStreamUtils {

    private DataStreamUtils() {
    }

    public static <T> DataStream<T> sideOut(
                                            DataStream<T> dataStream,
                                            BiConsumer<T, ProcessFunction<T, T>.Context> fun) {
        return dataStream.process(
            new ProcessFunction<T, T>() {

                @Override
                public void processElement(
                                           T value, Context ctx, Collector<T> out) {
                    fun.accept(value, ctx);
                    out.collect(value);
                }
            });
    }

    public static <T, R> DataStream<R> sideGet(
                                               SingleOutputStreamOperator<T> dataStream, String sideTag) {
        return dataStream.getSideOutput(new OutputTag<R>(sideTag) {
        });
    }

    public static <T, R> DataStream<R> sideGet(
                                               SingleOutputStreamOperator<T> dataStream, OutputTag<R> outputTag) {
        return dataStream.getSideOutput(outputTag);
    }

    public static <T> DataStream<T> boundedOutOfOrdernessWatermark(
                                                                   DataStream<T> dataStream,
                                                                   ToLongFunction<T> timestampExtractor,
                                                                   Duration duration) {
        return dataStream.assignTimestampsAndWatermarks(
            WatermarkStrategy.<T>forBoundedOutOfOrderness(duration)
                .withTimestampAssigner(
                    (SerializableTimestampAssigner<T>) (element, recordTimestamp) -> timestampExtractor
                        .applyAsLong(element)));
    }

    public static <T> DataStream<T> timeLagWatermark(
                                                     DataStream<T> dataStream, ToLongFunction<T> timestampExtractor,
                                                     Time maxTimeLag) {
        AssignerWithPeriodicWatermarks<T> assigner =
            new AssignerWithPeriodicWatermarks<T>() {

                @Override
                public long extractTimestamp(T element, long previousElementTimestamp) {
                    return timestampExtractor.applyAsLong(element);
                }

                @Override
                public Watermark getCurrentWatermark() {
                    return new Watermark(
                        System.currentTimeMillis() - maxTimeLag.toMilliseconds());
                }
            };
        return dataStream.assignTimestampsAndWatermarks(
            WatermarkStrategy.forGenerator(
                new AssignerWithPeriodicWatermarksAdapter.Strategy<>(assigner)));
    }

    public static <T> DataStream<T> punctuatedWatermark(
                                                        DataStream<T> dataStream,
                                                        ToLongFunction<T> extractTimeFun,
                                                        Predicate<T> checkFunc) {
        AssignerWithPunctuatedWatermarks<T> assigner =
            new AssignerWithPunctuatedWatermarks<T>() {

                @Override
                public long extractTimestamp(T element, long previousElementTimestamp) {
                    return extractTimeFun.applyAsLong(element);
                }

                @Override
                public Watermark checkAndGetNextWatermark(
                                                          T lastElement, long extractedTimestamp) {
                    if (checkFunc.test(lastElement)) {
                        return new Watermark(extractedTimestamp);
                    }
                    return null;
                }
            };
        return dataStream.assignTimestampsAndWatermarks(
            WatermarkStrategy.forGenerator(
                new AssignerWithPunctuatedWatermarksAdapter.Strategy<>(assigner)));
    }

    public static <T, R> DataStream<R> proc(
                                            DataStream<T> dataStream,
                                            ProcessFunction<T, R> processFunction) {
        return dataStream.process(processFunction);
    }

    public static <T, R> DataStream<R> proc(
                                            DataStream<T> dataStream,
                                            TriConsumer<T, ProcessFunction<T, R>.Context, Collector<R>> processFunction,
                                            OnTimerConsumer<T, R> onTimerFunction) {
        return dataStream.process(
            new ProcessFunction<T, R>() {

                @Override
                public void processElement(
                                           T value, Context ctx, Collector<R> out) {
                    processFunction.accept(value, ctx, out);
                }

                @Override
                public void onTimer(long timestamp, OnTimerContext ctx, Collector<R> out) throws Exception {
                    if (onTimerFunction != null) {
                        onTimerFunction.accept(timestamp, ctx, out);
                    } else {
                        super.onTimer(timestamp, ctx, out);
                    }
                }
            });
    }

    public static <T, R> DataStream<R> proc(
                                            DataStream<T> dataStream,
                                            TriConsumer<T, ProcessFunction<T, R>.Context, Collector<R>> processFunction) {
        return proc(dataStream, processFunction, null);
    }

    public static <IN, OUT, R> void sideOut(
                                            ProcessFunction<IN, OUT>.Context ctx, String outputTag, R value) {
        ctx.output(new OutputTag<R>(outputTag) {
        }, value);
    }

    /** Three-argument consumer for process element callbacks. */
    @FunctionalInterface
    public interface TriConsumer<T, U, V> {

        void accept(T t, U u, V v);
    }

    /** On-timer callback for {@link #proc}. */
    @FunctionalInterface
    public interface OnTimerConsumer<T, R> {

        void accept(
                    long timestamp,
                    ProcessFunction<T, R>.OnTimerContext ctx,
                    Collector<R> out);
    }
}
