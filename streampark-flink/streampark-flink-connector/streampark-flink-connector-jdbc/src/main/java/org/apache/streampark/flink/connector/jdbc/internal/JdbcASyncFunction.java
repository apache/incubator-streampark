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

package org.apache.streampark.flink.connector.jdbc.internal;

import org.apache.streampark.common.util.JdbcUtils;
import org.apache.streampark.flink.connector.function.TransformFunction;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** JDBC async lookup function based on a thread pool. */
public class JdbcASyncFunction<T, R> extends RichAsyncFunction<T, R> {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcASyncFunction.class);

    private final TransformFunction<T, String> sqlFun;
    private final JdbcResultFunction<T, R> resultFun;
    private final Properties jdbc;
    private final int capacity;
    private transient ExecutorService executorService;

    @FunctionalInterface
    public interface JdbcResultFunction<T, R> extends java.io.Serializable {
        R apply(T input, Map<String, Object> row);
    }

    public JdbcASyncFunction(
            TransformFunction<T, String> sqlFun,
            JdbcResultFunction<T, R> resultFun,
            Properties jdbc,
            int capacity) {
        this.sqlFun = sqlFun;
        this.resultFun = resultFun;
        this.jdbc = jdbc;
        this.capacity = capacity;
    }

    @Override
    public void open(Configuration parameters) {
        executorService = Executors.newFixedThreadPool(capacity);
    }

    @Override
    public void close() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public void asyncInvoke(T input, ResultFuture<R> resultFuture) {
        CompletableFuture.supplyAsync(() -> JdbcUtils.select(sqlFun.transform(input), jdbc), executorService)
                .thenAccept(
                        result -> {
                            List<R> outputs = new ArrayList<>();
                            if (result == null || !result.iterator().hasNext()) {
                                outputs.add(resultFun.apply(input, Collections.emptyMap()));
                            } else {
                                for (Map<String, ?> row : result) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> castRow = (Map<String, Object>) row;
                                    outputs.add(resultFun.apply(input, castRow));
                                }
                            }
                            resultFuture.complete(outputs);
                        });
    }

    @Override
    public void timeout(T input, ResultFuture<R> resultFuture) {
        LOG.warn("JdbcASync request timeout. retrying... ");
        asyncInvoke(input, resultFuture);
    }
}
