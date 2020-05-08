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

import java.io.IOException

import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.redis.common.config.{FlinkJedisConfigBase, FlinkJedisPoolConfig}
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}
import com.streamxhub.flink.core.StreamingContext

import scala.collection.JavaConversions._
import scala.collection.Map
import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.ConfigUtils
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.connectors.redis.{RedisSink => RSink}
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand._
import org.apache.flink.streaming.connectors.redis.common.container.{RedisCommandsContainer, RedisContainer => RContainer}
import redis.clients.jedis.{Jedis, JedisPool}

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
  def sink[T](stream: DataStream[T], ttl: Int = Int.MaxValue)(implicit mapper: RedisMapper[T]): DataStreamSink[T] = {
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
    val sinkFun = ttl match {
      case Int.MaxValue => new RSink[T](builder.build(), mapper)
      case _ => new RedisSinkFunction[T](builder.build(), mapper, ttl)
    }
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

case class Mapper[T](cmd: RedisCommand, key: String, k: T => String, v: T => String) extends RedisMapper[T] {

  override def getCommandDescription: RedisCommandDescription = new RedisCommandDescription(cmd, key)

  override def getKeyFromData(r: T): String = k(r)

  override def getValueFromData(r: T): String = v(r)

}


class RedisSinkFunction[R](jedisConfig: FlinkJedisConfigBase, redisMapper: RedisMapper[R], ttl: Int) extends RSink[R](jedisConfig, redisMapper) {

  private[this] var redisContainer: RedisCommandsContainer = _

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    val jedisPoolConfig = jedisConfig.asInstanceOf[FlinkJedisPoolConfig]
    val genericObjectPoolConfig = new GenericObjectPoolConfig
    genericObjectPoolConfig.setMaxIdle(jedisPoolConfig.getMaxIdle)
    genericObjectPoolConfig.setMaxTotal(jedisPoolConfig.getMaxTotal)
    genericObjectPoolConfig.setMinIdle(jedisPoolConfig.getMinIdle)
    val jedisPool = new JedisPool(
      genericObjectPoolConfig,
      jedisPoolConfig.getHost,
      jedisPoolConfig.getPort,
      jedisPoolConfig.getConnectionTimeout,
      jedisPoolConfig.getPassword,
      jedisPoolConfig.getDatabase
    )
    redisContainer = new RedisContainer(jedisPool, ttl)
  }

  override def invoke(input: R): Unit = {
    val key = redisMapper.getKeyFromData(input)
    val value = redisMapper.getValueFromData(input)
    redisMapper.getCommandDescription.getCommand match {
      case RPUSH => this.redisContainer.rpush(key, value)
      case LPUSH => this.redisContainer.lpush(key, value)
      case SADD => this.redisContainer.sadd(key, value)
      case SET => this.redisContainer.set(key, value)
      case PFADD => this.redisContainer.pfadd(key, value)
      case PUBLISH => this.redisContainer.publish(key, value)
      case ZADD => this.redisContainer.zadd(redisMapper.getCommandDescription.getAdditionalKey, value, key)
      case ZREM => this.redisContainer.zrem(redisMapper.getCommandDescription.getAdditionalKey, key)
      case HSET => this.redisContainer.hset(redisMapper.getCommandDescription.getAdditionalKey, key, value)
      case other => throw new IllegalArgumentException("Cannot process such data type: " + other)
    }
  }

  @throws[IOException]
  override def close(): Unit = if (redisContainer != null) redisContainer.close()

}

class RedisContainer(jedisPool: JedisPool, ttl: Int) extends RContainer(jedisPool) {

  override def hset(key: String, hashField: String, value: String): Unit = doRedis(r => r.hset(key, hashField, value), key)

  override def rpush(key: String, value: String): Unit = doRedis(r => r.rpush(key, value), key)

  override def lpush(key: String, value: String): Unit = doRedis(r => r.lpush(key, value), key)

  override def sadd(key: String, value: String): Unit = doRedis(r => r.sadd(key, value), key)

  override def publish(key: String, message: String): Unit = doRedis(r => r.publish(key, message), key)

  override def set(key: String, value: String): Unit = doRedis(r => r.set(key, value), key)

  override def pfadd(key: String, element: String): Unit = doRedis(r => r.pfadd(key, element), key)

  override def zadd(key: String, score: String, element: String): Unit = doRedis(r => r.zadd(key, score.toDouble, element), key)

  override def zrem(key: String, element: String): Unit = doRedis(r => r.zrem(key, element), key)

  def doRedis(fun: Jedis => Unit, key: String): Unit = {
    val jedis = jedisPool.getResource
    fun(jedis)
    jedis.expire(key, ttl)
    jedis.close()
  }

}

