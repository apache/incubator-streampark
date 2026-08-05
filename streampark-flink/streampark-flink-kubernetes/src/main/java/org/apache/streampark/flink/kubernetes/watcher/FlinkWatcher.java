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

package org.apache.streampark.flink.kubernetes.watcher;

import org.apache.streampark.common.util.LoggerSupport;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FlinkWatcher extends LoggerSupport implements AutoCloseable {

    private final AtomicBoolean started = new AtomicBoolean(false);

    private static final int CPU_NUM = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

    protected final ScheduledThreadPoolExecutor watchExecutor = new ScheduledThreadPoolExecutor(CPU_NUM);

    /**
     * Start watcher process. This method should be a thread-safe implementation of light locking and
     * can be called idempotent.
     */
    public final void start() {
        synchronized (this) {
            if (!started.getAndSet(true)) {
                doStart();
            }
        }
    }

    /**
     * Stop watcher process. This method should be a thread-safe implementation of light locking and
     * can be called idempotent.
     */
    public final void stop() {
        synchronized (this) {
            if (started.getAndSet(false)) {
                doStop();
            }
        }
    }

    @Override
    public final void close() {
        synchronized (this) {
            if (started.get()) {
                doStop();
            }
            doClose();
            watchExecutor.shutdownNow();
        }
    }

    /**
     * This method should be a thread-safe implementation of light locking and can be called
     * idempotent.
     */
    public final void restart() {
        synchronized (this) {
            stop();
            start();
        }
    }

    protected abstract void doStart();

    protected abstract void doStop();

    protected abstract void doClose();

    public abstract void doWatch();
}
