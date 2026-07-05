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

import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/** Equality partitioner that evenly writes data to each Kafka partition. */
public class KafkaEqualityPartitioner<T> extends FlinkKafkaPartitioner<T> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEqualityPartitioner.class);

    private final int parallelism;
    private int parallelInstanceId = 0;
    private final AtomicInteger partitionIndex = new AtomicInteger(0);

    public KafkaEqualityPartitioner(int parallelism) {
        this.parallelism = parallelism;
    }

    @Override
    public void open(int parallelInstanceId, int parallelInstances) {
        LOG.info("KafkaEqualityPartitioner: parallelism {}", parallelism);
        if (parallelInstanceId < 0 || parallelInstances <= 0) {
            throw new IllegalArgumentException(
                    "[StreamPark] KafkaEqualityPartitioner:Id of this subtask cannot be negative,"
                            + "Number of subtasks must be larger than 0.");
        }
        this.parallelInstanceId = parallelInstanceId;
    }

    @Override
    public int partition(
            T record, byte[] key, byte[] value, String targetTopic, int[] partitions) {
        if (partitions == null || partitions.length == 0) {
            throw new IllegalArgumentException(
                    "[StreamPark] KafkaEqualityPartitioner:Partitions of the target topic is empty.");
        }
        if (partitions.length == 1) {
            return 0;
        }
        if (parallelism % partitions.length == 0) {
            return partitions[parallelInstanceId % partitions.length];
        }
        int idx = partitionIndex.get();
        if (idx == partitions.length - 1) {
            partitionIndex.getAndSet(0);
            return partitions[partitions.length - 1];
        }
        return partitions[partitionIndex.incrementAndGet()];
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof KafkaEqualityPartitioner;
    }

    @Override
    public int hashCode() {
        return KafkaEqualityPartitioner.class.hashCode();
    }
}
