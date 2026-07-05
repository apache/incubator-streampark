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

package org.apache.streampark.flink.connector.redis.sink;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.redis.bean.RedisMapper;
import org.apache.streampark.flink.connector.redis.conf.RedisConfig;
import org.apache.streampark.flink.connector.redis.internal.Redis2PCSinkFunction;
import org.apache.streampark.flink.connector.redis.internal.RedisSinkFunction;
import org.apache.streampark.flink.connector.sink.Sink;
import org.apache.streampark.flink.core.scala.StreamingContext;
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.environment.ExecutionCheckpointingOptions;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisSentinelConfig;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

/** Redis sink connector. */
public class RedisSink implements Sink {

    private final StreamingContext ctx;
    private final Properties property;
    private final int parallelism;
    private final String name;
    private final String uid;
    private final Map<String, String> allProperties;
    private final Properties prop;
    private final RedisConfig redisConfig;
    private final boolean enableCheckpoint;
    private final CheckpointingMode cpMode;
    private final FlinkJedisConfigBase config;

    public RedisSink(StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
        this.parallelism = parallelism;
        this.name = name;
        this.uid = uid;
        this.allProperties = ctx.parameter.toMap();
        this.prop = Utils.toProperties(ctx.parameter.toMap());
        Utils.copyProperties(this.property, prop);
        this.redisConfig = new RedisConfig(prop);
        this.enableCheckpoint = FlinkUtils.isCheckpointEnabled(allProperties);
        Object cpModeVal = allProperties.get(ExecutionCheckpointingOptions.CHECKPOINTING_MODE.key());
        if (cpModeVal != null) {
            this.cpMode = CheckpointingMode.valueOf(cpModeVal.toString());
        } else {
            this.cpMode = ExecutionCheckpointingOptions.CHECKPOINTING_MODE.defaultValue();
        }
        this.config = buildConfig();
    }

    public static RedisSink of(StreamingContext ctx, Properties property, int parallelism, String name, String uid) {
        return new RedisSink(ctx, property, parallelism, name, uid);
    }

    private FlinkJedisConfigBase buildConfig() {
        Properties internalProp = redisConfig.sinkOption.getInternalConfig();
        String connectType = redisConfig.connectType;
        if ("sentinel".equals(connectType)) {
            FlinkJedisSentinelConfig.Builder builder =
                    new FlinkJedisSentinelConfig.Builder().setSentinels(redisConfig.sentinels);
            for (Map.Entry<Object, Object> entry : internalProp.entrySet()) {
                setFieldValue(builder, entry.getKey().toString(), entry.getValue().toString(), "sentinel");
            }
            return builder.build();
        } else if ("jedisPool".equals(connectType)) {
            FlinkJedisPoolConfig.Builder builder =
                    new FlinkJedisPoolConfig.Builder().setHost(redisConfig.host).setPort(redisConfig.port);
            for (Map.Entry<Object, Object> entry : internalProp.entrySet()) {
                setFieldValue(builder, entry.getKey().toString(), entry.getValue().toString(), "jedisPool");
            }
            return builder.build();
        }
        throw new IllegalArgumentException("Redis connectType must be jedisPool|sentinel " + connectType);
    }

    private void setFieldValue(Object targetObject, String fieldName, String value, String connectType) {
        try {
            Field field = targetObject.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            String simpleName = field.getType().getSimpleName();
            switch (simpleName) {
                case "String":
                    field.set(targetObject, value);
                    break;
                case "int":
                case "Integer":
                    field.set(targetObject, Integer.parseInt(value));
                    break;
                case "long":
                case "Long":
                    field.set(targetObject, Long.parseLong(value));
                    break;
                case "boolean":
                case "Boolean":
                    field.set(targetObject, Boolean.parseBoolean(value));
                    break;
                default:
                    break;
            }
        } catch (NoSuchFieldException e) {
            if ("sentinel".equals(connectType)) {
                throw new IllegalArgumentException(
                        "Redis config error,property:"
                                + fieldName
                                + " invalid,init FlinkJedisSentinelConfig error, property options:\n"
                                + "<String masterName>,\n"
                                + "<Set<String> sentinels>,\n"
                                + "<int connectionTimeout>,\n"
                                + "<int soTimeout>,\n"
                                + "<String password>,\n"
                                + "<int database>,\n"
                                + "<int maxTotal>,\n"
                                + "<int maxIdle>,\n"
                                + "<int minIdle>");
            }
            throw new IllegalArgumentException(
                    "Redis config error,property:"
                            + fieldName
                            + " invalid,init FlinkJedisPoolConfig error,property options:\n"
                            + "<String host>,\n"
                            + "<int port>,\n"
                            + "<int timeout>,\n"
                            + "<int database>,\n"
                            + "<String password>,\n"
                            + "<int maxTotal>,\n"
                            + "<int maxIdle>,\n"
                            + "<int minIdle>");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set Redis config field: " + fieldName, e);
        }
    }

    public <T> DataStreamSink<T> sink(DataStream<T> stream, RedisMapper<T> mapper, int ttl) {
        if (stream == null) {
            throw new IllegalArgumentException("Sink Stream must not null");
        }
        if (mapper == null) {
            throw new IllegalArgumentException("Redis mapper must not null");
        }
        if (ttl <= 0) {
            throw new IllegalArgumentException("Redis ttl must greater than 0");
        }
        org.apache.flink.streaming.api.functions.sink.SinkFunction<T> sinkFun;
        if (!enableCheckpoint && CheckpointingMode.EXACTLY_ONCE.equals(cpMode)) {
            throw new IllegalArgumentException("Redis sink EXACTLY_ONCE must enable checkpoint");
        } else if (enableCheckpoint && CheckpointingMode.EXACTLY_ONCE.equals(cpMode)) {
            sinkFun = new Redis2PCSinkFunction<>(config, mapper, ttl);
        } else {
            sinkFun = new RedisSinkFunction<>(config, mapper, ttl);
        }
        return afterSink(stream.addSink(sinkFun), parallelism, name, uid);
    }

    public <T> DataStreamSink<T> sink(DataStream<T> stream, RedisMapper<T> mapper) {
        return sink(stream, mapper, Integer.MAX_VALUE);
    }
}
