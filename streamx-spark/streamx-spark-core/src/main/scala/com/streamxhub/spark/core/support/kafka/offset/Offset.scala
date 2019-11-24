package com.streamxhub.spark.core.support.kafka.offset

import com.streamxhub.spark.core.util.Logger
import kafka.api.{OffsetRequest, PartitionMetadata, PartitionOffsetRequestInfo, TopicMetadata, TopicMetadataRequest}
import kafka.common.TopicAndPartition
import kafka.consumer.SimpleConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.spark.{SparkConf, SparkException}

import scala.collection.mutable
import scala.util.Try


/**
  * Offset 管理
  */
trait Offset extends Logger with Serializable {

  val sparkConf: SparkConf

  lazy val storeType: String = storeParams.getOrElse("type", "none")

  lazy val storeParams: Map[String, String] = sparkConf.getAllWithPrefix(s"spark.source.kafka.offset.store.").toMap

  lazy val reset: String = sparkConf.get("spark.source.kafka.consume.auto.offset.reset", "largest")

  lazy val (host, port) = sparkConf.get("spark.source.kafka.consume.bootstrap.servers")
    .split(",").head.split(":") match {
    case Array(h, p) => (h, p.toInt)
  }

  /**
    * 获取存储的Offset
    *
    * @param groupId
    * @param topics
    * @return
    */
  def get(groupId: String, topics: Set[String]): Map[TopicPartition, Long]

  /**
    * 更新 Offsets
    *
    * @param groupId
    * @param offsetInfos
    */
  def update(groupId: String, offsetInfos: Map[TopicPartition, Long]): Unit

  /**
    * 删除 Offsets
    *
    * @param groupId
    * @param topics
    */
  def delete(groupId: String, topics: Set[String]): Unit

  /**
    * 生成Key
    *
    * @param groupId
    * @param topic
    * @return
    */
  def key(groupId: String, topic: String): String = s"$groupId#$topic"


  /**
    * 获取最旧的Offsets
    *
    * @param topics
    * @return
    */
  def getEarliestOffsets(topics: Seq[String]): Map[TopicPartition, Long] = getOffsets(topics, OffsetRequest.EarliestTime)

  /**
    * 获取最新的Offset
    *
    * @param topics
    * @return
    */
  def getLatestOffsets(topics: Seq[String]): Map[TopicPartition, Long] = getOffsets(topics, OffsetRequest.LatestTime)


  private def getLeaders(topics: Seq[String]): Map[(String, Int), Seq[TopicPartition]] = {
    val consumer = new SimpleConsumer(host, port, 100000, 64 * 1024, s"leaderLookup-${System.currentTimeMillis()}")
    val req = new TopicMetadataRequest(topics, 0)
    val resp = consumer.send(req)

    val leaderAndTopicPartition = new mutable.HashMap[(String, Int), Seq[TopicPartition]]()

    resp.topicsMetadata.foreach((metadata: TopicMetadata) => {
      val topic = metadata.topic
      metadata.partitionsMetadata.foreach((partition: PartitionMetadata) => {
        partition.leader match {
          case Some(endPoint) =>
            val hp = endPoint.host -> endPoint.port
            leaderAndTopicPartition.get(hp) match {
              case Some(taps) => leaderAndTopicPartition.put(hp, taps :+ new TopicPartition(topic, partition.partitionId))
              case None => leaderAndTopicPartition.put(hp, Seq(new TopicPartition(topic, partition.partitionId)))
            }
          case None => throw new SparkException(s"get topic[$topic] partition[${partition.partitionId}] leader failed")
        }
      })
    })
    Try(consumer.close())
    leaderAndTopicPartition.toMap
  }

  /**
    * 获取指定时间的Offset
    * 想
    *
    * @param topics
    * @param time
    * @return
    */
  private def getOffsets(topics: Seq[String], time: Long): Map[TopicPartition, Long] = {
    val leaders = getLeaders(topics)
    val offsetMap = new mutable.HashMap[TopicPartition, Long]()

    leaders.foreach {
      case ((leaderHost, _port), taps) =>
        val consumer = new SimpleConsumer(leaderHost, _port, 100000, 64 * 1024, s"offsetLookup-${System.currentTimeMillis()}")

        val requestInfo = taps.map(x => TopicAndPartition(x.topic(), x.partition()) -> PartitionOffsetRequestInfo(time, 1)).toMap

        val offsetRequest = new OffsetRequest(requestInfo)

        val offsetResponse = consumer.getOffsetsBefore(offsetRequest)

        if (offsetResponse.hasError) {
          logError(s"[StreamX] get topic offset failed offsetResponse $offsetResponse")
          throw new SparkException(s"get topic offset failed $leaderHost $taps")
        }
        offsetResponse.offsetsGroupedByTopic.values.foreach(partitionToResponse => {
          partitionToResponse.foreach {
            case (tap, por) => offsetMap.put(new TopicPartition(tap.topic, tap.partition), por.offsets.head)
          }
        })
        Try(consumer.close())
    }
    offsetMap.toMap
  }

}