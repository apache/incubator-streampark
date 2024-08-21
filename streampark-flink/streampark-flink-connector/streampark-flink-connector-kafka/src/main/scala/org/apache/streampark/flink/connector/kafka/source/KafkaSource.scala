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

package org.apache.streampark.flink.connector.kafka.source

import org.apache.streampark.common.conf.ConfigKeys._
import org.apache.streampark.common.util.{ConfigUtils, Utils}
import org.apache.streampark.flink.connector.kafka.bean.KafkaRecord
import org.apache.streampark.flink.core.scala.StreamingContext

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation}
import org.apache.flink.api.java.typeutils.TypeExtractor.getForClass
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, KafkaDeserializationSchema}
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}

import java.io
import java.util.Properties
import java.util.regex.Pattern

import scala.annotation.meta.param
import org.apache.streampark.common.util.Implicits._
import scala.util.{Failure, Success, Try}

object KafkaSource {

  def apply(@(transient @param) property: Properties = new Properties())(implicit
      ctx: StreamingContext): KafkaSource = new KafkaSource(ctx, property)

  def getSource[T: TypeInformation](
      ctx: StreamingContext,
      property: Properties = new Properties(),
      topic: io.Serializable,
      alias: String,
      deserializer: KafkaDeserializationSchema[T],
      strategy: WatermarkStrategy[KafkaRecord[T]]): FlinkKafkaConsumer[KafkaRecord[T]] = {

    val prop = ConfigUtils.getConf(ctx.parameter.toMap, KAFKA_SOURCE_PREFIX + alias)
    Utils.copyProperties(property, prop)
    require(
      prop != null && prop.nonEmpty && prop.exists(
        x => x._1 == KEY_KAFKA_TOPIC || x._1 == KEY_KAFKA_PATTERN))

    // start.form parameter...
    val timestamp = Try(Some(prop(s"$KEY_KAFKA_START_FROM.$KEY_KAFKA_START_FROM_TIMESTAMP").toLong))
      .getOrElse(None)
    val startFrom = StartFrom.startForm(prop)
    require(
      !(timestamp.nonEmpty && startFrom != null),
      s"[StreamPark] start.form timestamp and offset cannot be defined at the same time")

    // topic parameter
    val topicOpt = Try(Some(prop.remove(KEY_KAFKA_TOPIC).toString)).getOrElse(None)
    val regexOpt = Try(Some(prop.remove(KEY_KAFKA_PATTERN).toString)).getOrElse(None)

    val kfkDeserializer = new KafkaDeserializer[T](deserializer)

    val consumer = (topicOpt, regexOpt) match {
      case (Some(_), Some(_)) =>
        throw new IllegalArgumentException(
          "[StreamPark] topic and regex cannot be defined at the same time")
      case (Some(top), _) =>
        val topics = top.split(",|\\s+")
        val topicList = topic match {
          case null => topics.toList
          case x: String => List(x)
          case x: Array[String] => x.toList
          case x: List[String] => x
          case _ =>
            throw new IllegalArgumentException(
              "[StreamPark] topic type must be String(one topic) or List[String](more topic)")
        }
        new FlinkKafkaConsumer(topicList, kfkDeserializer, prop)
      case (_, Some(reg)) =>
        val pattern: Pattern = topic match {
          case null => reg.r.pattern
          case x: String => x.r.pattern
          case _ =>
            throw new IllegalArgumentException(
              "[StreamPark] subscriptionPattern type must be String(regex)")
        }
        val kfkDeserializer = new KafkaDeserializer[T](deserializer)
        new FlinkKafkaConsumer(pattern, kfkDeserializer, prop)
      case _ => null
    }

    val autoCommit = prop.getOrElse(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true").toBoolean
    (ctx.getCheckpointConfig.isCheckpointingEnabled, autoCommit) match {
      case (true, _) => consumer.setCommitOffsetsOnCheckpoints(true)
      case (_, false) =>
        throw new IllegalArgumentException(
          "[StreamPark] error:flink checkpoint was disable,and kafka autoCommit was false.you can enable checkpoint or enable kafka autoCommit...")
      case _ =>
    }

    if (strategy != null) {
      val assignerWithPeriodicWatermarks =
        consumer.getClass.getMethod("assignTimestampsAndWatermarks", classOf[WatermarkStrategy[T]])
      assignerWithPeriodicWatermarks.setAccessible(true)
      assignerWithPeriodicWatermarks.invoke(consumer, strategy)
    }

    (timestamp, startFrom) match {
      // Globally set Timestamp, effective for all topics.
      case (Some(t), _) => consumer.setStartFromTimestamp(t)
      // Specify the offset for each topic and partition
      case _ =>
        val startFroms = (topicOpt, regexOpt) match {
          case (Some(top), _) =>
            topic match {
              case null => startFrom.toList
              case x: String => startFrom.filter(_.topic == x).toList
              case x: Array[_] =>
                val topics = if (topic == null) top.split(",|\\s+").toList else x.toList
                startFrom.filter(s => topics.contains(s.topic)).toList
              case x: List[_] =>
                val topics = if (topic == null) top.split(",|\\s+").toList else x
                startFrom.filter(s => topics.contains(s.topic)).toList
              case _ => List.empty[StartFrom]
            }
          case (_, Some(reg)) =>
            topic match {
              case null => startFrom.filter(s => reg.r.findFirstIn(s.topic).nonEmpty).toList
              case x: String => startFrom.filter(s => x.r.findFirstIn(s.topic).nonEmpty).toList
              case _ => List.empty[StartFrom]
            }
          case _ => null
        }

        // startOffsets...
        val startOffsets = new java.util.HashMap[KafkaTopicPartition, java.lang.Long]()
        startFroms
          .filter(x => x != null && x.partitionOffset != null)
          .foreach(
            start => {
              start.partitionOffset.foreach(
                x => startOffsets.put(new KafkaTopicPartition(start.topic, x._1), x._2))
            })

        if (startOffsets.nonEmpty) {
          consumer.setStartFromSpecificOffsets(startOffsets)
        }
    }
    consumer
  }

}

class KafkaSource(
    @(transient @param) private[this] val ctx: StreamingContext,
    property: Properties = new Properties()) {

  /**
   * commit offset method:<br/> &nbsp;&nbsp;Flink kafka consumer commit offset method needs to
   * distinguish whether checkpoint is enabled. <br/> &nbsp;&nbsp; 1) Checkpoint off: commit offset
   * depends on auto commit of kafka client. Need to set enable.auto.commit, auto.commit.interval.ms
   * parameters to consumer properties, It will periodically auto commit offset to kafka at regular
   * intervals. <br/> &nbsp;&nbsp; 2) Checkpoint is enabled: At this time, the offset consumed by
   * the job is managed and fault-tolerant by Flink in the state. Submitting offsets to kafka at
   * this time is generally used as an external progress monitor. I want to know the location and
   * lag of job consumption in real time. At this point, setCommitOffsetsOnCheckpoints needs to be
   * true to set the offset to be submitted to kafka when the checkpoint is successful. At this
   * time, the interval of commit offset depends on the interval of checkpoint
   *
   * Get DStream stream
   *
   * @param topic
   *   a group of topics or a single topic
   * @param alias
   *   Aliases to distinguish different kafka connection instances
   * @param deserializer
   *   DeserializationSchema
   * @param strategy
   *   Watermarks strategy
   * @tparam T
   */
  def getDataStream[T: TypeInformation](
      topic: java.io.Serializable = null,
      alias: String = "",
      deserializer: KafkaDeserializationSchema[T] =
        new KafkaStringDeserializationSchema().asInstanceOf[KafkaDeserializationSchema[T]],
      strategy: WatermarkStrategy[KafkaRecord[T]] = null): DataStream[KafkaRecord[T]] = {

    val consumer =
      KafkaSource.getSource[T](this.ctx, property, topic, alias, deserializer, strategy)
    ctx.addSource(consumer)
  }

}

class KafkaDeserializer[T: TypeInformation](deserializer: KafkaDeserializationSchema[T])
  extends KafkaDeserializationSchema[KafkaRecord[T]] {

  override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): KafkaRecord[T] = {
    val key = if (record.key() == null) null else new String(record.key(), "UTF-8")
    val value = deserializer.deserialize(record)
    val offset = record.offset()
    val partition = record.partition()
    val topic = record.topic()
    val timestamp = record.timestamp()
    new KafkaRecord[T](topic, partition, timestamp, offset, key, value)
  }

  override def getProducedType: TypeInformation[KafkaRecord[T]] = getForClass(
    classOf[KafkaRecord[T]])

  override def isEndOfStream(nextElement: KafkaRecord[T]): Boolean = false

}

class KafkaStringDeserializationSchema extends KafkaDeserializationSchema[String] {

  override def isEndOfStream(nextElement: String): Boolean = false

  override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): String =
    new String(record.value(), "UTF-8")

  override def getProducedType: TypeInformation[String] = BasicTypeInfo.STRING_TYPE_INFO

}

object StartFrom {

  def startForm(prop: Properties): Array[StartFrom] = {
    val startProp = prop.filter(_._1.startsWith(KEY_KAFKA_START_FROM))
    startProp.foreach(x => prop.remove(x._1))
    val topic = Try(
      startProp(s"$KEY_KAFKA_START_FROM.$KEY_KAFKA_START_FROM_OFFSET.$KEY_KAFKA_TOPIC").split(","))
      .getOrElse(Array.empty[String])
    if (topic.isEmpty) Array.empty[StartFrom]
    else {
      topic
        .map(
          x => {
            val offset =
              Try(Some(startProp(s"$KEY_KAFKA_START_FROM.$KEY_KAFKA_START_FROM_OFFSET.$x")))
                .getOrElse(None)
            offset match {
              case Some(o) =>
                Try(
                  o.split(",")
                    .map(
                      x => {
                        val array = x.split(":")
                        array.head.toInt -> array.last.toLong
                      })) match {
                  case Success(v) => new StartFrom(x, v)
                  case Failure(_) =>
                    throw new IllegalArgumentException(
                      s"[StreamPark] topic:$x start.form offset error, e.g: 1:10000,2:10000,3:10002")
                }
              case _ => null
            }
          })
        .filter(_ != null)
    }
  }

}

class StartFrom(val topic: String, val partitionOffset: Array[(Int, Long)])
