/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.test.flink.scala.datastream

import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.sink.{RedisMapper, RedisSink}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand
import org.json4s.DefaultFormats

object FlinkRedisSinkApp extends FlinkStreaming {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def handle(): Unit = {

    /**
     * 创造读取数据的源头
     */
    val source = context.addSource(new TestSource)


    // Redis sink..................
    //1)定义 RedisSink
    val sink = RedisSink()
    //2)写Mapper映射
    val personMapper: RedisMapper[TestEntity] = RedisMapper[TestEntity](RedisCommand.HSET, "flink_user", _.userId.toString, _.orderId.toString)

    sink.sink[TestEntity](source, personMapper, 60000000).setParallelism(1)

  }

}
