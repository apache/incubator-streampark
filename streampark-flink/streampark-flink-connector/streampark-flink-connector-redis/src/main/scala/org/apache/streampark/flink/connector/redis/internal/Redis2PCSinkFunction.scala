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
import org.apache.streampark.flink.connector.redis.bean.{RedisContainer, RedisMapper, RedisTransaction}
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase
import redis.clients.jedis.exceptions.JedisException

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
    // Prevent called preCommit directly without called the invoke method
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
      } catch {
        case t: Exception =>
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
