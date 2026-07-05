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

package org.apache.streampark.spark.connector.kafka.sink;

import org.apache.streampark.spark.connector.kafka.writer.RDDKafkaWriter;
import org.apache.streampark.spark.connector.sink.Sink;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.Time;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/** Kafka sink. */
public class KafkaSink<T> extends Sink<T> {

    private final Map<String, String> initParams;
    private Properties prop;

    public KafkaSink(SparkContext sc) {
        this(sc, Collections.emptyMap());
    }

    public KafkaSink(SparkContext sc, Map<String, String> initParams) {
        super(sc);
        this.initParams = initParams;
    }

    @Override
    public String getPrefix() {
        return "spark.sink.kafka.";
    }

    private Properties getProp() {
        if (prop == null) {
            prop = filterProp(getParam(), initParams, getPrefix(), "");
        }
        return prop;
    }

    @Override
    public void sink(JavaRDD<T> rdd, Time time) {
        String outputTopic = getProp().getProperty("topic");
        new RDDKafkaWriter<>(rdd)
                .writeToKafka(
                        getProp(),
                        x ->
                                new ProducerRecord<>(
                                        outputTopic, UUID.randomUUID().toString(), x.toString()));
    }
}
