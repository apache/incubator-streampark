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
package com.streamxhub.streamx.flink.connector.pulsar.source;

import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.pulsar.source.PulsarSource;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StartCursor;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StopCursor;
import org.apache.flink.connector.pulsar.source.reader.deserializer.PulsarDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import java.util.Properties;
import static org.apache.flink.connector.pulsar.source.reader.deserializer.PulsarDeserializationSchema.flinkSchema;

public class PulsarJavaSource<T> {
    private final StreamingContext context;
    private String alias = "";
    private StartCursor startCursor;
    private StopCursor stopCursor;
    private String[] topics;
    private PulsarDeserializationSchema<T> deserializer;
    private String sourceName = "pulsar-source";
    private WatermarkStrategy<T> strategy;
    private Properties property = new Properties();

    public PulsarJavaSource(StreamingContext context) {
        this.context = context;
        this.deserializer = (PulsarDeserializationSchema<T>) flinkSchema(new SimpleStringSchema());
    }

    public PulsarJavaSource<T> alias(String alias) {
        if (alias != null) {
            this.alias = alias;
        }
        return this;
    }

    public PulsarJavaSource<T> startCursor(StartCursor startCursor) {
        if (startCursor != null) {
            this.startCursor = startCursor;
        }
        return this;
    }

    public PulsarJavaSource<T> stopCursor(StopCursor stopCursor) {
        if (stopCursor != null) {
            this.stopCursor = stopCursor;
        }
        return this;
    }

    public PulsarJavaSource<T> topic(String... topic) {
        if (topic != null) {
            this.topics = topic;
        }
        return this;
    }

    public PulsarJavaSource<T> deserializer(PulsarDeserializationSchema<T> deserializer) {
        if (deserializer != null) {
            this.deserializer = deserializer;
        }
        return this;
    }

    public PulsarJavaSource<T> property(Properties property) {
        if (property != null) {
            this.property = property;
        }
        return this;
    }

    public PulsarJavaSource<T> strategy(WatermarkStrategy<T> strategy) {
        if (strategy != null) {
            this.strategy = strategy;
        }
        return this;
    }

    public PulsarJavaSource<T> sourceName(String sourceName) {
        if (sourceName != null) {
            this.sourceName = sourceName;
        }
        return this;
    }

    public DataStreamSource<T> getDataStream(){
        // Create a Pulsar source, it would consume messages from Pulsar on "sample/flink/simple-string" topic.
        final PulsarSource<T> pulsarSource = FlinkPulsarSource.getSource(
            this.context,
            this.alias,
            this.startCursor,
            this.stopCursor,
            this.topics,
            this.deserializer,
            this.property,null);

        // Pulsar Source don't require extra TypeInformation be provided.
        return context.getJavaEnv().fromSource(pulsarSource, this.strategy, this.sourceName);
    }
}
