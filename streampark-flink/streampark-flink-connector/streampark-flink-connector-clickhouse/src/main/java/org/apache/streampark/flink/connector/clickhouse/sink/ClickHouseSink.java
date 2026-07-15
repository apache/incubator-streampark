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

package org.apache.streampark.flink.connector.clickhouse.sink;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.clickhouse.internal.AsyncClickHouseSinkFunction;
import org.apache.streampark.flink.connector.clickhouse.internal.ClickHouseSinkFunction;
import org.apache.streampark.flink.connector.function.TransformFunction;
import org.apache.streampark.flink.connector.sink.Sink;
import org.apache.streampark.flink.core.scala.StreamingContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import java.util.Properties;
public class ClickHouseSink implements Sink {
    public static final String SINK_NULL_HINT_MSG = "Sink Stream must not null";
    private final StreamingContext ctx; private final Properties property;
    private final int parallelism; private final String name; private final String uid;
    private final Properties prop;
    public ClickHouseSink(StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        this.ctx = ctx; this.property = property != null ? property : new Properties();
        this.parallelism = parallelism; this.name = name; this.uid = uid;
        this.prop = Utils.toProperties(ctx.parameter.toMap());
        Utils.copyProperties(this.property, prop);
    }
    public static ClickHouseSink of(StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        return new ClickHouseSink(ctx, property, parallelism, name, uid);
    }
    public <T> DataStreamSink<T> asyncSink(DataStream<T> stream, TransformFunction<T, String> toSQLFn) {
        if (stream == null) throw new IllegalArgumentException(SINK_NULL_HINT_MSG);
        return afterSink(stream.addSink(new AsyncClickHouseSinkFunction<>(prop, toSQLFn)), parallelism, name, uid);
    }
    public <T> DataStreamSink<T> asyncSink(DataStream<T> stream) { return asyncSink(stream, null); }
    public <T> DataStreamSink<T> jdbcSink(DataStream<T> stream, TransformFunction<T, String> sqlFromFn) {
        if (stream == null) throw new IllegalArgumentException(SINK_NULL_HINT_MSG);
        return afterSink(stream.addSink(new ClickHouseSinkFunction<>(prop, sqlFromFn)), parallelism, name, uid);
    }
    public <T> DataStreamSink<T> jdbcSink(DataStream<T> stream) { return jdbcSink(stream, null); }
}
