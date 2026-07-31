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

package org.apache.streampark.console.core.managed.provider.volcengine;

import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/** Per-account concurrency and request-spacing guard for Volcengine OpenAPI calls. */
class VolcengineApiRateLimiter {

    private final int maxConcurrentRequests;

    private final long minIntervalNanos;

    private final ConcurrentMap<Long, Semaphore> concurrency = new ConcurrentHashMap<>();

    private final ConcurrentMap<Long, AtomicLong> nextRequestNanos = new ConcurrentHashMap<>();

    VolcengineApiRateLimiter(int maxConcurrentRequests, long minRequestIntervalMs) {
        if (maxConcurrentRequests <= 0 || minRequestIntervalMs < 0) {
            throw new IllegalArgumentException("Invalid Volcengine rate limit configuration");
        }
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.minIntervalNanos = minRequestIntervalMs * 1_000_000L;
    }

    Permit acquire(Long accountId) {
        Semaphore semaphore =
            concurrency.computeIfAbsent(accountId, ignored -> new Semaphore(maxConcurrentRequests));
        try {
            semaphore.acquire();
            awaitRequestSlot(accountId);
            return semaphore::release;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.TRANSIENT,
                "RequestInterrupted",
                null,
                "Volcengine Flink request was interrupted.");
        }
    }

    private void awaitRequestSlot(Long accountId) {
        if (minIntervalNanos == 0) {
            return;
        }
        AtomicLong next =
            nextRequestNanos.computeIfAbsent(accountId, ignored -> new AtomicLong());
        while (true) {
            long now = System.nanoTime();
            long current = next.get();
            long reserved = Math.max(now, current);
            if (next.compareAndSet(current, reserved + minIntervalNanos)) {
                long waitNanos = reserved - now;
                if (waitNanos > 0) {
                    LockSupport.parkNanos(waitNanos);
                }
                return;
            }
        }
    }

    interface Permit extends AutoCloseable {

        @Override
        void close();
    }
}
