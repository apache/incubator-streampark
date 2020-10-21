/*
 * Copyright (c) 2018 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.profiling.reporters;

import com.uber.profiling.Reporter;
import com.uber.profiling.ArgumentUtils;
import com.uber.profiling.util.AgentLogger;
import com.uber.profiling.util.JsonUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class KafkaOutputReporter implements Reporter {
    public final static String ARG_BROKER_LIST = "brokerList";
    public final static String ARG_SYNC_MODE = "syncMode";
    public final static String ARG_TOPIC_PREFIX = "topicPrefix";

    private static final AgentLogger logger = AgentLogger.getLogger(KafkaOutputReporter.class.getName());
    
    private String brokerList = "localhost:9092";
    private boolean syncMode = false;
    
    private String topicPrefix;
    
    private ConcurrentHashMap<String, String> profilerTopics = new ConcurrentHashMap<>();

    private Producer<String, byte[]> producer;

    public KafkaOutputReporter() {
    }
    
    public KafkaOutputReporter(String brokerList, boolean syncMode, String topicPrefix) {
        this.brokerList = brokerList;
        this.syncMode = syncMode;
        this.topicPrefix = topicPrefix;
    }

    @Override
    public void updateArguments(Map<String, List<String>> parsedArgs) {
        String argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_BROKER_LIST);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            setBrokerList(argValue);
            logger.info("Got argument value for brokerList: " + brokerList);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_SYNC_MODE);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            setSyncMode(Boolean.parseBoolean(argValue));
            logger.info("Got argument value for syncMode: " + syncMode);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_TOPIC_PREFIX);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            setTopicPrefix(argValue);
            logger.info("Got argument value for topicPrefix: " + topicPrefix);
        }
    }

    @Override
    public void report(String profilerName, Map<String, Object> metrics) {
        ensureProducer();

        String topicName = getTopic(profilerName);
        
        String str = JsonUtils.serialize(metrics);
        byte[] message = str.getBytes(StandardCharsets.UTF_8);

        Future<RecordMetadata> future = producer.send(
                new ProducerRecord<String, byte[]>(topicName, message));

        if (syncMode) {
            producer.flush();
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (producer == null) {
                return;
            }

            producer.flush();
            producer.close();

            producer = null;
        }
    }

    public String getBrokerList() {
        return brokerList;
    }

    public void setBrokerList(String brokerList) {
        this.brokerList = brokerList;
    }

    public boolean isSyncMode() {
        return syncMode;
    }

    public void setSyncMode(boolean syncMode) {
        this.syncMode = syncMode;
    }

    public void setTopic(String profilerName, String topicName) {
        profilerTopics.put(profilerName, topicName);
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public String getTopic(String profilerName) {
        String topic = profilerTopics.getOrDefault(profilerName, null);
        if (topic == null || topic.isEmpty()) {
            topic = topicPrefix == null ? "" : topicPrefix;
            topic += profilerName;
        }
        return topic;
    }

    private void ensureProducer() {
        synchronized (this) {
            if (producer != null) {
                return;
            }

            Properties props = new Properties();
            props.put("bootstrap.servers", brokerList);
            props.put("retries", 10);
            props.put("batch.size", 16384);
            props.put("linger.ms", 0);
            props.put("buffer.memory", 16384000);
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", org.apache.kafka.common.serialization.ByteArraySerializer.class.getName());

            if (syncMode) {
                props.put("acks", "all");
            }

            producer = new KafkaProducer<>(props);
        }
    }
}
