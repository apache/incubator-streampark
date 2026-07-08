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

package org.apache.streampark.flink.connector.kafka.source;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.ConfigUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.kafka.bean.KafkaRecord;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/** Kafka source factory and consumer configuration. */
public class KafkaSource {

    public static KafkaSource of(StreamingContext ctx, Properties property) {
        return new KafkaSource(ctx, property);
    }

    public static <T> FlinkKafkaConsumer<KafkaRecord<T>> getSource(
            StreamingContext ctx,
            Properties property,
            String[] topics,
            String alias,
            KafkaDeserializationSchema<T> deserializer,
            WatermarkStrategy<KafkaRecord<T>> strategy,
            Serializable topicParam) {

        Properties prop = ConfigUtils.getConf(ctx.parameter.toMap(), ConfigKeys.KAFKA_SOURCE_PREFIX + alias);
        Utils.copyProperties(property, prop);

        boolean hasTopic = prop.containsKey(ConfigKeys.KEY_KAFKA_TOPIC);
        boolean hasPattern = prop.containsKey(ConfigKeys.KEY_KAFKA_PATTERN);
        if (prop.isEmpty() || (!hasTopic && !hasPattern)) {
            throw new IllegalArgumentException("[StreamPark] kafka source config error");
        }

        Long timestamp = parseLong(prop.getProperty(
                ConfigKeys.KEY_KAFKA_START_FROM + "." + ConfigKeys.KEY_KAFKA_START_FROM_TIMESTAMP));
        StartFrom[] startFrom = StartFrom.startFrom(prop);
        if (timestamp != null && startFrom.length > 0) {
            throw new IllegalArgumentException(
                    "[StreamPark] start.form timestamp and offset cannot be defined at the same time");
        }

        String topicOpt = (String) prop.remove(ConfigKeys.KEY_KAFKA_TOPIC);
        String regexOpt = (String) prop.remove(ConfigKeys.KEY_KAFKA_PATTERN);

        KafkaDeserializer<T> kfkDeserializer = new KafkaDeserializer<>(deserializer);
        FlinkKafkaConsumer<KafkaRecord<T>> consumer;

        if (topicOpt != null && regexOpt != null) {
            throw new IllegalArgumentException(
                    "[StreamPark] topic and regex cannot be defined at the same time");
        } else if (topicOpt != null) {
            List<String> topicList = resolveTopics(topics, topicOpt);
            consumer = new FlinkKafkaConsumer<>(topicList, kfkDeserializer, prop);
        } else if (regexOpt != null) {
            Pattern pattern = resolvePattern(topicParam, regexOpt);
            consumer = new FlinkKafkaConsumer<>(pattern, kfkDeserializer, prop);
        } else {
            throw new IllegalArgumentException("[StreamPark] kafka topic or pattern required");
        }

        boolean autoCommit =
                Boolean.parseBoolean(
                        prop.getOrDefault(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true").toString());
        if (ctx.getJavaEnv().getCheckpointConfig().isCheckpointingEnabled()) {
            consumer.setCommitOffsetsOnCheckpoints(true);
        } else if (!autoCommit) {
            throw new IllegalArgumentException(
                    "[StreamPark] error:flink checkpoint was disable,and kafka autoCommit was false."
                            + "you can enable checkpoint or enable kafka autoCommit...");
        }

        if (strategy != null) {
            consumer.assignTimestampsAndWatermarks(strategy);
        }

        if (timestamp != null) {
            consumer.setStartFromTimestamp(timestamp);
        } else {
            List<StartFrom> startFroms = filterStartFrom(startFrom, topicOpt, regexOpt, topics, topicParam);
            Map<KafkaTopicPartition, Long> startOffsets = new HashMap<>();
            for (StartFrom start : startFroms) {
                if (start != null && start.getPartitionOffset() != null) {
                    for (Map.Entry<Integer, Long> entry : start.getPartitionOffset().entrySet()) {
                        startOffsets.put(
                                new KafkaTopicPartition(start.getTopic(), entry.getKey()), entry.getValue());
                    }
                }
            }
            if (!startOffsets.isEmpty()) {
                Map<KafkaTopicPartition, Long> offsets = new HashMap<>();
                for (Map.Entry<KafkaTopicPartition, Long> e : startOffsets.entrySet()) {
                    offsets.put(e.getKey(), e.getValue());
                }
                consumer.setStartFromSpecificOffsets(offsets);
            }
        }
        return consumer;
    }

    private static List<String> resolveTopics(String[] topics, String topicOpt) {
        if (topics != null && topics.length > 0) {
            return Arrays.asList(topics);
        }
        return Arrays.asList(topicOpt.split(",|\\s+"));
    }

    private static Pattern resolvePattern(Serializable topicParam, String regexOpt) {
        if (topicParam instanceof String) {
            return Pattern.compile((String) topicParam);
        }
        return Pattern.compile(regexOpt);
    }

    private static List<StartFrom> filterStartFrom(
            StartFrom[] startFrom,
            String topicOpt,
            String regexOpt,
            String[] topics,
            Serializable topicParam) {
        List<StartFrom> result = new ArrayList<>();
        if (topicOpt != null) {
            List<String> topicNames;
            if (topics != null && topics.length > 0) {
                topicNames = Arrays.asList(topics);
            } else {
                topicNames = Arrays.asList(topicOpt.split(",|\\s+"));
            }
            for (StartFrom s : startFrom) {
                if (s != null && topicNames.contains(s.getTopic())) {
                    result.add(s);
                }
            }
        } else if (regexOpt != null) {
            Pattern pattern = resolvePattern(topicParam, regexOpt);
            for (StartFrom s : startFrom) {
                if (s != null && pattern.matcher(s.getTopic()).find()) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    private static Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private final StreamingContext ctx;
    private final Properties property;

    private KafkaSource(StreamingContext ctx, Properties property) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
    }

    public <T> org.apache.flink.streaming.api.datastream.DataStreamSource<KafkaRecord<T>> getDataStream(
            String[] topics,
            String alias,
            KafkaDeserializationSchema<T> deserializer,
            WatermarkStrategy<KafkaRecord<T>> strategy) {
        @SuppressWarnings("unchecked")
        KafkaDeserializationSchema<T> deser =
                deserializer != null
                        ? deserializer
                        : (KafkaDeserializationSchema<T>) new KafkaStringDeserializationSchema();
        FlinkKafkaConsumer<KafkaRecord<T>> consumer =
                getSource(ctx, property, topics, alias != null ? alias : "", deser, strategy, null);
        return ctx.getJavaEnv().addSource(consumer);
    }

    /** Kafka deserialization wrapper producing {@link KafkaRecord}. */
    public static class KafkaDeserializer<T> implements KafkaDeserializationSchema<KafkaRecord<T>> {

        private final KafkaDeserializationSchema<T> deserializer;

        public KafkaDeserializer(KafkaDeserializationSchema<T> deserializer) {
            this.deserializer = deserializer;
        }

        @Override
        public KafkaRecord<T> deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception {
            String key =
                    record.key() == null ? null : new String(record.key(), StandardCharsets.UTF_8);
            T value = deserializer.deserialize(record);
            return new KafkaRecord<>(
                    record.topic(),
                    record.partition(),
                    record.timestamp(),
                    record.offset(),
                    key,
                    value);
        }

        @Override
        public TypeInformation<KafkaRecord<T>> getProducedType() {
            return TypeExtractor.getForClass((Class<KafkaRecord<T>>) (Class<?>) KafkaRecord.class);
        }

        @Override
        public boolean isEndOfStream(KafkaRecord<T> nextElement) {
            return false;
        }
    }

    /** Default string deserializer. */
    public static class KafkaStringDeserializationSchema
            implements KafkaDeserializationSchema<String> {

        @Override
        public String deserialize(ConsumerRecord<byte[], byte[]> record) {
            return new String(record.value(), StandardCharsets.UTF_8);
        }

        @Override
        public TypeInformation<String> getProducedType() {
            return TypeInformation.of(String.class);
        }

        @Override
        public boolean isEndOfStream(String nextElement) {
            return false;
        }
    }

    /** Start offset configuration per topic. */
    public static class StartFrom {
        private final String topic;
        private final Map<Integer, Long> partitionOffset;

        public StartFrom(String topic, Map<Integer, Long> partitionOffset) {
            this.topic = topic;
            this.partitionOffset = partitionOffset;
        }

        public String getTopic() {
            return topic;
        }

        public Map<Integer, Long> getPartitionOffset() {
            return partitionOffset;
        }

        public static StartFrom[] startFrom(Properties prop) {
            Map<String, String> startProp = new HashMap<>();
            for (String key : prop.stringPropertyNames()) {
                if (key.startsWith(ConfigKeys.KEY_KAFKA_START_FROM)) {
                    startProp.put(key, prop.getProperty(key));
                    prop.remove(key);
                }
            }
            String topicKey =
                    ConfigKeys.KEY_KAFKA_START_FROM
                            + "."
                            + ConfigKeys.KEY_KAFKA_START_FROM_OFFSET
                            + "."
                            + ConfigKeys.KEY_KAFKA_TOPIC;
            String topicStr = startProp.get(topicKey);
            if (topicStr == null || topicStr.isEmpty()) {
                return new StartFrom[0];
            }
            String[] topicNames = topicStr.split(",");
            List<StartFrom> result = new ArrayList<>();
            for (String topic : topicNames) {
                String offsetKey =
                        ConfigKeys.KEY_KAFKA_START_FROM
                                + "."
                                + ConfigKeys.KEY_KAFKA_START_FROM_OFFSET
                                + "."
                                + topic;
                String offset = startProp.get(offsetKey);
                if (offset == null) {
                    continue;
                }
                Map<Integer, Long> partitionMap = new HashMap<>();
                for (String part : offset.split(",")) {
                    String[] array = part.split(":");
                    partitionMap.put(Integer.parseInt(array[0]), Long.parseLong(array[1]));
                }
                result.add(new StartFrom(topic, partitionMap));
            }
            return result.toArray(new StartFrom[0]);
        }
    }
}
