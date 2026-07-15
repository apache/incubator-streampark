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

package org.apache.streampark.flink.connector.failover;

import org.apache.streampark.common.util.ThreadUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Periodically flushes sink buffers that exceed time threshold. */
public class FailoverChecker implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FailoverChecker.class);

    private final List<SinkBuffer> sinkBuffers = new ArrayList<>();
    private final ScheduledExecutorService scheduledExecutorService;

    public FailoverChecker(long delayTime) {
        ThreadFactory factory = ThreadUtils.threadFactory("FailoverChecker");
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(factory);
        scheduledExecutorService.scheduleWithFixedDelay(
                this::checkBuffers, delayTime, delayTime, TimeUnit.MILLISECONDS);
        LOG.info("Build Sink scheduled checker, timeout (microSeconds) = {}", delayTime);
    }

    public void addSinkBuffer(SinkBuffer buffer) {
        synchronized (this) {
            sinkBuffers.add(buffer);
        }
        LOG.debug("Add SinkBuffer, size: {}", buffer.getBufferSize());
    }

    private void checkBuffers() {
        synchronized (this) {
            LOG.debug("Start checking buffers. Current count of buffers = {}", sinkBuffers.size());
            for (SinkBuffer buffer : sinkBuffers) {
                buffer.tryAddToQueue();
            }
        }
    }

    @Override
    public void close() {
        try {
            ThreadUtils.shutdownExecutorService(scheduledExecutorService);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
