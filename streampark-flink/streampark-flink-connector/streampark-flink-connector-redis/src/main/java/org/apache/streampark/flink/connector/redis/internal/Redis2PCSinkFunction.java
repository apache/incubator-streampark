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
import org.apache.streampark.flink.connector.redis.bean.RedisTransaction;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.base.VoidSerializer;
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Transaction;

import java.util.Optional;

/** Exactly-once Redis sink function using two-phase commit. */
public class Redis2PCSinkFunction<T>
        extends TwoPhaseCommitSinkFunction<T, RedisTransaction<T>, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(Redis2PCSinkFunction.class);

    private final FlinkJedisConfigBase jedisConfig;
    private final RedisMapper<T> mapper;

    @SuppressWarnings("unchecked")
    public Redis2PCSinkFunction(FlinkJedisConfigBase jedisConfig, RedisMapper<T> mapper, int ttl) {
        super(
                new KryoSerializer<>((Class<RedisTransaction<T>>) (Class<?>) RedisTransaction.class, new ExecutionConfig()),
                VoidSerializer.INSTANCE);
        this.jedisConfig = jedisConfig;
        this.mapper = mapper;
        this.defaultTtl = ttl;
    }

    private final int defaultTtl;

    @Override
    protected RedisTransaction<T> beginTransaction() {
        LOG.info("Redis2PCSink beginTransaction.");
        return new RedisTransaction<>();
    }

    @Override
    protected void invoke(RedisTransaction<T> transaction, T value, SinkFunction.Context context) {
        transaction.invoked = true;
        transaction.add(mapper, value, defaultTtl);
    }

    @Override
    protected void preCommit(RedisTransaction<T> transaction) {
        if (transaction.invoked) {
            LOG.info("Redis2PCSink preCommit.TransactionId:{}", transaction.transactionId);
        }
    }

    @Override
    protected void commit(RedisTransaction<T> redisTransaction) {
        if (redisTransaction.invoked && !redisTransaction.mapper.isEmpty()) {
            try {
                RedisContainer redisContainer = RedisContainer.getContainer(jedisConfig);
                Transaction transaction = redisContainer.getJedis().multi();
                for (RedisTransaction.Entry<T> entry : redisTransaction.mapper) {
                    redisContainer.invoke(entry.mapper, entry.value, Optional.of(transaction));
                    String key = entry.mapper.getKeyFromData(entry.value);
                    transaction.expire(key, entry.ttl);
                }
                transaction.exec();
                transaction.close();
                redisContainer.close();
                redisTransaction.mapper.clear();
            } catch (Exception t) {
                LOG.error("Redis2PCSink commit Throwable:{}", t.getMessage());
                throw t;
            }
        }
    }

    @Override
    protected void abort(RedisTransaction<T> transaction) {
        LOG.info("Redis2PCSink abort,TransactionId:{}", transaction.transactionId);
        transaction.mapper.clear();
    }
}
