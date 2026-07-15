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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.dstream.InputDStream;
import org.apache.spark.streaming.kafka010.CanCommitOffsets;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.apache.spark.streaming.kafka010.LocationStrategy;
import org.apache.spark.streaming.kafka010.OffsetRange;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import scala.reflect.ClassTag;
import scala.reflect.ClassTag$;

/** Kafka direct stream client with offset management. */
public class KafkaClient implements Serializable {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(KafkaClient.class.getName());

    private final SparkConf sparkConf;
    private final Offset offsetManager;
    private transient CanCommitOffsets canCommitOffsets;

    public KafkaClient(SparkConf sparkConf) {
        this.sparkConf = sparkConf;
        this.offsetManager = createOffsetManager(sparkConf);
    }

    public String getOffsetStoreType() {
        return offsetManager.getStoreType();
    }

    private Offset createOffsetManager(SparkConf sparkConf) {
        String clazz = sparkConf.get("spark.source.kafka.offset.store.class", "none").trim();
        if (!"none".equals(clazz)) {
            LOG.info("Custom offset management class {}", clazz);
            try {
                Class<?> offsetsManagerClass = Class.forName(clazz);
                for (Constructor<?> c : offsetsManagerClass.getConstructors()) {
                    if (c.getParameterCount() == 1
                        && c.getParameterTypes()[0] == SparkConf.class) {
                        return (Offset) c.newInstance(sparkConf);
                    }
                }
                throw new IllegalStateException("No SparkConf constructor found for " + clazz);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        String type = sparkConf.get("spark.source.kafka.offset.store.type", "none").trim().toLowerCase();
        switch (type) {
            case "redis":
                return new RedisOffset(sparkConf);
            case "hbase":
                return new HBaseOffset(sparkConf);
            case "kafka":
            case "none":
                return new DefaultOffset(sparkConf);
            case "mysql":
                return new MySQLOffset(sparkConf);
            default:
                return new DefaultOffset(sparkConf);
        }
    }

    public <K, V> InputDStream<ConsumerRecord<K, V>> createDirectStream(
                                                                        StreamingContext ssc,
                                                                        Map<String, Object> kafkaParams,
                                                                        Set<String> topics) {
        Map<TopicPartition, Long> consumerOffsets = new HashMap<>();
        Object groupId = kafkaParams.get("group.id");
        if (groupId != null) {
            consumerOffsets = offsetManager.get(groupId.toString(), topics);
            LOG.info("createDirectStream witch group.id {} topics {}", groupId, String.join(",", topics));
        } else {
            LOG.info("createDirectStream witchOut group.id topics {}", String.join(",", topics));
        }
        InputDStream<ConsumerRecord<K, V>> stream;
        if (!consumerOffsets.isEmpty()) {
            LOG.info("read topics ==[{}]== from offsets ==[{}]==", topics, consumerOffsets);
            stream =
                KafkaUtils.createDirectStream(
                    ssc,
                    LocationStrategies.PreferConsistent(),
                    ConsumerStrategies.Assign(consumerOffsets.keySet(), kafkaParams, consumerOffsets));
        } else {
            stream =
                KafkaUtils.createDirectStream(
                    ssc,
                    LocationStrategies.PreferConsistent(),
                    ConsumerStrategies.Subscribe(topics, kafkaParams));
        }
        canCommitOffsets = (CanCommitOffsets) stream;
        return stream;
    }

    @SuppressWarnings("unchecked")
    public <K, V> JavaRDD<ConsumerRecord<K, V>> createRDD(
                                                          org.apache.spark.SparkContext sc,
                                                          Map<String, Object> kafkaParams,
                                                          OffsetRange[] offsetRanges,
                                                          LocationStrategy locationStrategy) {
        ClassTag<ConsumerRecord<K, V>> tag =
            (ClassTag<ConsumerRecord<K, V>>) (ClassTag<?>) ClassTag$.MODULE$.Any();
        return JavaRDD.fromRDD(
            KafkaUtils.createRDD(sc, kafkaParams, offsetRanges, locationStrategy), tag);
    }

    public void updateOffset(String groupId, OffsetRange[] offsetRanges) {
        if ("kafka".equals(getOffsetStoreType())) {
            canCommitOffsets.commitAsync(offsetRanges);
            return;
        }
        Map<TopicPartition, Long> tps = new HashMap<>();
        for (OffsetRange range : offsetRanges) {
            tps.put(new TopicPartition(range.topic(), range.partition()), range.untilOffset());
        }
        offsetManager.update(groupId, tps);
    }
}
