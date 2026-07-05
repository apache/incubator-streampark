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

import org.apache.streampark.flink.connector.function.TransformFunction;

import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;

/** Redis command mapper. */
public class RedisMapper<T>
        implements org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper<T> {

    public static final String INSERT_FAILOVER_TABLE_NULL_HINT = "Redis cmd insert failoverTable must not null";
    public static final String ADDITIONAL_FAILOVER_TABLE_NULL_HINT =
            "Redis additionalKey insert failoverTable must not null";

    private final RedisCommand cmd;
    private final String additionalKey;
    private final TransformFunction<T, String> keyFun;
    private final TransformFunction<T, String> valueFun;

    public RedisMapper(
            RedisCommand cmd,
            String additionalKey,
            TransformFunction<T, String> keyFun,
            TransformFunction<T, String> valueFun) {
        if (cmd == null) {
            throw new IllegalArgumentException(INSERT_FAILOVER_TABLE_NULL_HINT);
        }
        if (additionalKey == null) {
            throw new IllegalArgumentException(ADDITIONAL_FAILOVER_TABLE_NULL_HINT);
        }
        if (keyFun == null) {
            throw new IllegalArgumentException("Redis keyFun insert failoverTable must not null");
        }
        if (valueFun == null) {
            throw new IllegalArgumentException("Redis valueFun insert failoverTable must not null");
        }
        this.cmd = cmd;
        this.additionalKey = additionalKey;
        this.keyFun = keyFun;
        this.valueFun = valueFun;
    }

    public static <T> RedisMapper<T> map(
            RedisCommand cmd,
            String additionalKey,
            TransformFunction<T, String> keyFun,
            TransformFunction<T, String> valueFun) {
        return new RedisMapper<>(cmd, additionalKey, keyFun, valueFun);
    }

    @Override
    public RedisCommandDescription getCommandDescription() {
        return new RedisCommandDescription(cmd, additionalKey);
    }

    @Override
    public String getKeyFromData(T data) {
        return keyFun.transform(data);
    }

    @Override
    public String getValueFromData(T data) {
        return valueFun.transform(data);
    }
}
