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

package org.apache.streampark.flink.connector.hbase.request;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.hbase.bean.HBaseQuery;
import org.apache.streampark.flink.connector.hbase.function.HBaseQueryFunction;
import org.apache.streampark.flink.connector.hbase.function.HBaseResultFunction;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** HBase async request helper. */
public class HBaseRequest<T> {

    private final DataStream<T> stream;
    private final Properties property;

    public HBaseRequest(DataStream<T> stream, Properties property) {
        this.stream = stream;
        this.property = property != null ? property : new Properties();
    }

    public static <T> HBaseRequest<T> of(DataStream<T> stream, Properties property) {
        return new HBaseRequest<>(stream, property);
    }

    public <R> SingleOutputStreamOperator<R> requestOrdered(
            HBaseQueryFunction<T> queryFunc,
            HBaseAsyncResultFunction<T, R> resultFunc,
            long timeout,
            int capacity,
            Properties prop) {
        Utils.copyProperties(property, prop);
        HBaseAsyncFunction<T, R> async = new HBaseAsyncFunction<>(prop, queryFunc, resultFunc, capacity);
        return AsyncDataStream.orderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity);
    }

    public <R> SingleOutputStreamOperator<R> requestUnordered(
            HBaseQueryFunction<T> queryFunc,
            HBaseAsyncResultFunction<T, R> resultFunc,
            long timeout,
            int capacity,
            Properties prop) {
        Utils.copyProperties(property, prop);
        HBaseAsyncFunction<T, R> async = new HBaseAsyncFunction<>(prop, queryFunc, resultFunc, capacity);
        return AsyncDataStream.unorderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity);
    }

    @FunctionalInterface
    public interface HBaseAsyncResultFunction<T, R> extends java.io.Serializable {
        R apply(T input, Result result);
    }

    /** HBase async lookup function. */
    public static class HBaseAsyncFunction<T, R> extends RichAsyncFunction<T, R> {

        private static final Logger LOG = LoggerFactory.getLogger(HBaseAsyncFunction.class);

        private final Properties prop;
        private final HBaseQueryFunction<T> queryFunc;
        private final HBaseAsyncResultFunction<T, R> resultFunc;
        private final int capacity;
        private transient Table table;
        private transient ExecutorService executorService;

        public HBaseAsyncFunction(
                Properties prop,
                HBaseQueryFunction<T> queryFunc,
                HBaseAsyncResultFunction<T, R> resultFunc,
                int capacity) {
            this.prop = prop;
            this.queryFunc = queryFunc;
            this.resultFunc = resultFunc;
            this.capacity = capacity;
        }

        @Override
        public void open(Configuration parameters) {
            executorService = Executors.newFixedThreadPool(capacity);
        }

        @Override
        public void asyncInvoke(T input, ResultFuture<R> resultFuture) {
            CompletableFuture.supplyAsync(
                            () -> {
                                HBaseQuery query = queryFunc.query(input);
                                if (query == null || query.getTable() == null) {
                                    throw new IllegalArgumentException(
                                            "[StreamPark] HBaseRequest query and query's attr table must not be null ");
                                }
                                table = query.getTable(prop);
                                try {
                                    return table.getScanner(query);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            executorService)
                    .thenAccept(
                            scanner -> {
                                List<R> outputs = new ArrayList<>();
                                boolean hasResult = false;
                                for (Result result : scanner) {
                                    hasResult = true;
                                    outputs.add(resultFunc.apply(input, result));
                                }
                                if (!hasResult) {
                                    outputs.add(resultFunc.apply(input, Result.EMPTY_RESULT));
                                }
                                resultFuture.complete(outputs);
                            });
        }

        @Override
        public void timeout(T input, ResultFuture<R> resultFuture) {
            LOG.warn("HBaseASync request timeout. retrying... ");
            asyncInvoke(input, resultFuture);
        }

        @Override
        public void close() throws Exception {
            if (table != null) {
                table.close();
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
    }
}
