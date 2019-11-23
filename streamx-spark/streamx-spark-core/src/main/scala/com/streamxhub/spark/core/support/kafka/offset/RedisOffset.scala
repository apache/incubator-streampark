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

package com.streamxhub.spark.core.support.kafka.offset

import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import com.streamxhub.spark.core.support.redis.RedisClient._

import scala.collection.JavaConversions._

/**
  *
  *
  * Offset 存储到Redis
  */
private[kafka] class RedisOffset(val sparkConf: SparkConf) extends Offset {

  override def get(groupId: String, topics: Set[String]): Map[TopicPartition, Long] = {
    val earliestOffsets = getEarliestOffsets(topics.toSeq)
    val offsetMap = close { redis =>
      topics.flatMap(topic => {
        redis.hgetAll(key(groupId, topic)).map {
          case (partition, offset) =>
            // 如果Offset失效了，则用 earliestOffsets 替代
            val tp = new TopicPartition(topic, partition.toInt)
            val finalOffset = earliestOffsets.get(tp) match {
              case Some(left) if left > offset.toLong =>
                logWarning(s"consumer group:$groupId,topic:${tp.topic},partition:${tp.partition} offsets已经过时，更新为: $left")
                left
              case _ => offset.toLong
            }
            tp -> finalOffset
        }
      })
    }(connect(storeParams))

    // fix bug
    // 如果GroupId 已经在Hbase存在了，这个时候新加一个topic ，则新加的Topic 不会被消费
    val offsetMaps = reset.toLowerCase() match {
      case "largest" => getLatestOffsets(topics.toSeq) ++ offsetMap
      case _ => getEarliestOffsets(topics.toSeq) ++ offsetMap
    }
    logInfo(s"getOffsets [$groupId,${offsetMaps.mkString(",")}] ")
    offsetMaps
  }


  override def update(groupId: String, offsets: Map[TopicPartition, Long]): Unit = {
    close { redis =>
      offsets.foreach { case (tp, offset) => redis.hset(key(groupId, tp.topic), tp.partition().toString, offset.toString) }
    }(connect(storeParams))
    logInfo(s"updateOffsets [ $groupId,${offsets.mkString(",")} ]")
  }

  override def delete(groupId: String, topics: Set[String]): Unit = {
    close { redis =>
      topics.foreach(x => redis.del(key(groupId, x)))
    }(connect(storeParams))
    logInfo(s"delOffsets [ $groupId,${topics.mkString(",")} ]")
  }
}
