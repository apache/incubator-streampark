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

package org.apache.streampark.flink.connector.hbase.sink;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.ConfigUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.function.TransformFunction;
import org.apache.streampark.flink.connector.hbase.internal.HBaseSinkFunction;
import org.apache.streampark.flink.connector.sink.Sink;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.hadoop.hbase.client.Mutation;

import java.util.Properties;

/** HBase sink connector. */
public class HBaseSink implements Sink {

    private final StreamingContext ctx;
    private final Properties property;
    private final int parallelism;
    private final String name;
    private final String uid;
    private final String alias;

    public HBaseSink(
            StreamingContext ctx, Properties property, int parallelism, String name, String uid, String alias) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
        this.parallelism = parallelism;
        this.name = name;
        this.uid = uid;
        this.alias = alias != null ? alias : "";
    }

    public static HBaseSink of(
            StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        return new HBaseSink(ctx, property, parallelism, name, uid, "");
    }

    public <T> DataStreamSink<T> sink(
            DataStream<T> stream, String tableName, TransformFunction<T, Iterable<Mutation>> fun) {
        Properties prop = checkProp(stream, tableName, fun);
        HBaseSinkFunction<T> sinkFun = new HBaseSinkFunction<>(tableName, prop, fun);
        return afterSink(stream.addSink(sinkFun), parallelism, name, uid);
    }

    private Properties checkProp(Object stream, String tableName, Object fun) {
        Properties prop =
                ConfigUtils.getConf(
                        ctx.parameter.toMap(), ConfigKeys.HBASE_PREFIX, ConfigKeys.HBASE_PREFIX, alias);
        Utils.copyProperties(property, prop);
        if (stream == null) {
            throw new IllegalArgumentException("Sink Stream must not null");
        }
        if (tableName == null) {
            throw new IllegalArgumentException("Sink tableName must not null");
        }
        if (fun == null) {
            throw new IllegalArgumentException("Func must not null");
        }
        return prop;
    }
}
