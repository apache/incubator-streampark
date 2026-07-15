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

package org.apache.streampark.spark.connector.kafka.source;

import org.apache.streampark.spark.connector.kafka.offset.KafkaClient;
import org.apache.streampark.spark.connector.source.Source;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.rdd.RDD;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.Time;
import org.apache.spark.streaming.dstream.DStream;
import org.apache.spark.streaming.kafka010.HasOffsetRanges;
import org.apache.spark.streaming.kafka010.OffsetRange;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import scala.Function1;
import scala.Function2;
import scala.Tuple2;
import scala.reflect.ClassTag;
import scala.reflect.ClassTag$;

/** Wrapped Kafka Direct API. */
public class KafkaSource<K, V> extends Source {

    private final Map<String, String> overrideParams;
    private final KafkaClient kafkaClient;
    private final Map<Long, OffsetRange[]> offsetRanges = new ConcurrentHashMap<>();

    public KafkaSource(StreamingContext ssc) {
        this(ssc, Collections.emptyMap());
    }

    public KafkaSource(StreamingContext ssc, Map<String, String> overrideParams) {
        super(ssc);
        this.overrideParams = overrideParams;
        this.kafkaClient = new KafkaClient(ssc.sparkContext().getConf());
    }

    @Override
    public String getPrefix() {
        return "spark.source.kafka.consume.";
    }

    private int getRepartition() {
        return Integer.parseInt(getSparkConf().get("spark.source.kafka.consume.repartition", "0"));
    }

    private Set<String> getTopicSet() {
        String topics =
            overrideParams.getOrDefault(
                "consume.topics", getSparkConf().get("spark.source.kafka.consume.topics"));
        Set<String> topicSet = new HashSet<>();
        for (String t : topics.split(",")) {
            topicSet.add(t.trim());
        }
        return topicSet;
    }

    private Map<String, Object> getKafkaParams() {
        Map<String, Object> params = new HashMap<>();
        scala.collection.Iterator<Tuple2<String, String>> iter =
            scala.collection.JavaConverters.asScalaIteratorConverter(
                Arrays.asList(getSparkConf().getAll()).iterator())
                .asScala()
                .toIterator();
        String prefix = getPrefix();
        while (iter.hasNext()) {
            Tuple2<String, String> t = iter.next();
            if (t._1().startsWith(prefix) && t._2() != null && !t._2().isEmpty()) {
                params.put(t._1().substring(prefix.length()), t._2());
            }
        }
        params.putAll(overrideParams);
        params.put("enable.auto.commit", "false");
        return params;
    }

    public Optional<String> getGroupId() {
        Object groupId = getKafkaParams().get("group.id");
        return groupId == null ? Optional.empty() : Optional.of(groupId.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> DStream<R> getDStream(Function<Object, R> recordHandler) {
        DStream<ConsumerRecord<K, V>> stream =
            kafkaClient.createDirectStream(ssc, getKafkaParams(), getTopicSet());
        ClassTag<ConsumerRecord<K, V>> recordTag =
            (ClassTag<ConsumerRecord<K, V>>) (ClassTag<?>) ClassTag$.MODULE$.Any();
        ClassTag<R> resultTag = (ClassTag<R>) (ClassTag<?>) ClassTag$.MODULE$.Any();
        DStream<ConsumerRecord<K, V>> withOffsets =
            stream.transform(
                new Function2<RDD<ConsumerRecord<K, V>>, Time, RDD<ConsumerRecord<K, V>>>() {

                    @Override
                    public RDD<ConsumerRecord<K, V>> apply(
                                                           RDD<ConsumerRecord<K, V>> rdd, Time time) {
                        HasOffsetRanges hasOffsets = (HasOffsetRanges) rdd;
                        offsetRanges.put(time.milliseconds(), hasOffsets.offsetRanges());
                        return rdd;
                    }
                },
                recordTag);
        return withOffsets.map(
            new Function1<ConsumerRecord<K, V>, R>() {

                @Override
                public R apply(ConsumerRecord<K, V> record) {
                    try {
                        return recordHandler.call(record);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            },
            resultTag);
    }

    public void updateOffset(Time time) {
        long milliseconds = time.milliseconds();
        getGroupId()
            .ifPresent(
                groupId -> {
                    log.info(
                        "updateOffset with {} for time {} offsetRanges: {}",
                        kafkaClient.getOffsetStoreType(),
                        milliseconds,
                        offsetRanges);
                    OffsetRange[] offsetRange = offsetRanges.get(milliseconds);
                    if (offsetRange != null) {
                        kafkaClient.updateOffset(groupId, offsetRange);
                    }
                });
        offsetRanges.remove(milliseconds);
    }
}
