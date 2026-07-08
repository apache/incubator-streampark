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

package org.apache.streampark.common.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CompletableFutureUtils {

    private CompletableFutureUtils() {
    }

    private static class DaemonThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("CompletableFutureDelayScheduler");
            return t;
        }
    }

    private static final ScheduledThreadPoolExecutor COMPLETABLE_DELAYER;

    static {
        COMPLETABLE_DELAYER = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory());
        COMPLETABLE_DELAYER.setRemoveOnCancelPolicy(true);
    }

    private static <T> CompletableFuture<T> setTimeout(long timeout, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<>();
        COMPLETABLE_DELAYER.schedule(
            (Callable<Boolean>) () -> {
                result.completeExceptionally(new TimeoutException());
                return true;
            },
            timeout,
            unit);
        return result;
    }

    public static <T> CompletableFuture<T> supplyTimeout(
                                                         CompletableFuture<T> future,
                                                         long timeout,
                                                         TimeUnit unit,
                                                         Function<T, T> handle,
                                                         Function<Throwable, T> exceptionally) {
        return future.applyToEither(setTimeout(timeout, unit), handle)
            .exceptionally(exceptionally)
            .whenComplete(
                (BiConsumer<T, Throwable>) (t, u) -> {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                });
    }

    public static <T> CompletableFuture<Void> runTimeout(
                                                         CompletableFuture<T> future, long timeout, TimeUnit unit) {
        return runTimeout(future, timeout, unit, null, null);
    }

    public static <T> CompletableFuture<Void> runTimeout(
                                                         CompletableFuture<T> future,
                                                         long timeout,
                                                         TimeUnit unit,
                                                         Consumer<T> handle,
                                                         Consumer<Throwable> exceptionally) {
        return future.applyToEither(
            setTimeout(timeout, unit),
            (Function<T, Void>) t -> {
                if (handle != null) {
                    handle.accept(t);
                }
                return null;
            })
            .exceptionally(
                (Function<Throwable, Void>) t -> {
                    if (exceptionally != null) {
                        exceptionally.accept(t);
                    }
                    return null;
                })
            .whenComplete(
                (BiConsumer<Void, Throwable>) (t, u) -> {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                });
    }
}
