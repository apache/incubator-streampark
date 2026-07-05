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

package org.apache.streampark.flink.connector.kafka.bean;

/** Kafka consumer record wrapper. */
public class KafkaRecord<T> {

    private final String topic;
    private final long partition;
    private final long timestamp;
    private final long offset;
    private final String key;
    private final T value;

    public KafkaRecord(
            String topic, long partition, long timestamp, long offset, String key, T value) {
        this.topic = topic;
        this.partition = partition;
        this.timestamp = timestamp;
        this.offset = offset;
        this.key = key;
        this.value = value;
    }

    public String getTopic() {
        return topic;
    }

    public long getPartition() {
        return partition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getOffset() {
        return offset;
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }
}
