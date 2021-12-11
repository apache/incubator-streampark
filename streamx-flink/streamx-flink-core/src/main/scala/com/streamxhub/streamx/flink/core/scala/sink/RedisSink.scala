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

package com.streamxhub.streamx.flink.core.scala.sink

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{ConfigUtils, Logger, Utils}
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.redis.common.config.{FlinkJedisConfigBase, FlinkJedisPoolConfig}
import org.apache.flink.streaming.connectors.redis.common.container.{RedisContainer => RContainer}
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand._
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper => RMapper}
import org.apache.flink.streaming.connectors.redis.{RedisSink => RSink}
import redis.clients.jedis.exceptions.JedisException
import redis.clients.jedis.{Jedis, JedisPool}

import java.io.IOException
import java.util.Properties
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.mutable

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

  lazy val config: FlinkJedisConfigBase = {
    val redisConf = ConfigUtils.getConf(ctx.parameter.toMap, REDIS_PREFIX)
    Utils.copyProperties(property, redisConf)
    val builder = new FlinkJedisPoolConfig.Builder()
    redisConf.map {
      case (KEY_HOST, host) => builder.setHost(host)
      case (KEY_PORT, port) => builder.setPort(port.toInt)
      case (KEY_DB, db) => builder.setDatabase(db.toInt)
      case (KEY_PASSWORD, password) => builder.setPassword(password)
      case _ =>
    }
    builder.build()
  }

  def sink[T](stream: DataStream[T], mapper: RedisMapper[T], ttl: Int = Int.MaxValue): DataStreamSink[T] = {
    val sinkFun = ttl match {
      case Int.MaxValue => new RSink[T](config, mapper)
      case _ => new RedisSinkFunction[T](config, mapper, ttl)
    }
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  def towPCSink[T](stream: DataStream[T], mapper: RedisMapper[T], ttl: Int = Int.MaxValue): DataStreamSink[T] = {
    val sinkFun = new Redis2PCSinkFunction[T](config, mapper, ttl)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}


class RedisSinkFunction[T](jedisConfig: FlinkJedisConfigBase, mapper: RedisMapper[T], ttl: Int) extends RSink[T](jedisConfig, mapper) with Logger {

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

//-------------Redis2PCSinkFunction,端到端精准一次语义实现---------------------------------------------------------------------------------------

class Redis2PCSinkFunction[T](jedisConfig: FlinkJedisConfigBase, mapper: RedisMapper[T], ttl: Int)
  extends TwoPhaseCommitSinkFunction[T, RedisTransaction[T], Void](new KryoSerializer[RedisTransaction[T]](classOf[RedisTransaction[T]], new ExecutionConfig), VoidSerializer.INSTANCE) with Logger {

  private[this] val buffer: collection.mutable.Map[String, RedisTransaction[T]] = collection.mutable.Map.empty[String, RedisTransaction[T]]

  override def beginTransaction(): RedisTransaction[T] = {
    logInfo("Redis2PCSink beginTransaction.")
    RedisTransaction[T]()
  }

  override def invoke(transaction: RedisTransaction[T], value: T, context: SinkFunction.Context): Unit = {
    transaction.invoked = true
    transaction + (mapper, value, ttl)
  }

  override def preCommit(transaction: RedisTransaction[T]): Unit = {
    //防止未调用invoke方法直接调用preCommit
    if (transaction.invoked) {
      logInfo(s"Redis2PCSink preCommit.TransactionId:${transaction.transactionId}")
      buffer += transaction.transactionId -> transaction
    }
  }

  override def commit(redisTransaction: RedisTransaction[T]): Unit = {
    if (redisTransaction.invoked && redisTransaction.mapper.nonEmpty) {
      try {
        val redisContainer = RedisContainer.getContainer(jedisConfig)
        val transaction = redisContainer.jedis.multi()
        redisTransaction.mapper.foreach(x => {
          redisContainer.invoke[T](x._1, x._2, Some(transaction))
          val key = mapper.getKeyFromData(x._2)
          transaction.expire(key, x._3)
        })
        transaction.exec()
        transaction.close()
        redisContainer.close()
        //成功,清除state...
        buffer -= redisTransaction.transactionId
      } catch {
        case e: JedisException =>
          logError(s"Redis2PCSink commit JedisException:${e.getMessage}")
          throw e
        case t: Throwable =>
          logError(s"Redis2PCSink commit Throwable:${t.getMessage}")
          throw t
      }
    }
  }

  override def abort(transaction: RedisTransaction[T]): Unit = {
    logInfo(s"Redis2PCSink abort,TransactionId:${transaction.transactionId}")
    buffer -= transaction.transactionId
  }

}


case class RedisTransaction[T](
                                transactionId: String = Utils.uuid(),
                                mapper: mutable.MutableList[(RedisMapper[T], T, Int)] = mutable.MutableList.empty[(RedisMapper[T], T, Int)],
                                var invoked: Boolean = false) extends Serializable {
  def +(redisMapper: (RedisMapper[T], T, Int)): Unit = mapper += redisMapper

  override def toString: String = s"(transactionId:$transactionId,size:${mapper.size},invoked:$invoked)"
}


object RedisContainer extends Logger {

  def getContainer(jedisConfig: FlinkJedisConfigBase): RedisContainer = {
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
    try {
      val redisContainer = new RedisContainer(jedisPool)
      redisContainer.open()
      redisContainer
    } catch {
      case e: Exception =>
        logError("RedisSink:Redis has not been properly initialized: ", e)
        throw e
    }
  }

}

class RedisContainer(jedisPool: JedisPool) extends RContainer(jedisPool) {

  lazy val jedis: Jedis = {
    val method = classOf[RContainer].getDeclaredMethod("getInstance")
    method.setAccessible(true)
    method.invoke(this).asInstanceOf[Jedis]
  }

  def invoke[T](mapper: RedisMapper[T], input: T, transaction: Option[redis.clients.jedis.Transaction]): Unit = {
    val key = mapper.getKeyFromData(input)
    val value = mapper.getValueFromData(input)
    mapper.getCommandDescription.getCommand match {
      case RPUSH => transaction match {
        case Some(t) => t.rpush(key, value)
        case _ => this.rpush(key, value)
      }
      case LPUSH => transaction match {
        case Some(t) => t.lpush(key, value)
        case _ => this.lpush(key, value)
      }
      case SADD => transaction match {
        case Some(t) => t.sadd(key, value)
        case _ => this.sadd(key, value)
      }
      case SET => transaction match {
        case Some(t) => t.set(key, value)
        case _ => this.set(key, value)
      }
      case PFADD => transaction match {
        case Some(t) => t.pfadd(key, value)
        case _ => this.pfadd(key, value)
      }
      case PUBLISH => transaction match {
        case Some(t) => t.publish(key, value)
        case _ => this.publish(key, value)
      }
      case ZADD => transaction match {
        case Some(t) => t.zadd(mapper.getCommandDescription.getAdditionalKey, value.toDouble, key)
        case _ => this.zadd(mapper.getCommandDescription.getAdditionalKey, value, key)
      }
      case ZREM => transaction match {
        case Some(t) => t.zrem(mapper.getCommandDescription.getAdditionalKey, key)
        case _ => this.zrem(mapper.getCommandDescription.getAdditionalKey, key)
      }
      case HSET => transaction match {
        case Some(t) => t.hset(mapper.getCommandDescription.getAdditionalKey, key, value)
        case _ => this.hset(mapper.getCommandDescription.getAdditionalKey, key, value)
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

}

case class RedisMapper[T](cmd: RedisCommand, additionalKey: String, key: T => String, value: T => String) extends RMapper[T] with Serializable {

  override def getCommandDescription: RedisCommandDescription = new RedisCommandDescription(cmd, additionalKey)

  override def getKeyFromData(r: T): String = key(r)

  override def getValueFromData(r: T): String = value(r)

}

