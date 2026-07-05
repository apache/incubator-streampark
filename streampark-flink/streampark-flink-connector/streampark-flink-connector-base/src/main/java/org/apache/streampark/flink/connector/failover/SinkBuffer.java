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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Local buffer that flushes sink records based on size or time threshold. */
public class SinkBuffer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SinkBuffer.class);

    private final SinkWriter writer;
    private final long flushInterval;
    private final int bufferSize;

    private long timestamp = 0L;
    private final CopyOnWriteArrayList<String> localValues = new CopyOnWriteArrayList<>();

    public SinkBuffer(SinkWriter writer, long flushInterval, int bufferSize) {
        this.writer = writer;
        this.flushInterval = flushInterval;
        this.bufferSize = bufferSize;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void put(String value) {
        tryAddToQueue();
        localValues.add(value);
        timestamp = System.currentTimeMillis();
    }

    public void tryAddToQueue() {
        synchronized (this) {
            if (shouldFlush()) {
                addToQueue();
            }
        }
    }

    private void addToQueue() {
        List<String> deepCopy = buildDeepCopy(localValues);
        SinkRequest params = new SinkRequest(deepCopy);
        LOG.debug("Build blank with params: buffer size = {}", params.size());
        writer.write(params);
        localValues.clear();
    }

    private boolean shouldFlush() {
        if (localValues.isEmpty()) {
            return false;
        }
        if (localValues.size() >= bufferSize) {
            return true;
        }
        if (timestamp == 0) {
            return false;
        }
        return System.currentTimeMillis() - timestamp > flushInterval;
    }

    private List<String> buildDeepCopy(List<String> original) {
        return Collections.unmodifiableList(new ArrayList<>(original));
    }

    @Override
    public void close() {
        if (!localValues.isEmpty()) {
            addToQueue();
        }
    }
}
