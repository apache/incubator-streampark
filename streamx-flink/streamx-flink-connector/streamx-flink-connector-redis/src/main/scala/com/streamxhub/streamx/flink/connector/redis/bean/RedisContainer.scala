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

package com.streamxhub.streamx.flink.connector.redis.bean

import com.streamxhub.streamx.common.util.Logger
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.apache.flink.streaming.connectors.redis.common.config.{FlinkJedisConfigBase, FlinkJedisPoolConfig, FlinkJedisSentinelConfig}
import org.apache.flink.streaming.connectors.redis.common.container.{RedisContainer => BahirRedisContainer}
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand._
import redis.clients.jedis.{Jedis, JedisPool, JedisSentinelPool}

class RedisContainer(container: BahirRedisContainer) {

  def open(): Unit = {
    container.open()
  }

  lazy val jedis: Jedis = {
    val method = container.getClass.getDeclaredMethod("getInstance")
    method.setAccessible(true)
    method.invoke(container).asInstanceOf[Jedis]
  }

  def invoke[T](mapper: RedisMapper[T], input: T, transaction: Option[redis.clients.jedis.Transaction]): Unit = {
    val key = mapper.getKeyFromData(input)
    val value = mapper.getValueFromData(input)
    mapper.getCommandDescription.getCommand match {
      case  RPUSH => transaction match {
        case Some(t) => t.rpush(key, value)
        case _ => this.container.rpush(key, value)
      }
      case LPUSH => transaction match {
        case Some(t) => t.lpush(key, value)
        case _ => this.container.lpush(key, value)
      }
      case SADD => transaction match {
        case Some(t) => t.sadd(key, value)
        case _ => this.container.sadd(key, value)
      }
      case SET => transaction match {
        case Some(t) => t.set(key, value)
        case _ => this.container.set(key, value)
      }
      case PFADD => transaction match {
        case Some(t) => t.pfadd(key, value)
        case _ => this.container.pfadd(key, value)
      }
      case PUBLISH => transaction match {
        case Some(t) => t.publish(key, value)
        case _ => this.container.publish(key, value)
      }
      case ZADD => transaction match {
        case Some(t) => t.zadd(mapper.getCommandDescription.getAdditionalKey, value.toDouble, key)
        case _ => this.container.zadd(mapper.getCommandDescription.getAdditionalKey, value, key)
      }
      case ZREM => transaction match {
        case Some(t) => t.zrem(mapper.getCommandDescription.getAdditionalKey, key)
        case _ => this.container.zrem(mapper.getCommandDescription.getAdditionalKey, key)
      }
      case HSET => transaction match {
        case Some(t) => t.hset(mapper.getCommandDescription.getAdditionalKey, key, value)
        case _ => this.container.hset(mapper.getCommandDescription.getAdditionalKey, key, value)
      }
      case other => throw new IllegalArgumentException("[StreamX] RedisSink:Cannot process such data type: " + other)
    }
  }

  def expire(key: String, ttl: Int): Unit = {
    ttl match {
      case Int.MaxValue =>
      case _ => jedis.expire(key, ttl)
    }
  }

  def close(): Unit = {
    container.close()
  }

}

object RedisContainer extends Logger {

  def getContainer(jedisConfig: FlinkJedisConfigBase): RedisContainer = {
    val genericObjectPoolConfig = new GenericObjectPoolConfig
    genericObjectPoolConfig.setMaxIdle(jedisConfig.getMaxIdle)
    genericObjectPoolConfig.setMaxTotal(jedisConfig.getMaxTotal)
    genericObjectPoolConfig.setMinIdle(jedisConfig.getMinIdle)
    try {
      val bahirRedisContainer = jedisConfig match {
        case jedisPoolConfig: FlinkJedisPoolConfig =>
          val jedisPool = new JedisPool(
            genericObjectPoolConfig,
            jedisPoolConfig.getHost,
            jedisPoolConfig.getPort,
            jedisPoolConfig.getConnectionTimeout,
            jedisPoolConfig.getPassword,
            jedisPoolConfig.getDatabase
          )
          new BahirRedisContainer(jedisPool)
        case _ =>
          val jedisSentinelConfig = jedisConfig.asInstanceOf[FlinkJedisSentinelConfig]
          val jedisSentinelPool = new JedisSentinelPool(jedisSentinelConfig.getMasterName,
            jedisSentinelConfig.getSentinels,
            genericObjectPoolConfig,
            jedisSentinelConfig.getSoTimeout,
            jedisSentinelConfig.getPassword,
            jedisSentinelConfig.getDatabase
          )
          new BahirRedisContainer(jedisSentinelPool)
      }
      val redisContainer = new RedisContainer(bahirRedisContainer)
      redisContainer.open()
      redisContainer
    } catch {
      case e: Exception =>
        logError("RedisSink:Redis has not been properly initialized: ", e)
        throw e
    }
  }

}
