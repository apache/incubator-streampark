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

import java.util.Properties;
import java.util.function.Function;

/** Simple single-message Kafka writer. */
public class SimpleKafkaWriter<T> extends KafkaWriter<T> {
    private final T msg;

    public SimpleKafkaWriter(T msg) {
        this.msg = msg;
    }

    @Override
    public <K, V> void writeToKafka(
            Properties producerConfig, Function<T, ProducerRecord<K, V>> serializerFunc) {
        KafkaProducer<K, V> producer = getProducer(producerConfig);
        producer.send(serializerFunc.apply(msg));
    }
}
