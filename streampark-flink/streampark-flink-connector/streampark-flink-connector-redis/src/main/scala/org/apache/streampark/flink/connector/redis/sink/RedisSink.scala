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

package org.apache.streampark.flink.connector.redis.sink

import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.connector.redis.bean.RedisMapper
import org.apache.streampark.flink.connector.redis.conf.RedisConfig
import org.apache.streampark.flink.connector.redis.internal.{Redis2PCSinkFunction, RedisSinkFunction}
import org.apache.streampark.flink.connector.sink.Sink
import org.apache.streampark.flink.core.scala.StreamingContext
import org.apache.streampark.flink.util.FlinkUtils

import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.datastream.{DataStream => JavaDataStream, DataStreamSink}
import org.apache.flink.streaming.api.environment.ExecutionCheckpointingOptions
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.redis.common.config.{FlinkJedisConfigBase, FlinkJedisPoolConfig, FlinkJedisSentinelConfig}

import java.lang.reflect.Field
import java.util
import java.util.Properties

import scala.annotation.meta.param
import org.apache.streampark.common.util.Implicits._
import scala.util.Try

object RedisSink {

  def apply(
      @(transient @param)
      property: Properties = new Properties(),
      parallelism: Int = 0,
      name: String = null,
      uid: String = null)(implicit ctx: StreamingContext): RedisSink =
    new RedisSink(ctx, property, parallelism, name, uid)
}

class RedisSink(
    @(transient @param) ctx: StreamingContext,
    property: Properties = new Properties(),
    parallelism: Int = 0,
    name: String = null,
    uid: String = null)
  extends Sink {

  def this(ctx: StreamingContext) {
    this(ctx, new Properties(), 0, null, null)
  }

  private val allProperties: util.Map[String, String] = ctx.parameter.toMap
  val prop = ctx.parameter.getProperties
  Utils.copyProperties(property, prop)
  private val redisConfig: RedisConfig = new RedisConfig(prop)
  val enableCheckpoint: Boolean = FlinkUtils.isCheckpointEnabled(allProperties)

  val cpMode: CheckpointingMode = Try(
    CheckpointingMode.valueOf(
      allProperties.get(ExecutionCheckpointingOptions.CHECKPOINTING_MODE.key())))
    .getOrElse(ExecutionCheckpointingOptions.CHECKPOINTING_MODE.defaultValue())

  lazy val config: FlinkJedisConfigBase = {
    val connectType: String = redisConfig.connectType

    val internalProp: Properties = redisConfig.sinkOption.getInternalConfig()

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

    redisConfig.connectType match {
      case "sentinel" =>
        val builder = new FlinkJedisSentinelConfig.Builder().setSentinels(redisConfig.sentinels)
        internalProp.foreach(
          x => {
            val field = Try(builder.getClass.getDeclaredField(x._1)).getOrElse {
              throw new IllegalArgumentException(
                s"""
                   |Redis config error,property:${x._1} invalid,init FlinkJedisSentinelConfig error, property options:
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

      case "jedisPool" =>
        val builder: FlinkJedisPoolConfig.Builder =
          new FlinkJedisPoolConfig.Builder().setHost(redisConfig.host).setPort(redisConfig.port)
        internalProp.foreach(
          x => {
            val field = Try(builder.getClass.getDeclaredField(x._1)).getOrElse {
              throw new IllegalArgumentException(
                s"""
                   |Redis config error,property:${x._1} invalid,init FlinkJedisPoolConfig error,property options:
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
      case _ =>
        throw throw new IllegalArgumentException(
          s"Redis connectType must be jedisPool|sentinel $connectType")
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
  def sink[T](
      stream: DataStream[T],
      mapper: RedisMapper[T],
      ttl: Int = Int.MaxValue): DataStreamSink[T] = {
    require(stream != null, () => s"Sink Stream must not null")
    require(mapper != null, () => s"Redis mapper must not null")
    require(ttl > 0, () => s"Redis ttl must greater than 0")
    val sinkFun = (enableCheckpoint, cpMode) match {
      case (false, CheckpointingMode.EXACTLY_ONCE) =>
        throw new IllegalArgumentException("Redis sink EXACTLY_ONCE must enable checkpoint")
      case (true, CheckpointingMode.EXACTLY_ONCE) =>
        new Redis2PCSinkFunction[T](config, mapper, ttl)
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
