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

package org.apache.streampark.spark.connector.kafka.source

import org.apache.streampark.spark.connector.kafka.offset.KafkaClient
import org.apache.streampark.spark.connector.source.Source

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.{StreamingContext, Time}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}

import java.util.concurrent.ConcurrentHashMap

import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Wrapped Kafka Direct Api
 *
 * @param ssc
 *   StreamingContext
 * @param overrideParams
 *   specific kafka params
 */
class KafkaSource[K: ClassTag, V: ClassTag](
    @transient val ssc: StreamingContext,
    overrideParams: Map[String, String] = Map.empty[String, String])
  extends Source {

  override val prefix: String = "spark.source.kafka.consume."

  // partition nums
  lazy val repartition: Int =
    sparkConf.get("spark.source.kafka.consume.repartition", "0").toInt

  // kafka consume topic
  private lazy val topicSet: Set[String] =
    overrideParams
      .getOrElse("consume.topics", sparkConf.get("spark.source.kafka.consume.topics"))
      .split(",")
      .map(_.trim)
      .toSet

  // assemble kafka params
  private lazy val kafkaParams: Map[String, String] = {
    sparkConf.getAll.flatMap {
      case (k, v) if k.startsWith(prefix) && Try(v.nonEmpty).getOrElse(false) =>
        Some(k.substring(prefix.length) -> v)
      case _ => None
    } toMap
  } ++ overrideParams ++ Map("enable.auto.commit" -> "false")

  lazy val groupId: Option[String] = kafkaParams.get("group.id")

  override type SourceType = ConsumerRecord[K, V]

  val kafkaClient = new KafkaClient(ssc.sparkContext.getConf)

  private lazy val offsetRanges: java.util.Map[Long, Array[OffsetRange]] =
    new ConcurrentHashMap[Long, Array[OffsetRange]]

  /** Get DStream */
  override def getDStream[R: ClassTag](recordHandler: ConsumerRecord[K, V] => R): DStream[R] = {
    val stream =
      kafkaClient.createDirectStream[K, V](ssc, kafkaParams, topicSet)
    stream
      .transform((rdd, time) => {
        offsetRanges.put(time.milliseconds, rdd.asInstanceOf[HasOffsetRanges].offsetRanges)
        rdd
      })
      .map(recordHandler)
  }

  /**
   * Update offset Note that: Must be placed at the end of all logical code, it ensures that the
   * offset is updated only after the action is executed successfully.
   */
  def updateOffset(time: Time): Unit = {
    val milliseconds = time.milliseconds
    if (groupId.isDefined) {
      logInfo(
        s"updateOffset with ${kafkaClient.offsetStoreType} for time $milliseconds offsetRanges: $offsetRanges")
      val offsetRange = offsetRanges.get(milliseconds)
      kafkaClient.updateOffset(groupId.get, offsetRange)
    }
    offsetRanges.remove(milliseconds)
  }

}
