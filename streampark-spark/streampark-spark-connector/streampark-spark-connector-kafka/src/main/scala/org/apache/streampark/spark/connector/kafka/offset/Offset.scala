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

import org.apache.streampark.common.util.Implicits._
import org.apache.streampark.common.util.Logger

import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf

import java.util.Properties

import scala.language.implicitConversions
import scala.util.Try

/** Offset Manager */
trait Offset extends Logger with Serializable {

  val sparkConf: SparkConf

  lazy val storeType: String = storeParams.getOrElse("type", "none")

  implicit lazy val storeParams: Map[String, String] =
    sparkConf.getAllWithPrefix(s"spark.source.kafka.offset.store.").toMap

  implicit def toProperty(map: Map[String, String]): Properties = {
    require(map != null)
    val prop = new Properties()
    map.foreach((a) => prop.setProperty(a._1, a._2))
    prop
  }

  lazy val reset: String =
    sparkConf.get("spark.source.kafka.consume.auto.offset.reset", "largest")

  lazy val (host, port) = sparkConf
    .get("spark.source.kafka.consume.bootstrap.servers")
    .split(",")
    .head
    .split(":") match {
    case Array(h, p) => (h, p.toInt)
  }

  /**
   * get stored offset
   *
   * @param groupId
   * @param topics
   * @return
   */
  def get(groupId: String, topics: Set[String]): Map[TopicPartition, Long]

  /**
   * update offset
   *
   * @param groupId
   * @param offsetInfos
   */
  def update(groupId: String, offsetInfos: Map[TopicPartition, Long]): Unit

  /**
   * delete offset
   *
   * @param groupId
   * @param topics
   */
  def delete(groupId: String, topics: Set[String]): Unit

  /**
   * generate key
   *
   * @param groupId
   * @param topic
   * @return
   */
  def key(groupId: String, topic: String): String = s"$groupId#$topic"

  final private val LatestTime = -1L
  final private val EarliestTime = -2L

  /**
   * get earliest offset
   *
   * @param topics
   * @return
   */
  def getEarliestOffsets(topics: Seq[String]): Map[TopicPartition, Long] =
    getOffsets(topics, EarliestTime)

  /**
   * get latest offset
   *
   * @param topics
   * @return
   */
  def getLatestOffsets(topics: Seq[String]): Map[TopicPartition, Long] =
    getOffsets(topics, LatestTime)

  /**
   * get specific timestamp offset
   *
   * @param topics
   * @param time
   * @return
   */
  private def getOffsets(topics: Seq[String], time: Long): Map[TopicPartition, Long] = {

    import org.apache.kafka.clients.consumer.KafkaConsumer
    val props = new Properties()
    props.setProperty("bootstrap.servers", s"$host:$port")
    props.setProperty("group.id", s"offsetLookup-${System.currentTimeMillis()}")
    props.setProperty("enable.auto.commit", "false")
    props.setProperty(
      "key.deserializer",
      "org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty(
      "value.deserializer",
      "org.apache.kafka.common.serialization.StringDeserializer")
    val consumer = new KafkaConsumer[String, String](props)

    val partitions = topics.flatMap(topic => {
      consumer
        .partitionsFor(topic)
        .asScala
        .map(x => new TopicPartition(x.topic(), x.partition()))
    })

    val offsetInfos = time match {
      case EarliestTime =>
        consumer.beginningOffsets(partitions.asJavaCollection)
      case LatestTime => consumer.endOffsets(partitions.asJavaCollection)
    }

    Try(consumer.close())

    offsetInfos.asInstanceOf[Map[TopicPartition, Long]]
  }

}
