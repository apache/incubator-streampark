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

package org.apache.streampark.flink.connector.elasticsearch7.sink;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.elasticsearch7.bean.RestClientFactoryImpl;
import org.apache.streampark.flink.connector.elasticsearch7.conf.ES7Config;
import org.apache.streampark.flink.connector.elasticsearch7.internal.ESSinkFunction;
import org.apache.streampark.flink.connector.function.TransformFunction;
import org.apache.streampark.flink.connector.sink.Sink;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkBase;
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.flink.streaming.connectors.elasticsearch7.RestClientFactory;
import org.elasticsearch.action.ActionRequest;

import java.util.Map;
import java.util.Properties;

public class ES7Sink implements Sink {
    public static final String FUNCTION_NULL_HINT_MSG = "ES pocess element func must not null";
    public static final String SINK_NULL_HINT_MSG = "Sink Stream must not null";

    private final StreamingContext ctx;
    private final Properties property;
    private final int parallelism;
    private final String name;
    private final String uid;
    private final Properties prop;
    private final ES7Config config;

    public ES7Sink(StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
        this.parallelism = parallelism;
        this.name = name;
        this.uid = uid;
        this.prop = Utils.toProperties(ctx.parameter.toMap());
        Utils.copyProperties(this.property, prop);
        this.config = new ES7Config(prop);
    }

    public static ES7Sink of(StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        return new ES7Sink(ctx, property, parallelism, name, uid);
    }

    public <T> DataStreamSink<T> sink(DataStream<T> stream, RestClientFactory restClientFactory,
            ActionRequestFailureHandler failureHandler, TransformFunction<T, ActionRequest> f) {
        if (stream == null) throw new IllegalArgumentException(SINK_NULL_HINT_MSG);
        if (f == null) throw new IllegalArgumentException(FUNCTION_NULL_HINT_MSG);
        ESSinkFunction<T> sinkFunc = new ESSinkFunction<>(f);
        ElasticsearchSink<T> esSink = buildESSink(restClientFactory, failureHandler, sinkFunc);
        if (config.disableFlushOnCheckpoint) esSink.disableFlushOnCheckpoint();
        return afterSink(stream.addSink(esSink), parallelism, name, uid);
    }

    public <T> DataStreamSink<T> sink(DataStream<T> stream, RestClientFactory restClientFactory, TransformFunction<T, ActionRequest> f) {
        return sink(stream, restClientFactory, new RetryRejectedExecutionFailureHandler(), f);
    }

    public <T> DataStreamSink<T> sink(DataStream<T> stream, TransformFunction<T, ActionRequest> f) {
        return sink(stream, null, new RetryRejectedExecutionFailureHandler(), f);
    }

    private <T> ElasticsearchSink<T> buildESSink(RestClientFactory restClientFactory,
            ActionRequestFailureHandler failureHandler, ESSinkFunction<T> sinkFunc) {
        ElasticsearchSink.Builder<T> sinkBuilder = new ElasticsearchSink.Builder<>(config.host, sinkFunc);
        sinkBuilder.setFailureHandler(failureHandler);
        if (restClientFactory != null) {
            sinkBuilder.setRestClientFactory(restClientFactory);
        } else {
            sinkBuilder.setRestClientFactory(new RestClientFactoryImpl(config));
        }
        for (Map.Entry<String, String> entry : config.sinkOption.getInternalConfig().entrySet()) {
            applyBulkConfig(sinkBuilder, entry.getKey(), entry.getValue());
        }
        return sinkBuilder.build();
    }

    private <T> void applyBulkConfig(ElasticsearchSink.Builder<T> sinkBuilder, String key, String value) {
        switch (key) {
            case ElasticsearchSinkBase.CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS:
                sinkBuilder.setBulkFlushMaxActions(Integer.parseInt(value)); break;
            case ElasticsearchSinkBase.CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB:
                sinkBuilder.setBulkFlushMaxSizeMb(Integer.parseInt(value)); break;
            case ElasticsearchSinkBase.CONFIG_KEY_BULK_FLUSH_INTERVAL_MS:
                sinkBuilder.setBulkFlushInterval(Integer.parseInt(value)); break;
            case ElasticsearchSinkBase.CONFIG_KEY_BULK_FLUSH_BACKOFF_ENABLE:
                sinkBuilder.setBulkFlushBackoff(Boolean.parseBoolean(value)); break;
            case ElasticsearchSinkBase.CONFIG_KEY_BULK_FLUSH_BACKOFF_TYPE:
                sinkBuilder.setBulkFlushBackoffType(ElasticsearchSinkBase.FlushBackoffType.valueOf(value)); break;
            case ElasticsearchSinkBase.CONFIG_KEY_BULK_FLUSH_BACKOFF_RETRIES:
                sinkBuilder.setBulkFlushBackoffRetries(Integer.parseInt(value)); break;
            case ElasticsearchSinkBase.CONFIG_KEY_BULK_FLUSH_BACKOFF_DELAY:
                sinkBuilder.setBulkFlushBackoffDelay(Long.parseLong(value)); break;
            default: break;
        }
    }
}
