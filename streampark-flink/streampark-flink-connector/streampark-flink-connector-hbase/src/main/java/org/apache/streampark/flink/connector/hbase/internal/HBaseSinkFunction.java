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

package org.apache.streampark.flink.connector.hbase.internal;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.HBaseClient;
import org.apache.streampark.flink.connector.function.TransformFunction;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.BufferedMutatorParams;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.client.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** HBase sink function with buffered mutations. */
public class HBaseSinkFunction<T> extends RichSinkFunction<T> {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseSinkFunction.class);

    private final String tabName;
    private final Properties prop;
    private final TransformFunction<T, Iterable<Mutation>> transformFunc;

    private Connection connection;
    private Table table;
    private BufferedMutator mutator;
    private final AtomicLong offset = new AtomicLong(0L);
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final int commitBatch;
    private final long writeBufferSize;
    private final List<Mutation> mutations = new ArrayList<>();
    private final List<Put> putArray = new ArrayList<>();
    private transient ScheduledExecutorService service;

    public HBaseSinkFunction(
            String tabName, Properties properties, TransformFunction<T, Iterable<Mutation>> transformFunc) {
        this.tabName = tabName;
        this.prop = properties;
        this.transformFunc = transformFunc;
        Object batch = prop.getOrDefault(ConfigKeys.KEY_HBASE_COMMIT_BATCH, String.valueOf(ConfigKeys.DEFAULT_HBASE_COMMIT_BATCH));
        this.commitBatch = Integer.parseInt(batch.toString());
        Object writeSize = prop.getOrDefault(ConfigKeys.KEY_HBASE_WRITE_SIZE, String.valueOf(ConfigKeys.DEFAULT_HBASE_WRITE_SIZE));
        this.writeBufferSize = Long.parseLong(writeSize.toString());
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        service = Executors.newSingleThreadScheduledExecutor();
        connection = HBaseClient.apply(prop).getConnection();
        TableName tableName = TableName.valueOf(tabName);
        BufferedMutator.ExceptionListener listener =
                (exception, mutator) -> {
                    for (int i = 0; i < exception.getNumExceptions(); i++) {
                        LOG.error(
                                "[StreamPark] HBaseSink Failed to sent put {},error:{}",
                                exception.getRow(i),
                                exception.getLocalizedMessage());
                    }
                };
        BufferedMutatorParams mutatorParam =
                new BufferedMutatorParams(tableName)
                        .writeBufferSize(writeBufferSize)
                        .listener(listener);
        mutator = connection.getBufferedMutator(mutatorParam);
        table = connection.getTable(tableName);
    }

    @Override
    public void invoke(T value, SinkFunction.Context context) throws Exception {
        Iterable<Mutation> list = transformFunc.transform(value);
        for (Mutation mutation : list) {
            if (mutation instanceof Put) {
                putArray.add((Put) mutation);
            } else {
                mutations.add(mutation);
            }
        }
        long count = offset.incrementAndGet();
        if (count % commitBatch == 0) {
            execBatch();
        } else if (!scheduled.get()) {
            scheduled.set(true);
            service.schedule(
                    () -> {
                        scheduled.set(false);
                        try {
                            execBatch();
                        } catch (Exception e) {
                            LOG.error("HBaseSink scheduled batch error", e);
                        }
                    },
                    10,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    public void close() throws Exception {
        execBatch();
        if (mutator != null) {
            mutator.flush();
            mutator.close();
        }
        if (table != null) {
            table.close();
        }
        if (service != null) {
            service.shutdown();
        }
    }

    private void execBatch() throws Exception {
        if (offset.get() > 0) {
            long start = System.currentTimeMillis();
            mutator.mutate(putArray);
            mutator.flush();
            putArray.clear();
            if (!mutations.isEmpty()) {
                Object[] results = new Object[mutations.size()];
                table.batch(mutations, results);
                LOG.info(
                        "HBaseSink batchSize:{} use {} MS",
                        mutations.size(),
                        System.currentTimeMillis() - start);
                mutations.clear();
            }
            offset.set(0L);
        }
    }
}
