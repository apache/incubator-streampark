/**
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
package com.streamxhub.flink.core.sink

import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.redis.{RedisSink => RSink}
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}
import com.streamxhub.flink.core.StreamingContext

import scala.collection.JavaConversions._
import scala.collection.Map
import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.ConfigUtils

import scala.annotation.meta.param

object RedisSink {
  def apply(@(transient@param) ctx: StreamingContext,
            overrideParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null): RedisSink = new RedisSink(ctx, overrideParams, parallelism, name, uid)
}

class RedisSink(@(transient@param) ctx: StreamingContext,
                overrideParams: Map[String, String] = Map.empty[String, String],
                parallelism: Int = 0,
                name: String = null,
                uid: String = null
               ) extends Sink {

  @Override
  def sink[T](stream: DataStream[T])(implicit mapper: RedisMapper[T]): DataStreamSink[T] = {
    val builder = new FlinkJedisPoolConfig.Builder()
    val config = ConfigUtils.getConf(ctx.paramMap, REDIS_PREFIX)
    overrideParams.foreach(x => config.put(x._1, x._2))
    config.map {
      case (KEY_HOST, host) => builder.setHost(host)
      case (KEY_PORT, port) => builder.setPort(port.toInt)
      case (KEY_DB, db) => builder.setDatabase(db.toInt)
      case (KEY_PASSWORD, password) => builder.setPassword(password)
      case _ =>
    }
    val sink = stream.addSink(new RSink[T](builder.build(), mapper))
    afterSink(sink, parallelism, name, uid)
  }

}

case class Mapper[T](cmd: RedisCommand, key: String, k: T => String, v: T => String) extends RedisMapper[T] {
  override def getCommandDescription: RedisCommandDescription = new RedisCommandDescription(cmd, key)

  override def getKeyFromData(r: T): String = k(r)

  override def getValueFromData(r: T): String = v(r)
}

