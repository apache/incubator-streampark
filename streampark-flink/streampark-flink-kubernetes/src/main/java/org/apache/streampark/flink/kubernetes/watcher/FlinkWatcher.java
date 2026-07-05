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

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FlinkWatcher implements AutoCloseable {

    private final AtomicBoolean started = new AtomicBoolean(false);

    private static final int CPU_NUM = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

    protected final ScheduledThreadPoolExecutor watchExecutor = new ScheduledThreadPoolExecutor(CPU_NUM);

    public synchronized void start() {
        if (!started.getAndSet(true)) {
            doStart();
        }
    }

    public synchronized void stop() {
        if (started.getAndSet(false)) {
            doStop();
        }
    }

    @Override
    public synchronized void close() {
        if (started.get()) {
            doStop();
        }
        doClose();
        watchExecutor.shutdownNow();
    }

    public synchronized void restart() {
        stop();
        start();
    }

    protected abstract void doStart();

    protected abstract void doStop();

    protected abstract void doClose();

    public abstract void doWatch();

    protected Runnable toRunnable(Runnable fun) {
        return fun;
    }
}
