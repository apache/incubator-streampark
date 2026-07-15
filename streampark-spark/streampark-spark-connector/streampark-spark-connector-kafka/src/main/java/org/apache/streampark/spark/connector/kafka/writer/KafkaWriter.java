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

package org.apache.streampark.spark.connector.kafka.writer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.dstream.DStream;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.function.Function;

/** Kafka writer utilities. */
public abstract class KafkaWriter<T> implements Serializable {

    private static final HashMap<Properties, Object> PRODUCERS = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected static <K, V> KafkaProducer<K, V> getProducer(Properties config) {
        return (KafkaProducer<K, V>) PRODUCERS.computeIfAbsent(
            config,
            k -> {
                KafkaProducer<K, V> producer = new KafkaProducer<>(config);
                return producer;
            });
    }

    public static <T> KafkaWriter<T> forDStream(DStream<T> dstream) {
        return new DStreamKafkaWriter<>(dstream);
    }

    public static <T> KafkaWriter<T> forRdd(JavaRDD<T> rdd) {
        return new RDDKafkaWriter<>(rdd);
    }

    public static <T> KafkaWriter<T> forIterator(Iterator<T> msg) {
        return new IterKafkaWriter<>(msg);
    }

    public static <T> KafkaWriter<T> forMessage(T msg) {
        return new SimpleKafkaWriter<>(msg);
    }

    public abstract <K, V> void writeToKafka(
                                             Properties producerConfig,
                                             Function<T, ProducerRecord<K, V>> serializerFunc);
}
