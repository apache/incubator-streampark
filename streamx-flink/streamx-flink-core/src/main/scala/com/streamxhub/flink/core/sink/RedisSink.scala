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
import com.streamxhub.common.util.{ConfigUtils, Logger, Utils}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}
import org.apache.flink.streaming.connectors.redis.{RedisSink => RSink}
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand._
import org.apache.flink.streaming.connectors.redis.common.container.{RedisContainer => RContainer}
import redis.clients.jedis.exceptions.JedisException
import redis.clients.jedis.{Jedis, JedisPool}

import scala.annotation.meta.param
import scala.collection.mutable.ListBuffer

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

  lazy val config: FlinkJedisConfigBase = {
    val redisConf = ConfigUtils.getConf(ctx.parameter.toMap, REDIS_PREFIX)
    overrideParams.foreach(x => redisConf.put(x._1, x._2))
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

  @Override
  def sink[T](stream: DataStream[T], mapper: Mapper[T], ttl: Int = Int.MaxValue): DataStreamSink[T] = {
    val sinkFun = ttl match {
      case Int.MaxValue => new RSink[T](config, mapper)
      case _ => new RedisSinkFunction[T](config, mapper, ttl)
    }
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  def towPCSink[T](stream: DataStream[T], mapper: Mapper[T], ttl: Int = Int.MaxValue): DataStreamSink[T] = {
    val sinkFun = new Redis2PCSinkFunction[T](config, mapper, ttl)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class Mapper[T](cmd: RedisCommand, key: String, k: T => String, v: T => String) extends RedisMapper[T] with Serializable {

  override def getCommandDescription: RedisCommandDescription = new RedisCommandDescription(cmd, key)

  override def getKeyFromData(r: T): String = k(r)

  override def getValueFromData(r: T): String = v(r)

}

class RedisSinkFunction[R](jedisConfig: FlinkJedisConfigBase, mapper: RedisMapper[R], ttl: Int) extends RSink[R](jedisConfig, mapper) with Logger {

  private[this] var redisContainer: RedisContainer = _

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    redisContainer = RedisContainer.getContainer(jedisConfig)
  }

  override def invoke(input: R, context: SinkFunction.Context[_]): Unit = {
    val key = mapper.getKeyFromData(input)
    val value = mapper.getValueFromData(input)
    mapper.getCommandDescription.getCommand match {
      case RPUSH => this.redisContainer.rpush(key, value)
      case LPUSH => this.redisContainer.lpush(key, value)
      case SADD => this.redisContainer.sadd(key, value)
      case SET => this.redisContainer.set(key, value)
      case PFADD => this.redisContainer.pfadd(key, value)
      case PUBLISH => this.redisContainer.publish(key, value)
      case ZADD => this.redisContainer.zadd(mapper.getCommandDescription.getAdditionalKey, value, key)
      case ZREM => this.redisContainer.zrem(mapper.getCommandDescription.getAdditionalKey, key)
      case HSET => this.redisContainer.hset(mapper.getCommandDescription.getAdditionalKey, key, value)
      case other => throw new IllegalArgumentException("[Streamx] RedisSink:Cannot process such data type: " + other)
    }
    redisContainer.expireAndClose(key, ttl)
  }

  @throws[IOException]
  override def close(): Unit = if (redisContainer != null) redisContainer.close()

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
        logger.error("[Streamx] RedisSink:Redis has not been properly initialized: ", e)
        throw e
    }
  }
}

class RedisContainer(jedisPool: JedisPool) extends RContainer(jedisPool) {

  lazy val jedis: Jedis = jedisPool.getResource

  override def hset(key: String, hashField: String, value: String): Unit = jedis.hset(key, hashField, value)

  override def rpush(key: String, value: String): Unit = jedis.rpush(key, value)

  override def lpush(key: String, value: String): Unit = jedis.lpush(key, value)

  override def sadd(key: String, value: String): Unit = jedis.sadd(key, value)

  override def publish(key: String, message: String): Unit = jedis.publish(key, message)

  override def set(key: String, value: String): Unit = jedis.set(key, value)

  override def pfadd(key: String, element: String): Unit = jedis.pfadd(key, element)

  override def zadd(key: String, score: String, element: String): Unit = jedis.zadd(key, score.toDouble, element)

  override def zrem(key: String, element: String): Unit = jedis.zrem(key, element)

  def expireAndClose(key: String, ttl: Int): Unit = {
    ttl match {
      case Int.MaxValue =>
      case _ => jedis.expire(key, ttl)
    }
    jedis.close()
  }
}


//-----------------------

class Redis2PCSinkFunction[R](jedisConfig: FlinkJedisConfigBase, mapper: Mapper[R], ttl: Int)
  extends TwoPhaseCommitSinkFunction[R, Transaction[R], Void](new KryoSerializer[Transaction[R]](classOf[Transaction[R]], new ExecutionConfig), VoidSerializer.INSTANCE) with Logger {

  private[this] val buffer: collection.mutable.Map[String, Transaction[R]] = collection.mutable.Map.empty[String, Transaction[R]]

  override def beginTransaction(): Transaction[R] = {
    logInfo("[StreamX] Redis2PCSink beginTransaction.")
    Transaction[R]()
  }

  override def invoke(transaction: Transaction[R], value: R, context: SinkFunction.Context[_]): Unit = {
    transaction.invoked = true
    transaction + (mapper, ttl, value)
  }

  override def preCommit(transaction: Transaction[R]): Unit = {
    //防止未调用invoke方法直接调用preCommit
    if (transaction.invoked) {
      logInfo(s"[StreamX] Jdbc2PCSink preCommit.TransactionId:${transaction.transactionId}")
      buffer += transaction.transactionId -> transaction
    }
  }

  override def commit(transaction: Transaction[R]): Unit = {
    if (transaction.invoked && transaction.mapper.nonEmpty) {
      try {
        val redisContainer = RedisContainer.getContainer(jedisConfig)
        val jedisTransaction = redisContainer.jedis.multi()
        transaction.mapper.foreach(x => {
          val mapper = x._1
          val ttl = x._2
          val r = x._3
          val key = mapper.getKeyFromData(r)
          val value = mapper.getValueFromData(r)
          mapper.getCommandDescription.getCommand match {
            case RPUSH => redisContainer.rpush(key, value)
            case LPUSH => redisContainer.lpush(key, value)
            case SADD => redisContainer.sadd(key, value)
            case SET => redisContainer.set(key, value)
            case PFADD => redisContainer.pfadd(key, value)
            case PUBLISH => redisContainer.publish(key, value)
            case ZADD => redisContainer.zadd(mapper.getCommandDescription.getAdditionalKey, value, key)
            case ZREM => redisContainer.zrem(mapper.getCommandDescription.getAdditionalKey, key)
            case HSET => redisContainer.hset(mapper.getCommandDescription.getAdditionalKey, key, value)
            case other => throw new IllegalArgumentException("[Streamx] RedisSink:Cannot process such data type: " + other)
          }
          redisContainer.expireAndClose(key, ttl)
          jedisTransaction.exec()
          //成功,清除state...
          buffer -= transaction.transactionId
        })
      } catch {
        case e: JedisException =>
          logError(s"[StreamX] Redis2PCSink commit JedisException:${e.getMessage}")
          throw e
        case t: Throwable =>
          logError(s"[StreamX] Redis2PCSink commit Throwable:${t.getMessage}")
          throw t
      }
    }
  }

  override def abort(transaction: Transaction[R]): Unit = {
    logInfo(s"[StreamX] Redis2PCSink abort,TransactionId:${transaction.transactionId}")
    buffer -= transaction.transactionId
  }

}


case class Transaction[R](transactionId: String = Utils.uuid(), mapper: ListBuffer[(Mapper[R], Int, R)] = ListBuffer.empty, var invoked: Boolean = false) extends Serializable {
  def +(_mapper: Mapper[R], ttl: Int, r: R): Unit = mapper.add((_mapper, ttl, r))
}


