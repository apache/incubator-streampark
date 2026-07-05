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

package org.apache.streampark.flink.connector.elasticsearch5.sink;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.elasticsearch5.conf.ESConfig;
import org.apache.streampark.flink.connector.elasticsearch5.internal.ESSinkFunction;
import org.apache.streampark.flink.connector.function.TransformFunction;
import org.apache.streampark.flink.connector.sink.Sink;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch5.ElasticsearchSink;
import org.elasticsearch.action.ActionRequest;

import java.util.Map;
import java.util.Properties;

public class ES5Sink implements Sink {
    public static final String FUNCTION_NULL_HINT_MSG = "ES pocess element func must not null";
    public static final String SINK_NULL_HINT_MSG = "Sink Stream must not null";

    private final StreamingContext ctx;
    private final Properties property;
    private final int parallelism;
    private final String name;
    private final String uid;
    private final Properties prop;
    private final ESConfig config;

    public ES5Sink(StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
        this.parallelism = parallelism;
        this.name = name;
        this.uid = uid;
        this.prop = Utils.toProperties(ctx.parameter.toMap());
        Utils.copyProperties(this.property, prop);
        this.config = new ESConfig(prop);
    }

    public static ES5Sink of(StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        return new ES5Sink(ctx, property, parallelism, name, uid);
    }

    public <T> DataStreamSink<T> sink(Map<String, String> userConfig, DataStream<T> stream,
            ActionRequestFailureHandler failureHandler, TransformFunction<T, ActionRequest> f) {
        if (stream == null) throw new IllegalArgumentException(SINK_NULL_HINT_MSG);
        if (f == null) throw new IllegalArgumentException(FUNCTION_NULL_HINT_MSG);
        ElasticsearchSink<T> esSink = new ElasticsearchSink<>(userConfig, config.host, new ESSinkFunction<>(f), failureHandler);
        if (config.disableFlushOnCheckpoint) esSink.disableFlushOnCheckpoint();
        return afterSink(stream.addSink(esSink), parallelism, name, uid);
    }

    public <T> DataStreamSink<T> sink(Map<String, String> userConfig, DataStream<T> stream, TransformFunction<T, ActionRequest> f) {
        return sink(userConfig, stream, new RetryRejectedExecutionFailureHandler(), f);
    }
}
