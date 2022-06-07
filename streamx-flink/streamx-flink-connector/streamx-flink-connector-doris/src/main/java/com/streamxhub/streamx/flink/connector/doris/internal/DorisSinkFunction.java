/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.connector.doris.internal;

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.util.ConfigUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.flink.connector.doris.bean.DorisStreamLoad;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.util.concurrent.ExecutorThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * DorisSinkFunction
 **/
public class DorisSinkFunction<T> extends RichSinkFunction<T> implements CheckpointedFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DorisSinkFunction.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String database;
    private final String table;
    private final String user;
    private final String password;
    private final Properties streamLoadProp;
    private final List<Object> batch = new ArrayList<>();
    private String fenodes;
    private Integer batchSize;
    private long intervalMs;
    private Integer maxRetries;
    private DorisStreamLoad dorisStreamLoad;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private transient volatile Exception flushException;
    private transient volatile boolean closed = false;

    public DorisSinkFunction(StreamingContext context, Properties props, String alias) {
        Properties config = ConfigUtils.getConf(context.parameter().toMap(), ConfigConst.DORIS_SINK_PREFIX(), "", alias);
        Utils.copyProperties(props, config);
        this.fenodes = config.getProperty(ConfigConst.DORIS_FENODES());
        this.database = config.getProperty(ConfigConst.DORIS_DATABASE());
        this.table = config.getProperty(ConfigConst.DORIS_TABLE());
        this.user = config.getProperty(ConfigConst.DORIS_USER());
        this.password = config.getProperty(ConfigConst.DORIS_PASSWORD());
        this.batchSize = Integer.valueOf(config.getProperty(ConfigConst.DORIS_BATCHSIZE(), ConfigConst.DORIS_DEFAULT_BATCHSIZE()));
        this.intervalMs = Long.parseLong(config.getProperty(ConfigConst.DORIS_INTERVALMS(), ConfigConst.DORIS_DEFAULT_INTERVALMS()));
        this.maxRetries = Integer.valueOf(config.getProperty(ConfigConst.DORIS_MAXRETRIES(), ConfigConst.DORIS_DEFAULT_MAXRETRIES()));
        this.streamLoadProp = parseStreamLoadProps(config, ConfigConst.DORIS_STREAM_LOAD_PROP_PREFIX());
        this.dorisStreamLoad = new DorisStreamLoad(fenodes, database, table, user, password, streamLoadProp);
    }

    public Properties parseStreamLoadProps(Properties properties, String prefix) {
        Properties result = new Properties();
        properties.forEach((k, v) -> {
            String key = k.toString();
            if (key.startsWith(prefix)) {
                result.put(key.substring(prefix.length()), v);
            }
        });
        return result;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        if (intervalMs > 0 && batchSize != 1) {
            this.scheduler = Executors.newScheduledThreadPool(1, new ExecutorThreadFactory("doris-streamload-output-format"));
            this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(() -> {
                synchronized (DorisSinkFunction.this) {
                    if (!closed) {
                        try {
                            flush();
                        } catch (Exception e) {
                            flushException = e;
                        }
                    }
                }
            }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void invoke(T value, SinkFunction.Context context) throws Exception {
        checkFlushException();
        addBatch(value);
        if (batchSize > 0 && batch.size() >= batchSize) {
            flush();
        }
    }

    private void addBatch(T row) {
        if (row instanceof String) {
            batch.add(row);
        } else {
            throw new RuntimeException("unSupport type " + row.getClass());
        }
    }

    public synchronized void flush() throws IOException {
        checkFlushException();
        if (batch.isEmpty()) {
            return;
        }
        String result;
        if (batch.get(0) instanceof String) {
            result = batch.toString();
        } else {
            result = OBJECT_MAPPER.writeValueAsString(batch);
        }

        for (int i = 0; i <= maxRetries; i++) {
            try {
                dorisStreamLoad.load(result);
                batch.clear();
                break;
            } catch (Exception e) {
                LOGGER.error("doris sink error, retry times = {}", i, e);
                if (i >= maxRetries) {
                    throw new IOException(e);
                }
                try {
                    Thread.sleep(1000 * i);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("unable to flush; interrupted while doing another attempt", e);
                }
            }
        }
    }

    private void checkFlushException() {
        if (flushException != null) {
            throw new RuntimeException("Writing records to streamload failed.", flushException);
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
    }

    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
        flush();
    }

    @Override
    public void initializeState(FunctionInitializationContext functionInitializationContext) throws Exception {

    }
}
