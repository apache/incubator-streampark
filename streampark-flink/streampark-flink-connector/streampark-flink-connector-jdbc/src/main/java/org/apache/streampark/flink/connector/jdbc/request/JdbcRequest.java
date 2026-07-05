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

package org.apache.streampark.flink.connector.jdbc.request;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.function.TransformFunction;
import org.apache.streampark.flink.connector.jdbc.internal.JdbcASyncFunction;

import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** JDBC async request helper. */
public class JdbcRequest<T> {

    private final DataStream<T> stream;
    private final Properties property;

    public JdbcRequest(DataStream<T> stream, Properties property) {
        this.stream = stream;
        this.property = property != null ? property : new Properties();
    }

    public static <T> JdbcRequest<T> of(DataStream<T> stream, Properties property) {
        return new JdbcRequest<>(stream, property);
    }

    public <R> SingleOutputStreamOperator<R> requestOrdered(
            TransformFunction<T, String> sqlFun,
            JdbcASyncFunction.JdbcResultFunction<T, R> resultFun,
            long timeout,
            int capacity,
            Properties jdbc) {
        Utils.copyProperties(property, jdbc);
        JdbcASyncFunction<T, R> async = new JdbcASyncFunction<>(sqlFun, resultFun, jdbc, capacity);
        return AsyncDataStream.orderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity);
    }

    public <R> SingleOutputStreamOperator<R> requestUnordered(
            TransformFunction<T, String> sqlFun,
            JdbcASyncFunction.JdbcResultFunction<T, R> resultFun,
            long timeout,
            int capacity,
            Properties jdbc) {
        Utils.copyProperties(property, jdbc);
        JdbcASyncFunction<T, R> async = new JdbcASyncFunction<>(sqlFun, resultFun, jdbc, capacity);
        return AsyncDataStream.unorderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity);
    }
}
