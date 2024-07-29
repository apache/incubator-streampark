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

package org.apache.streampark.spark.connector.kafka.offset

import org.apache.streampark.common.util.{RedisEndpoint, RedisUtils}
import org.apache.streampark.common.util.Implicits._

import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import redis.clients.jedis.Protocol

import scala.util.Try

/** Redis Offset Manager */
private[kafka] class RedisOffset(val sparkConf: SparkConf) extends Offset {

  implicit private[this] def endpoint(implicit params: Map[String, String]): RedisEndpoint = {
    val host = params.getOrElse("redis.hosts", Protocol.DEFAULT_HOST)
    val port =
      params.getOrElse("redis.port", Protocol.DEFAULT_PORT.toString).toInt
    val auth = Try(params("redis.auth")).getOrElse(null)
    val dbNum =
      params.getOrElse("redis.db", Protocol.DEFAULT_DATABASE.toString).toInt
    val timeout =
      params.getOrElse("redis.timeout", Protocol.DEFAULT_TIMEOUT.toString).toInt
    RedisEndpoint(host, port, auth, dbNum, timeout)
  }

  override def get(groupId: String, topics: Set[String]): Map[TopicPartition, Long] = {
    val earliestOffsets = getEarliestOffsets(topics.toSeq)
    val offsetMap = RedisUtils.doRedis {
      redis =>
        topics.flatMap(topic => {
          redis.hgetAll(key(groupId, topic)).map {
            case (partition, offset) =>
              // if offset invalid, please use earliest offset to instead of
              val tp = new TopicPartition(topic, partition.toInt)
              val finalOffset = earliestOffsets.get(tp) match {
                case Some(left) if left > offset.toLong =>
                  logWarn(
                    s"storeType:Redis,consumer group:$groupId,topic:${tp.topic},partition:${tp.partition} offsets Outdated,updated:$left")
                  left
                case _ => offset.toLong
              }
              tp -> finalOffset
          }
        })
    }

    val offsetMaps = reset.toLowerCase() match {
      case "largest" => getLatestOffsets(topics.toSeq) ++ offsetMap
      case _ => getEarliestOffsets(topics.toSeq) ++ offsetMap
    }
    logInfo(s"getOffsets [$groupId,${offsetMaps.mkString(",")}] ")
    offsetMaps
  }

  override def update(groupId: String, offsets: Map[TopicPartition, Long]): Unit = {
    RedisUtils.doRedis {
      redis =>
        offsets.foreach {
          case (tp, offset) =>
            redis.hset(key(groupId, tp.topic), tp.partition().toString, offset.toString)
        }
    }
    logInfo(s"storeType:Redis,updateOffsets [ $groupId,${offsets.mkString(",")} ]")
  }

  override def delete(groupId: String, topics: Set[String]): Unit = {
    RedisUtils.doRedis(redis => topics.foreach(x => redis.del(key(groupId, x))))
    logInfo(s"storeType:Redis,deleteOffsets [ $groupId,${topics.mkString(",")} ]")
  }

}
