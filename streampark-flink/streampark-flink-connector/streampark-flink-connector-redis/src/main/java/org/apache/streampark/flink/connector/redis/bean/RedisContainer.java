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

package org.apache.streampark.flink.connector.redis.bean;

import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisSentinelConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Transaction;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;

/** Redis container wrapper with command dispatch. */
public class RedisContainer {

    private static final Logger LOG = LoggerFactory.getLogger(RedisContainer.class);

    private final org.apache.flink.streaming.connectors.redis.common.container.RedisContainer container;

    public RedisContainer(org.apache.flink.streaming.connectors.redis.common.container.RedisContainer container) {
        this.container = container;
    }

    public void open() {
        try {
            container.open();
        } catch (Exception e) {
            throw new RuntimeException("Failed to open Redis container", e);
        }
    }

    public Jedis getJedis() {
        try {
            Method method = container.getClass().getDeclaredMethod("getInstance");
            method.setAccessible(true);
            return (Jedis) method.invoke(container);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to obtain Jedis instance from RedisContainer", e);
        }
    }

    public <T> void invoke(RedisMapper<T> mapper, T input, Optional<Transaction> transaction) {
        String key = mapper.getKeyFromData(input);
        String value = mapper.getValueFromData(input);
        RedisCommand command = mapper.getCommandDescription().getCommand();
        switch (command) {
            case RPUSH:
                if (transaction.isPresent()) {
                    transaction.get().rpush(key, value);
                } else {
                    container.rpush(key, value);
                }
                break;
            case LPUSH:
                if (transaction.isPresent()) {
                    transaction.get().lpush(key, value);
                } else {
                    container.lpush(key, value);
                }
                break;
            case SADD:
                if (transaction.isPresent()) {
                    transaction.get().sadd(key, value);
                } else {
                    container.sadd(key, value);
                }
                break;
            case SET:
                if (transaction.isPresent()) {
                    transaction.get().set(key, value);
                } else {
                    container.set(key, value);
                }
                break;
            case PFADD:
                if (transaction.isPresent()) {
                    transaction.get().pfadd(key, value);
                } else {
                    container.pfadd(key, value);
                }
                break;
            case PUBLISH:
                if (transaction.isPresent()) {
                    transaction.get().publish(key, value);
                } else {
                    container.publish(key, value);
                }
                break;
            case ZADD:
                if (transaction.isPresent()) {
                    transaction
                            .get()
                            .zadd(mapper.getCommandDescription().getAdditionalKey(), Double.parseDouble(value), key);
                } else {
                    container.zadd(mapper.getCommandDescription().getAdditionalKey(), value, key);
                }
                break;
            case ZREM:
                if (transaction.isPresent()) {
                    transaction.get().zrem(mapper.getCommandDescription().getAdditionalKey(), key);
                } else {
                    container.zrem(mapper.getCommandDescription().getAdditionalKey(), key);
                }
                break;
            case HSET:
                if (transaction.isPresent()) {
                    transaction.get().hset(mapper.getCommandDescription().getAdditionalKey(), key, value);
                } else {
                    container.hset(
                            mapper.getCommandDescription().getAdditionalKey(), key, value, Integer.MAX_VALUE);
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "[StreamPark] RedisSink:Cannot process such data type: " + command);
        }
    }

    public void expire(String key, int ttl) {
        if (ttl != Integer.MAX_VALUE) {
            getJedis().expire(key, ttl);
        }
    }

    public void close() {
        try {
            container.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close Redis container", e);
        }
    }

    public static RedisContainer getContainer(FlinkJedisConfigBase jedisConfig) {
        GenericObjectPoolConfig<?> genericObjectPoolConfig = new GenericObjectPoolConfig<>();
        genericObjectPoolConfig.setMaxIdle(jedisConfig.getMaxIdle());
        genericObjectPoolConfig.setMaxTotal(jedisConfig.getMaxTotal());
        genericObjectPoolConfig.setMinIdle(jedisConfig.getMinIdle());
        try {
            org.apache.flink.streaming.connectors.redis.common.container.RedisContainer bahirRedisContainer;
            if (jedisConfig instanceof FlinkJedisPoolConfig) {
                FlinkJedisPoolConfig jedisPoolConfig = (FlinkJedisPoolConfig) jedisConfig;
                JedisPool jedisPool =
                        new JedisPool(
                                genericObjectPoolConfig,
                                jedisPoolConfig.getHost(),
                                jedisPoolConfig.getPort(),
                                jedisPoolConfig.getConnectionTimeout(),
                                jedisPoolConfig.getPassword(),
                                jedisPoolConfig.getDatabase());
                bahirRedisContainer =
                        new org.apache.flink.streaming.connectors.redis.common.container.RedisContainer(jedisPool);
            } else {
                FlinkJedisSentinelConfig jedisSentinelConfig = (FlinkJedisSentinelConfig) jedisConfig;
                JedisSentinelPool jedisSentinelPool =
                        new JedisSentinelPool(
                                jedisSentinelConfig.getMasterName(),
                                jedisSentinelConfig.getSentinels(),
                                genericObjectPoolConfig,
                                jedisSentinelConfig.getSoTimeout(),
                                jedisSentinelConfig.getPassword(),
                                jedisSentinelConfig.getDatabase());
                bahirRedisContainer =
                        new org.apache.flink.streaming.connectors.redis.common.container.RedisContainer(
                                jedisSentinelPool);
            }
            RedisContainer redisContainer = new RedisContainer(bahirRedisContainer);
            redisContainer.open();
            return redisContainer;
        } catch (Exception e) {
            LOG.error("RedisSink:Redis has not been properly initialized: ", e);
            throw e;
        }
    }
}
