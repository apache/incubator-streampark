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

package org.apache.streampark.console.base.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MoreFutures {
    private MoreFutures() {
    }

    public static <T> T await(CompletionStage<T> stage) {
        CompletableFuture<T> future = stage.toCompletableFuture();

        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public static <T> T deref(CompletionStage<T> stage, long timeout, TimeUnit unit) {
        CompletableFuture<T> future = stage.toCompletableFuture();

        try {
            return future.get(timeout, unit);
        } catch (TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public static <T> T derefUsingDefaultTimeout(CompletionStage<T> stage) {
        return deref(stage, 5L, TimeUnit.SECONDS);
    }

    public static <T> CompletionStage<T> exceptionallyCompose(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn) {
        return stage.thenApply(CompletableFuture::completedFuture).exceptionally((Function<Throwable, ? extends CompletableFuture<T>>) fn).thenCompose(Function.identity());
    }

    public static <T> CompletableFuture<T> completeImmediately(Supplier<T> supplier) {
        try {
            return CompletableFuture.completedFuture(supplier.get());
        } catch (Throwable e) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

    public static <T> CompletableFuture<T> completedExceptionally(Throwable cause) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(cause);
        return future;
    }

    public static <T> ScheduledFuture<T> completedScheduledFuture(T value) {
        return new CompletedScheduledFuture<>(value);
    }

    private static final class CompletedScheduledFuture<T> implements ScheduledFuture<T> {
        private final T value;

        private CompletedScheduledFuture(T value) {
            this.value = value;
        }

        public long getDelay(TimeUnit unit) {
            return 0L;
        }

        public int compareTo(Delayed o) {
            return Long.compare(0L, o.getDelay(TimeUnit.MILLISECONDS));
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return true;
        }

        public T get() throws InterruptedException, ExecutionException {
            return this.value;
        }

        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return this.value;
        }
    }
}
