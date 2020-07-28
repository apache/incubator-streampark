/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.flink.core.source;

import com.streamxhub.flink.core.StreamingContext;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;

import static scala.collection.JavaConversions.*;

import java.util.Collections;
import java.util.Map;

public class KafakJavaSource<T> {

    private StreamingContext ctx;
    private String[] topics;
    private String alias = "";
    private KafkaDeserializationSchema<T> deserializer;
    private WatermarkStrategy<KafkaRecord<T>> strategy;
    private Map<String, String> param = Collections.emptyMap();

    public KafakJavaSource(StreamingContext ctx) {
        this.ctx = ctx;
        this.deserializer = (KafkaDeserializationSchema<T>) new KafkaStringDeserializationSchema();
    }

    public KafakJavaSource<T> param(Map<String, String> param) {
        if (param != null) {
            this.param = param;
        }
        return this;
    }

    public KafakJavaSource<T> topic(String... topic) {
        if (topic != null) {
            this.topics = topic;
        }
        return this;
    }

    public KafakJavaSource<T> alias(String alias) {
        if (alias != null) {
            this.alias = alias;
        }
        return this;
    }

    public KafakJavaSource<T> deserializer(KafkaDeserializationSchema<T> deserializer) {
        if (deserializer != null) {
            this.deserializer = deserializer;
        }
        return this;
    }

    public KafakJavaSource<T> strategy(WatermarkStrategy<KafkaRecord<T>> strategy) {
        if (strategy != null) {
            this.strategy = strategy;
        }
        return this;
    }

    public DataStreamSource<KafkaRecord<T>> getDataStream() {
        FlinkKafkaConsumer<KafkaRecord<T>> consumer = KafkaSource.getSource(this.ctx, mapAsScalaMap(this.param), this.topics, this.alias, this.deserializer, this.strategy, null);
        return ctx.getJavaEnv().addSource(consumer);
    }

}
