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
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.redis.common.config.{FlinkJedisConfigBase, FlinkJedisPoolConfig, FlinkJedisSentinelConfig}
import org.apache.flink.streaming.connectors.redis.common.container.{RedisContainer => BahirRedisContainer}
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand._
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper => BahirRedisMapper}
import org.apache.flink.streaming.connectors.redis.{RedisSink => BahirRedisSink}
import redis.clients.jedis.exceptions.JedisException
import redis.clients.jedis.{Jedis, JedisPool, JedisSentinelPool}

import java.io.IOException
import java.lang.reflect.Field
import java.util
import java.util.Properties
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.mutable
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

      case _ => throw throw new IllegalArgumentException(s"redis connectType must be jedisPool|sentinel|cluster $connectType")
    }
  }

  def sink[T](stream: DataStream[T], mapper: RedisMapper[T], ttl: Int = Int.MaxValue): DataStreamSink[T] = {
    val sinkFun = (enableCheckpoint, cpMode) match {
      case (false, CheckpointingMode.EXACTLY_ONCE) => throw new IllegalArgumentException("redis sink EXACTLY_ONCE must enable checkpoint")
      case (true, CheckpointingMode.EXACTLY_ONCE) => new Redis2PCSinkFunction[T](config, mapper, ttl)
      case _ => new RedisSinkFunction[T](config, mapper, ttl)
    }
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}


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

//-------------Redis2PCSinkFunction,端到端精准一次语义实现---------------------------------------------------------------------------------------

class Redis2PCSinkFunction[T](jedisConfig: FlinkJedisConfigBase, mapper: RedisMapper[T], ttl: Int)
  extends TwoPhaseCommitSinkFunction[T, RedisTransaction[T], Void](new KryoSerializer[RedisTransaction[T]](classOf[RedisTransaction[T]], new ExecutionConfig), VoidSerializer.INSTANCE) with Logger {


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
        redisTransaction.mapper.clear()
        //成功,清除state...
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
    transaction.mapper.clear()
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
      case RPUSH => transaction match {
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


case class RedisMapper[T](cmd: RedisCommand, additionalKey: String, key: T => String, value: T => String) extends BahirRedisMapper[T] {

  override def getCommandDescription: RedisCommandDescription = new RedisCommandDescription(cmd, additionalKey)

  override def getKeyFromData(r: T): String = key(r)

  override def getValueFromData(r: T): String = value(r)

}

