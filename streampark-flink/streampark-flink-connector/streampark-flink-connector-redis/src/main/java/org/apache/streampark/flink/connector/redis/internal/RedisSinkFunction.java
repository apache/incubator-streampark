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

package org.apache.streampark.flink.connector.redis.internal;

import org.apache.streampark.flink.connector.redis.bean.RedisContainer;
import org.apache.streampark.flink.connector.redis.bean.RedisMapper;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;

import java.io.IOException;
import java.util.Optional;

/** At-least-once Redis sink function. */
public class RedisSinkFunction<T> extends org.apache.flink.streaming.connectors.redis.RedisSink<T> {

    private final FlinkJedisConfigBase jedisConfig;
    private final RedisMapper<T> mapper;
    private final int ttl;
    private transient RedisContainer redisContainer;

    public RedisSinkFunction(FlinkJedisConfigBase jedisConfig, RedisMapper<T> mapper, int ttl) {
        super(jedisConfig, mapper);
        this.jedisConfig = jedisConfig;
        this.mapper = mapper;
        this.ttl = ttl;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        redisContainer = RedisContainer.getContainer(jedisConfig);
    }

    @Override
    public void invoke(T input, SinkFunction.Context context) throws Exception {
        redisContainer.invoke(mapper, input, Optional.empty());
        redisContainer.expire(mapper.getKeyFromData(input), ttl);
    }

    @Override
    public void close() throws IOException {
        if (redisContainer != null) {
            redisContainer.close();
        }
    }
}
