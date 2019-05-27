package com.streamxhub.spark.core.support.kafka.manager

import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import com.streamxhub.spark.core.support.redis.RedisClient._

import scala.collection.JavaConversions._

/**
  *
  *
  * Offset 存储到Redis
  */
private[kafka] class RedisOffsetsManager(val sparkConf: SparkConf) extends OffsetsManager {

  override def getOffsets(groupId: String, topics: Set[String]): Map[TopicPartition, Long] = {

    val earliestOffsets = getEarliestOffsets(topics.toSeq)


    val offsetMap = close { jedis =>
      topics.flatMap(topic => {
        jedis.hgetAll(generateKey(groupId, topic)).map {
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


  override def updateOffsets(groupId: String, offsetInfos: Map[TopicPartition, Long]): Unit = {
    close { jedis =>
      for ((tp, offset) <- offsetInfos) {
        jedis.hset(generateKey(groupId, tp.topic), tp.partition().toString, offset.toString)
      }
    }(connect(storeParams))
    logInfo(s"updateOffsets [ $groupId,${offsetInfos.mkString(",")} ]")
  }

  override def delOffsets(groupId: String, topics: Set[String]): Unit = {
    close { jedis =>
      for (topic <- topics) {
        jedis.del(generateKey(groupId, topic))
      }
    }(connect(storeParams))
    logInfo(s"delOffsets [ $groupId,${topics.mkString(",")} ]")
  }
}
