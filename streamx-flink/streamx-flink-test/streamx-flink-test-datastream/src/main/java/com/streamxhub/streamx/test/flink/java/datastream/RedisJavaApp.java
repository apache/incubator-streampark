/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.test.flink.java.datastream;

import com.streamxhub.streamx.flink.core.StreamEnvConfig;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.test.flink.scala.datastream.TestEntity;
import com.streamxhub.streamx.test.flink.scala.datastream.TestSource;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;

/**
 * @author wudi
 **/
public class RedisJavaApp {

    public static void main(String[] args) {
        StreamEnvConfig envConfig = new StreamEnvConfig(args, null);
        StreamingContext context = new StreamingContext(envConfig);
        DataStreamSource<TestEntity> source = context.getJavaEnv().addSource(new TestSource());
        //1)定义 RedisSink
        RedisSink sink = new RedisSink(context);
        //2)写Mapper映射
        RedisMapper<TestEntity> flink_user = RedisMapper.builderJavaRedisMapper(RedisCommand.HSET, "flink_user", (TestEntity x) -> x.userId() + "", (TestEntity x) -> x.userId() + "");
        sink.sink(source, flink_user, 60000000).setParallelism(1);
        context.start();
    }
}
