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

package org.apache.streampark.flink.connector.redis.internal

import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.connector.redis.bean.{RedisContainer, RedisMapper}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase
import org.apache.flink.streaming.connectors.redis.{RedisSink => BahirRedisSink}

import java.io.IOException

class RedisSinkFunction[T](jedisConfig: FlinkJedisConfigBase, mapper: RedisMapper[T], ttl: Int) extends BahirRedisSink[T](jedisConfig, mapper) with Logger {

  private[this] var redisContainer: RedisContainer = _

  @throws[Exception] override def open(parameters: Configuration): Unit = {
    redisContainer = RedisContainer.getContainer(jedisConfig)
  }

  override def invoke(input: T, context: SinkFunction.Context): Unit = {
    redisContainer.invoke[T](mapper, input, None)
    val key = mapper.getKeyFromData(input)
    redisContainer.expire(key, ttl)
  }

  @throws[IOException] override def close(): Unit = if (redisContainer != null) redisContainer.close()

}
