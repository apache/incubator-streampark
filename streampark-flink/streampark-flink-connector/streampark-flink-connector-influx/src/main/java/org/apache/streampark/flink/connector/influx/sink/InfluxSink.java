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

package org.apache.streampark.flink.connector.influx.sink;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.ConfigUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.influx.bean.InfluxEntity;
import org.apache.streampark.flink.connector.influx.function.InfluxFunction;
import org.apache.streampark.flink.connector.sink.Sink;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;

import java.util.Properties;

/** InfluxDB sink connector. */
public class InfluxSink implements Sink {

    private final StreamingContext ctx;
    private final Properties property;
    private final int parallelism;
    private final String name;
    private final String uid;

    public InfluxSink(
            StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
        this.parallelism = parallelism;
        this.name = name;
        this.uid = uid;
    }

    public static InfluxSink of(
            StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        return new InfluxSink(ctx, property, parallelism, name, uid);
    }

    public <T> DataStreamSink<T> sink(DataStream<T> stream, String alias, InfluxEntity<T> entity) {
        String influxPrefix = ConfigKeys.INFLUX_PREFIX() + (alias != null ? alias : "");
        Properties prop = ConfigUtils.getConf(ctx.parameter.toMap(), influxPrefix, "", "");
        Utils.copyProperties(property, prop);
        InfluxFunction<T> sinkFun = new InfluxFunction<>(prop, entity);
        DataStreamSink<T> sink = stream.addSink(sinkFun);
        return afterSink(sink, parallelism, name, uid);
    }
}
