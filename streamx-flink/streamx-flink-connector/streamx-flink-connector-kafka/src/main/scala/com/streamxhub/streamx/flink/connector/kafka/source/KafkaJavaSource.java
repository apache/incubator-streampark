/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.connector.kafka.source;

import com.streamxhub.streamx.flink.connector.kafka.bean.KafkaRecord;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;

import java.util.Properties;

/**
 * @author benjobs
 */
public class KafkaJavaSource<T> {

    private StreamingContext context;
    private String[] topics;
    private String alias = "";
    private KafkaDeserializationSchema<T> deserializer;
    private WatermarkStrategy<KafkaRecord<T>> strategy;
    private Properties property = new Properties();

    public KafkaJavaSource(StreamingContext context) {
        this.context = context;
        this.deserializer = (KafkaDeserializationSchema<T>) new KafkaStringDeserializationSchema();
    }

    public KafkaJavaSource<T> property(Properties property) {
        if (property != null) {
            this.property = property;
        }
        return this;
    }

    public KafkaJavaSource<T> topic(String... topic) {
        if (topic != null) {
            this.topics = topic;
        }
        return this;
    }

    public KafkaJavaSource<T> alias(String alias) {
        if (alias != null) {
            this.alias = alias;
        }
        return this;
    }

    public KafkaJavaSource<T> deserializer(KafkaDeserializationSchema<T> deserializer) {
        if (deserializer != null) {
            this.deserializer = deserializer;
        }
        return this;
    }

    public KafkaJavaSource<T> strategy(WatermarkStrategy<KafkaRecord<T>> strategy) {
        if (strategy != null) {
            this.strategy = strategy;
        }
        return this;
    }

    public DataStreamSource<KafkaRecord<T>> getDataStream() {
        FlinkKafkaConsumer<KafkaRecord<T>> consumer = KafkaSource.getSource(
            this.context,
            this.property,
            this.topics,
            this.alias,
            this.deserializer,
            this.strategy,
            null);
        return context.getJavaEnv().addSource(consumer);
    }

}
