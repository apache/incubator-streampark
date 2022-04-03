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

package com.streamxhub.streamx.flink.connector.redis.sink

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{ConfigUtils, Utils}
import com.streamxhub.streamx.flink.connector.redis.scala.conf.RedisConfigConst._
import com.streamxhub.streamx.flink.connector.redis.scala.domain.RedisMapper
import com.streamxhub.streamx.flink.connector.redis.scala.internal.{Redis2PCSinkFunction, RedisSinkFunction}
import com.streamxhub.streamx.flink.connector.sink.Sink
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.redis.common.config.{FlinkJedisConfigBase, FlinkJedisPoolConfig, FlinkJedisSentinelConfig}
import org.apache.flink.streaming.api.datastream.{DataStream => JavaDataStream}

import java.lang.reflect.Field
import java.util
import java.util.Properties
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.util.Try

object RedisSink {

  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): RedisSink = new RedisSink(ctx, property, parallelism, name, uid)
}

class RedisSink(@(transient@param) ctx: StreamingContext,
                property: Properties = new Properties(),
                parallelism: Int = 0,
                name: String = null,
                uid: String = null
               ) extends Sink {

  def this(ctx: StreamingContext) {
    this(ctx, new Properties(), 0, null, null)
  }

  val enableCheckpoint: Boolean = ctx.parameter.toMap.getOrElse(KEY_FLINK_CHECKPOINTS_ENABLE, "false").toBoolean

  val cpMode: CheckpointingMode = Try(
    CheckpointingMode.valueOf(ctx.parameter.toMap.get(KEY_FLINK_CHECKPOINTS_MODE))
  ).getOrElse(CheckpointingMode.AT_LEAST_ONCE)


  lazy val config: FlinkJedisConfigBase = {
    val map: util.Map[String, String] = ctx.parameter.toMap
    val redisConf = ConfigUtils.getConf(map, REDIS_PREFIX)
    val connectType: String = Try(redisConf.remove(REDIS_CONNECT_TYPE).toString).getOrElse(DEFAULT_REDIS_CONNECT_TYPE)
    Utils.copyProperties(property, redisConf)

    val host: String = redisConf.remove(KEY_HOST) match {
      case null => throw new IllegalArgumentException("redis host  must not null")
      case hostStr => hostStr.toString
    }

    val port: Int = redisConf.remove(KEY_PORT) match {
      case null => 6379
      case portStr => portStr.toString.toInt
    }

    def setFieldValue(field: Field, targetObject: Any, value: String): Unit = {
      field.setAccessible(true)
      field.getType.getSimpleName match {
        case "String" => field.set(targetObject, value)
        case "int" | "Integer" => field.set(targetObject, value.toInt)
        case "long" | "Long" => field.set(targetObject, value.toLong)
        case "boolean" | "Boolean" => field.set(targetObject, value.toBoolean)
        case _ =>
      }
    }

    connectType match {

      case "sentinel" =>
        val sentinels: Set[String] = host.split(SIGN_COMMA).map(x => {
          if (x.contains(SIGN_COLON)) x; else {
            throw new IllegalArgumentException(s"redis sentinel host invalid {$x} must match host:port ")
          }
        }).toSet
        val builder = new FlinkJedisSentinelConfig.Builder().setSentinels(sentinels)
        redisConf.foreach(x => {
          val field = Try(builder.getClass.getDeclaredField(x._1)).getOrElse {
            throw new IllegalArgumentException(
              s"""
                 |redis config error,property:${x._1} invalid,init FlinkJedisSentinelConfig error, property options:
                 |<String masterName>,
                 |<Set<String> sentinels>,
                 |<int connectionTimeout>,
                 |<int soTimeout>,
                 |<String password>,
                 |<int database>,
                 |<int maxTotal>,
                 |<int maxIdle>,
                 |<int minIdle>
                 |""".stripMargin)
          }
          setFieldValue(field, builder, x._2)
        })
        builder.build()

      case DEFAULT_REDIS_CONNECT_TYPE =>
        val builder: FlinkJedisPoolConfig.Builder = new FlinkJedisPoolConfig.Builder().setHost(host).setPort(port)
        redisConf.foreach(x => {
          val field = Try(builder.getClass.getDeclaredField(x._1)).getOrElse {
            throw new IllegalArgumentException(
              s"""
                 |redis config error,property:${x._1} invalid,init FlinkJedisPoolConfig error,property options:
                 |<String host>,
                 |<int port>,
                 |<int timeout>,
                 |<int database>,
                 |<String password>,
                 |<int maxTotal>,
                 |<int maxIdle>,
                 |<int minIdle>
                 |""".stripMargin)
          }
          setFieldValue(field, builder, x._2)
        })

        builder.build()

      case _ => throw throw new IllegalArgumentException(s"redis connectType must be jedisPool|sentinel $connectType")
    }
  }

  /**
   * scala stream
   *
   * @param stream
   * @param mapper
   * @param ttl
   * @tparam T
   * @return
   */
  def sink[T](stream: DataStream[T], mapper: RedisMapper[T], ttl: Int = Int.MaxValue): DataStreamSink[T] = {
    require(stream != null, () => s"sink Stream must not null")
    require(mapper != null, () => s"redis mapper must not null")
    require(ttl > 0, () => s"redis ttl must more 0")
    val sinkFun = (enableCheckpoint, cpMode) match {
      case (false, CheckpointingMode.EXACTLY_ONCE) => throw new IllegalArgumentException("redis sink EXACTLY_ONCE must enable checkpoint")
      case (true, CheckpointingMode.EXACTLY_ONCE) => new Redis2PCSinkFunction[T](config, mapper, ttl)
      case _ => new RedisSinkFunction[T](config, mapper, ttl)
    }
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  /**
   * java stream
   *
   * @param stream
   * @param mapper
   * @param ttl
   * @tparam T
   * @return
   */
  def sink[T](stream: JavaDataStream[T], mapper: RedisMapper[T], ttl: Int): DataStreamSink[T] = {
    sink(new DataStream[T](stream), mapper, ttl)
  }

  /**
   * java stream
   *
   * @param stream
   * @param mapper
   * @tparam T
   * @return
   */
  def sink[T](stream: JavaDataStream[T], mapper: RedisMapper[T]): DataStreamSink[T] = {
    sink(new DataStream[T](stream), mapper, Int.MaxValue)
  }

}
