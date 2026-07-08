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

package org.apache.streampark.flink.connector.kafka.sink;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.ConfigUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.kafka.bean.KafkaEqualityPartitioner;
import org.apache.streampark.flink.connector.sink.Sink;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.internals.KeyedSerializationSchemaWrapper;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;

import java.util.Optional;
import java.util.Properties;

/** Kafka sink connector. */
public class KafkaSink<T> implements Sink {

    private final StreamingContext ctx;
    private final Properties property;
    private final int parallelism;
    private final String name;
    private final String uid;

    public KafkaSink(StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
        this.parallelism = parallelism;
        this.name = name;
        this.uid = uid;
    }

    public static <T> KafkaSink<T> of(
            StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        return new KafkaSink<>(ctx, property, parallelism, name, uid);
    }

    public DataStreamSink<T> sink(
            DataStream<T> stream,
            String alias,
            String topic,
            SerializationSchema<T> serializer,
            FlinkKafkaPartitioner<T> partitioner) {

        Properties prop = ConfigUtils.getKafkaSinkConf(ctx.parameter.toMap(), topic, alias != null ? alias : "");
        Utils.copyProperties(property, prop);
        String topicId = prop.remove(ConfigKeys.KEY_KAFKA_TOPIC).toString();

        FlinkKafkaProducer.Semantic semantic;
        Object semanticVal = prop.remove(ConfigKeys.KEY_KAFKA_SEMANTIC);
        if (semanticVal == null) {
            semantic = FlinkKafkaProducer.Semantic.AT_LEAST_ONCE;
        } else {
            switch (semanticVal.toString().toUpperCase()) {
                case "AT_LEAST_ONCE":
                    semantic = FlinkKafkaProducer.Semantic.AT_LEAST_ONCE;
                    break;
                case "EXACTLY_ONCE":
                    semantic = FlinkKafkaProducer.Semantic.EXACTLY_ONCE;
                    break;
                case "NONE":
                    semantic = FlinkKafkaProducer.Semantic.NONE;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "[StreamPark] kafka.sink semantic error,must be (AT_LEAST_ONCE|EXACTLY_ONCE|NONE) ");
            }
        }

        @SuppressWarnings("unchecked")
        SerializationSchema<T> ser =
                serializer != null ? serializer : (SerializationSchema<T>) new SimpleStringSchema();
        KeyedSerializationSchemaWrapper<T> schema = new KeyedSerializationSchemaWrapper<>(ser);

        FlinkKafkaPartitioner<T> part =
                partitioner != null ? partitioner : new KafkaEqualityPartitioner<>(ctx.getJavaEnv().getParallelism());
        Optional<FlinkKafkaPartitioner<T>> customPartitioner = Optional.of(part);

        FlinkKafkaProducer<T> producer =
                new FlinkKafkaProducer<>(
                        topicId,
                        schema,
                        prop,
                        customPartitioner,
                        semantic,
                        FlinkKafkaProducer.DEFAULT_KAFKA_PRODUCERS_POOL_SIZE);

        producer.setWriteTimestampToKafka(true);
        DataStreamSink<T> sink = stream.addSink(producer);
        return afterSink(sink, parallelism, name, uid);
    }
}
