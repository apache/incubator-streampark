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

package com.streamxhub.spark.core.support.kafka.manager

/**
  *
  *
  * 封装 Kafka
  */

import java.lang.reflect.Constructor
import java.{util => ju}

import com.streamxhub.spark.core.util.{Logger, Utils}
import kafka.api._
import kafka.common.TopicAndPartition
import kafka.consumer.SimpleConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext, SparkException}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010._

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.Try

private[kafka] class KafkaManager(val sparkConf: SparkConf) extends Logger with Serializable {


  // 自定义
  private lazy val offsetsManager = {
    sparkConf.get("spark.source.kafka.offset.store.class", "none").trim match {
      case "none" =>
        sparkConf.get("spark.source.kafka.offset.store.type", "none").trim.toLowerCase match {
          case "redis" => new RedisOffsetsManager(sparkConf)
          case "hbase" => new HBaseOffsetsManager(sparkConf)
          case "kafka" => new DefaultOffsetsManager(sparkConf)
          case "none" => new DefaultOffsetsManager(sparkConf)
        }
      case clazz =>

        logInfo(s"Custom offset management class $clazz")
        val constructors = {
          val offsetsManagerClass = Utils.classForName(clazz)
          offsetsManagerClass
            .getConstructors
            .asInstanceOf[Array[Constructor[_ <: SparkConf]]]
        }
        val constructorTakingSparkConf = constructors.find { c =>
          c.getParameterTypes.sameElements(Array(classOf[SparkConf]))
        }
        constructorTakingSparkConf.get.newInstance(sparkConf).asInstanceOf[OffsetsManager]
    }
  }


  def offsetManagerType: String = offsetsManager.storeType


  /**
    * 从Kafka创建一个 InputDStream[ConsumerRecord[K, V]]
    *
    * @param ssc
    * @param kafkaParams
    * @param topics
    * @tparam K
    * @tparam V
    * @return
    */
  def createDirectStream[K: ClassTag, V: ClassTag](ssc: StreamingContext,
                                                   kafkaParams: Map[String, Object],
                                                   topics: Set[String]
                                                  ): InputDStream[ConsumerRecord[K, V]] = {

    var consumerOffsets = Map.empty[TopicPartition, Long]

    kafkaParams.get("group.id") match {
      case Some(groupId) =>

        logInfo(s"createDirectStream witch group.id $groupId topics ${topics.mkString(",")}")

        consumerOffsets = offsetsManager.getOffsets(groupId.toString, topics)

      case _ =>
        logInfo(s"createDirectStream witchout group.id topics ${topics.mkString(",")}")
    }

    if (consumerOffsets.nonEmpty) {
      logInfo(s"read topics ==[$topics]== from offsets ==[$consumerOffsets]==")
      KafkaUtils.createDirectStream[K, V](ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Assign[K, V](consumerOffsets.keys, kafkaParams, consumerOffsets)
      )
    } else {
      KafkaUtils.createDirectStream[K, V](ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[K, V](topics, kafkaParams)
      )
    }

  }

  /**
    *
    * @param sc
    * @param kafkaParams
    * @param offsetRanges
    * @param locationStrategy
    * @tparam K
    * @tparam V
    * @return
    */
  def createRDD[K: ClassTag, V: ClassTag](sc: SparkContext,
                                          kafkaParams: ju.Map[String, Object],
                                          offsetRanges: Array[OffsetRange],
                                          locationStrategy: LocationStrategy): RDD[ConsumerRecord[K, V]] = {
    KafkaUtils.createRDD(sc, kafkaParams, offsetRanges, locationStrategy)
  }


  /**
    * 更新 Offsets
    *
    * @param groupId
    * @param offsets
    */
  def updateOffsets(groupId: String, offsets: Array[OffsetRange]): Unit = {
    val offsetInfos = offsets.map(x => new TopicPartition(x.topic, x.partition) -> x.untilOffset).toMap
    offsetsManager.updateOffsets(groupId, offsetInfos)
  }

}

/**
  * Offset 管理
  */
trait OffsetsManager extends Logger with Serializable {

  val sparkConf: SparkConf

  lazy val storeParams: Map[String, String] = sparkConf
    .getAllWithPrefix(s"spark.source.kafka.offset.store.")
    .toMap

  lazy val storeType: String = storeParams.getOrElse("type", "none")

  lazy val (host, port) = sparkConf.get("spark.source.kafka.consume.bootstrap.servers")
    .split(",").head.split(":") match {
    case Array(h, p) => (h, p.toInt)
  }

  lazy val reset = sparkConf.get("spark.source.kafka.consume.auto.offset.reset", "largest")

  /**
    * 获取存储的Offset
    *
    * @param groupId
    * @param topics
    * @return
    */
  def getOffsets(groupId: String, topics: Set[String]): Map[TopicPartition, Long]

  /**
    * 更新 Offsets
    *
    * @param groupId
    * @param offsetInfos
    */
  def updateOffsets(groupId: String, offsetInfos: Map[TopicPartition, Long]): Unit

  /**
    * 删除 Offsets
    *
    * @param groupId
    * @param topics
    */
  def delOffsets(groupId: String, topics: Set[String]): Unit = {}

  /**
    * 生成Key
    *
    * @param groupId
    * @param topic
    * @return
    */
  def generateKey(groupId: String, topic: String): String = s"$groupId#$topic"


  def getLeaders(topics: Seq[String]): Map[(String, Int), Seq[TopicPartition]] = {
    val consumer = new SimpleConsumer(host, port, 100000, 64 * 1024, s"leaderLookup-${System.currentTimeMillis()}")
    val req = new TopicMetadataRequest(topics, 0)
    val resp = consumer.send(req)

    val leaderAndTopicPartition = new mutable.HashMap[(String, Int), Seq[TopicPartition]]()

    resp.topicsMetadata.foreach((metadata: TopicMetadata) => {
      val topic = metadata.topic
      metadata.partitionsMetadata.foreach((partition: PartitionMetadata) => {
        partition.leader match {
          case Some(endPoint) =>
            val hp = Tuple2(endPoint.host, endPoint.port)
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

  /**
    * 获取指定时间的Offset
    * 想
    *
    * @param topics
    * @param time
    * @return
    */
  def getOffsets(topics: Seq[String], time: Long): Map[TopicPartition, Long] = {
    val leaders = getLeaders(topics)
    val offsetMap = new mutable.HashMap[TopicPartition, Long]()

    leaders.foreach {
      case ((leaderHost, _port), taps) =>
        val consumer = new SimpleConsumer(leaderHost, _port, 100000, 64 * 1024, s"offsetLookup-${System.currentTimeMillis()}")

        val requestInfo = taps.map(x => TopicAndPartition(x.topic(), x.partition()) -> PartitionOffsetRequestInfo(time, 1)).toMap

        val offsetRequest = new OffsetRequest(requestInfo)

        val offsetResponse = consumer.getOffsetsBefore(offsetRequest)

        if (offsetResponse.hasError) {
          println(s"get topic offset failed offsetResponse $offsetResponse")
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
