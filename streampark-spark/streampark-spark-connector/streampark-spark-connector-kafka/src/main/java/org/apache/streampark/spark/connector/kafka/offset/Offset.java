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

package org.apache.streampark.spark.connector.kafka.offset;

import org.apache.streampark.common.util.StreamParkLoggerFactory;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.spark.SparkConf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import scala.Tuple2;

/** Offset manager base class. */
public abstract class Offset implements Serializable {

    protected final Logger log =
        StreamParkLoggerFactory.loggerFactory().getLogger(getClass().getName());

    protected final SparkConf sparkConf;
    protected Map<String, String> storeParams;

    private static final long LATEST_TIME = -1L;
    private static final long EARLIEST_TIME = -2L;

    protected Offset(SparkConf sparkConf) {
        this.sparkConf = sparkConf;
    }

    public String getStoreType() {
        return getStoreParams().getOrDefault("type", "none");
    }

    protected Map<String, String> getStoreParams() {
        if (storeParams == null) {
            storeParams = new HashMap<>();
            scala.collection.Iterator<Tuple2<String, String>> iter =
                scala.collection.JavaConverters.asScalaIteratorConverter(
                    Arrays.asList(
                        sparkConf.getAllWithPrefix(
                            "spark.source.kafka.offset.store."))
                        .iterator())
                    .asScala()
                    .toIterator();
            while (iter.hasNext()) {
                Tuple2<String, String> t = iter.next();
                storeParams.put(t._1(), t._2());
            }
        }
        return storeParams;
    }

    protected Properties toProperties(Map<String, String> map) {
        Properties prop = new Properties();
        map.forEach(prop::setProperty);
        return prop;
    }

    protected String getReset() {
        return sparkConf.get("spark.source.kafka.consume.auto.offset.reset", "largest");
    }

    protected String getHost() {
        return getHostPort()[0];
    }

    protected int getPort() {
        return Integer.parseInt(getHostPort()[1]);
    }

    private String[] getHostPort() {
        String servers = sparkConf.get("spark.source.kafka.consume.bootstrap.servers");
        String[] hp = servers.split(",")[0].split(":");
        return new String[]{hp[0], hp[1]};
    }

    public abstract Map<TopicPartition, Long> get(String groupId, java.util.Set<String> topics);

    public abstract void update(String groupId, Map<TopicPartition, Long> offsetInfos);

    public abstract void delete(String groupId, java.util.Set<String> topics);

    public String key(String groupId, String topic) {
        return groupId + "#" + topic;
    }

    protected Map<TopicPartition, Long> getEarliestOffsets(List<String> topics) {
        return getOffsets(topics, EARLIEST_TIME);
    }

    protected Map<TopicPartition, Long> getLatestOffsets(List<String> topics) {
        return getOffsets(topics, LATEST_TIME);
    }

    private Map<TopicPartition, Long> getOffsets(List<String> topics, long time) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", getHost() + ":" + getPort());
        props.setProperty("group.id", "offsetLookup-" + System.currentTimeMillis());
        props.setProperty("enable.auto.commit", "false");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        List<TopicPartition> partitions = new ArrayList<>();
        for (String topic : topics) {
            consumer.partitionsFor(topic).forEach(x -> partitions.add(new TopicPartition(x.topic(), x.partition())));
        }
        Map<TopicPartition, Long> offsetInfos;
        if (time == EARLIEST_TIME) {
            offsetInfos = consumer.beginningOffsets(partitions);
        } else {
            offsetInfos = consumer.endOffsets(partitions);
        }
        try {
            consumer.close();
        } catch (Exception ignored) {
        }
        return offsetInfos;
    }
}
