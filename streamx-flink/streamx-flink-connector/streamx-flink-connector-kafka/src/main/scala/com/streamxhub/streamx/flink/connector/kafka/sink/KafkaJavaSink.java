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

package com.streamxhub.streamx.flink.connector.kafka.sink;

import com.streamxhub.streamx.flink.connector.kafka.bean.KafkaEqualityPartitioner;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;

import java.util.Properties;

/**
 * @author benjobs
 */
public class KafkaJavaSink<T> {

    private final StreamingContext context;
    /**
     * common param
     */
    private Properties property = new Properties();
    private Integer parallelism = 0;
    private String name;
    private String uid;
    //---end---

    private String alias = "";
    private String topic = "";
    private SerializationSchema<T> serializer = (SerializationSchema<T>) new SimpleStringSchema();
    private FlinkKafkaPartitioner<T> partitioner;

    public KafkaJavaSink(StreamingContext context) {
        this.context = context;
        //默认partitioner为KafkaEqualityPartitioner
        partitioner = new KafkaEqualityPartitioner<T>(context.getParallelism());
    }

    public KafkaJavaSink<T> parallelism(Integer parallelism) {
        if (parallelism != null) {
            this.parallelism = parallelism;
        }
        return this;
    }

    public KafkaJavaSink<T> name(String name) {
        if (name != null) {
            this.name = name;
        }
        return this;
    }

    public KafkaJavaSink<T> uid(String uid) {
        if (uid != null) {
            this.uid = uid;
        }
        return this;
    }

    public KafkaJavaSink<T> property(Properties property) {
        if (property != null) {
            this.property = property;
        }
        return this;
    }

    public KafkaJavaSink<T> alias(String alias) {
        if (alias != null) {
            this.alias = alias;
        }
        return this;
    }

    /**
     * 设置要下沉的topic
     *
     * @param topic: topic name
     * @return KafkaSink: KafkaSink instance
     */
    public KafkaJavaSink<T> topic(String topic) {
        if (topic != null) {
            this.topic = topic;
        }
        return this;
    }

    /**
     * set SerializationSchema
     *
     * @param serializer: serializer
     * @return KafkaSink: KafkaSink instance
     */
    public KafkaJavaSink<T> serializer(SerializationSchema<T> serializer) {
        if (serializer != null) {
            this.serializer = serializer;
        }
        return this;
    }

    /**
     * set FlinkKafkaPartitioner
     *
     * @param partitioner: FlinkKafkaPartitioner
     * @return KafkaSink: KafkaSink
     */
    public KafkaJavaSink<T> partitioner(FlinkKafkaPartitioner<T> partitioner) {
        if (partitioner != null) {
            this.partitioner = partitioner;
        }
        return this;
    }

    public DataStreamSink<T> sink(DataStream<T> source) {
        return this.sink(source, this.topic);
    }

    public DataStreamSink<T> sink(DataStream<T> source, String topic) {
        this.topic(topic);
        KafkaSink scalaSink = new KafkaSink(
            this.context,
            this.property,
            this.parallelism,
            this.name,
            this.uid
        );
        return scalaSink.sink(
            new org.apache.flink.streaming.api.scala.DataStream<>(source),
            this.alias,
            this.topic,
            this.serializer,
            this.partitioner
        );
    }
}
