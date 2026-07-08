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

import org.apache.streampark.common.util.RedisEndpoint;
import org.apache.streampark.common.util.RedisUtils;

import org.apache.kafka.common.TopicPartition;
import org.apache.spark.SparkConf;

import redis.clients.jedis.Protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Redis offset manager. */
class RedisOffset extends Offset {

    RedisOffset(SparkConf sparkConf) {
        super(sparkConf);
    }

    private RedisEndpoint endpoint() {
        Map<String, String> params = getStoreParams();
        String host = params.getOrDefault("redis.hosts", Protocol.DEFAULT_HOST);
        int port = Integer.parseInt(params.getOrDefault("redis.port", String.valueOf(Protocol.DEFAULT_PORT)));
        String auth = params.get("redis.auth");
        int dbNum = Integer.parseInt(params.getOrDefault("redis.db", String.valueOf(Protocol.DEFAULT_DATABASE)));
        int timeout = Integer.parseInt(params.getOrDefault("redis.timeout", String.valueOf(Protocol.DEFAULT_TIMEOUT)));
        return new RedisEndpoint(host, port, auth, dbNum, timeout);
    }

    @Override
    public Map<TopicPartition, Long> get(String groupId, Set<String> topics) {
        Map<TopicPartition, Long> earliestOffsets = getEarliestOffsets(new ArrayList<>(topics));
        Map<TopicPartition, Long> offsetMap = new HashMap<>();
        RedisUtils.doRedis(
            redis -> {
                for (String topic : topics) {
                    Map<String, String> entries = RedisUtils.hgetAll(key(groupId, topic), endpoint());
                    for (Map.Entry<String, String> entry : entries.entrySet()) {
                        TopicPartition tp = new TopicPartition(topic, Integer.parseInt(entry.getKey()));
                        Long left = earliestOffsets.get(tp);
                        long finalOffset = entry.getValue() == null ? 0L : Long.parseLong(entry.getValue());
                        if (left != null && left > finalOffset) {
                            log.warn(
                                "storeType:Redis,consumer group:{},topic:{},partition:{} offsets Outdated,updated:{}",
                                groupId,
                                tp.topic(),
                                tp.partition(),
                                left);
                            finalOffset = left;
                        }
                        offsetMap.put(tp, finalOffset);
                    }
                }
                return null;
            },
            endpoint());
        Map<TopicPartition, Long> offsetMaps;
        if ("largest".equalsIgnoreCase(getReset())) {
            offsetMaps = new HashMap<>(getLatestOffsets(new ArrayList<>(topics)));
        } else {
            offsetMaps = new HashMap<>(getEarliestOffsets(new ArrayList<>(topics)));
        }
        offsetMaps.putAll(offsetMap);
        log.info("getOffsets [{},{}] ", groupId, offsetMaps);
        return offsetMaps;
    }

    @Override
    public void update(String groupId, Map<TopicPartition, Long> offsets) {
        RedisUtils.doRedis(
            redis -> {
                for (Map.Entry<TopicPartition, Long> entry : offsets.entrySet()) {
                    RedisUtils.hset(
                        key(groupId, entry.getKey().topic()),
                        String.valueOf(entry.getKey().partition()),
                        String.valueOf(entry.getValue()),
                        null,
                        endpoint());
                }
                return null;
            },
            endpoint());
        log.info("storeType:Redis,updateOffsets [ {},{} ]", groupId, offsets);
    }

    @Override
    public void delete(String groupId, Set<String> topics) {
        RedisUtils.doRedis(
            redis -> {
                for (String topic : topics) {
                    RedisUtils.del(key(groupId, topic), endpoint());
                }
                return null;
            },
            endpoint());
        log.info("storeType:Redis,deleteOffsets [ {},{} ]", groupId, topics);
    }
}
