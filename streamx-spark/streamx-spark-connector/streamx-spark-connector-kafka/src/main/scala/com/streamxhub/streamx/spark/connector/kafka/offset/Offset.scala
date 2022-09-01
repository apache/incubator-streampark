/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.spark.connector.kafka.offset

import com.streamxhub.streamx.common.util.Logger
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf

import java.util.Properties
import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.Try

/**
 * Offset 管理
 */
trait Offset extends Logger with Serializable {

  val sparkConf: SparkConf

  lazy val storeType: String = storeParams.getOrElse("type", "none")

  lazy implicit val storeParams: Map[String, String] = sparkConf.getAllWithPrefix(s"spark.source.kafka.offset.store.").toMap

  implicit def toProperty(map: Map[String, String]): Properties = {
    require(map != null)
    val prop = new Properties()
    map.foreach((a) => prop.setProperty(a._1, a._2))
    prop
  }

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


  private final val LatestTime = -1L
  private final val EarliestTime = -2L

  /**
   * 获取最旧的Offsets
   *
   * @param topics
   * @return
   */
  def getEarliestOffsets(topics: Seq[String]): Map[TopicPartition, Long] = getOffsets(topics, EarliestTime)

  /**
   * 获取最新的Offset
   *
   * @param topics
   * @return
   */
  def getLatestOffsets(topics: Seq[String]): Map[TopicPartition, Long] = getOffsets(topics, LatestTime)


  /**
   * 获取指定时间的Offset
   * 想
   *
   * @param topics
   * @param time
   * @return
   */
  private def getOffsets(topics: Seq[String], time: Long): Map[TopicPartition, Long] = {

    import org.apache.kafka.clients.consumer.KafkaConsumer
    val props = new Properties()
    props.setProperty("bootstrap.servers", s"${host}:${port}")
    props.setProperty("group.id", s"offsetLookup-${System.currentTimeMillis()}")
    props.setProperty("enable.auto.commit", "false")
    props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    val consumer = new KafkaConsumer[String, String](props)

    val partitions = topics.flatMap(topic => {
      consumer.partitionsFor(topic).asScala.map(x => new TopicPartition(x.topic(), x.partition()))
    })

    val offsetInfos = time match {
      case EarliestTime => consumer.beginningOffsets(partitions.asJavaCollection)
      case LatestTime => consumer.endOffsets(partitions.asJavaCollection)
    }

    Try(consumer.close())

    offsetInfos.asInstanceOf[Map[TopicPartition, Long]]
  }

}
