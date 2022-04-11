/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.connector.kafka.source

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{ConfigUtils, Utils}
import com.streamxhub.streamx.flink.connector.kafka.bean.KafkaRecord
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation}
import org.apache.flink.api.java.typeutils.TypeExtractor.getForClass
import org.apache.flink.streaming.api.scala.{DataStream, _}
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, KafkaDeserializationSchema}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}

import java.io
import java.util.Properties
import java.util.regex.Pattern
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

object KafkaSource {

  def apply(@(transient@param) property: Properties = new Properties())(implicit ctx: StreamingContext): KafkaSource = new KafkaSource(ctx, property)

  def getSource[T: TypeInformation](ctx: StreamingContext,
                                    property: Properties = new Properties(),
                                    topic: io.Serializable,
                                    alias: String,
                                    deserializer: KafkaDeserializationSchema[T],
                                    strategy: WatermarkStrategy[KafkaRecord[T]]
                                   ): FlinkKafkaConsumer[KafkaRecord[T]] = {

    val prop = ConfigUtils.getConf(ctx.parameter.toMap, KAFKA_SOURCE_PREFIX + alias)
    Utils.copyProperties(property, prop)
    require(prop != null && prop.nonEmpty && prop.exists(x => x._1 == KEY_KAFKA_TOPIC || x._1 == KEY_KAFKA_PATTERN))

    //start.form parameter...
    val timestamp = Try(Some(prop(s"$KEY_KAFKA_START_FROM.$KEY_KAFKA_START_FROM_TIMESTAMP").toLong)).getOrElse(None)
    val startFrom = StartFrom.startForm(prop)
    require(!(timestamp.nonEmpty && startFrom != null), s"[StreamX] start.form timestamp and offset cannot be defined at the same time")

    //topic parameter
    val topicOpt = Try(Some(prop.remove(KEY_KAFKA_TOPIC).toString)).getOrElse(None)
    val regexOpt = Try(Some(prop.remove(KEY_KAFKA_PATTERN).toString)).getOrElse(None)

    val kfkDeserializer = new KafkaDeserializer[T](deserializer)

    val consumer = (topicOpt, regexOpt) match {
      case (Some(_), Some(_)) =>
        throw new IllegalArgumentException("[StreamX] topic and regex cannot be defined at the same time")
      case (Some(top), _) =>
        val topics = top.split(",|\\s+")
        val topicList = topic match {
          case null => topics.toList
          case x: String => List(x)
          case x: Array[String] => x.toList
          case x: List[String] => x
          case _ => throw new IllegalArgumentException("[StreamX] topic type must be String(one topic) or List[String](more topic)")
        }
        new FlinkKafkaConsumer(topicList, kfkDeserializer, prop)
      case (_, Some(reg)) =>
        val pattern: Pattern = topic match {
          case null => reg.r.pattern
          case x: String => x.r.pattern
          case _ => throw new IllegalArgumentException("[StreamX] subscriptionPattern type must be String(regex)")
        }
        val kfkDeserializer = new KafkaDeserializer[T](deserializer)
        new FlinkKafkaConsumer(pattern, kfkDeserializer, prop)
      case _ => null
    }

    val autoCommit = prop.getOrElse(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true").toBoolean
    (ctx.getCheckpointConfig.isCheckpointingEnabled, autoCommit) match {
      case (true, _) => consumer.setCommitOffsetsOnCheckpoints(true)
      case (_, false) => throw new IllegalArgumentException("[StreamX] error:flink checkpoint was disable,and kafka autoCommit was false.you can enable checkpoint or enable kafka autoCommit...")
      case _ =>
    }

    if (strategy != null) {
      val assignerWithPeriodicWatermarks = consumer.getClass.getMethod("assignTimestampsAndWatermarks", classOf[WatermarkStrategy[T]])
      assignerWithPeriodicWatermarks.setAccessible(true)
      assignerWithPeriodicWatermarks.invoke(consumer, strategy)
    }

    (timestamp, startFrom) match {
      //全局设定Timestamp,对所有的topic生效.
      case (Some(t), _) => consumer.setStartFromTimestamp(t)
      //精确为每个topic,partition指定offset
      case _ =>
        val startFroms = (topicOpt, regexOpt) match {
          //topic方式...
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

        //startOffsets...
        val startOffsets = new java.util.HashMap[KafkaTopicPartition, java.lang.Long]()
        startFroms.filter(x => x != null && x.partitionOffset != null).foreach(start => {
          start.partitionOffset.foreach(x => startOffsets.put(new KafkaTopicPartition(start.topic, x._1), x._2))
        })

        if (startOffsets.nonEmpty) {
          consumer.setStartFromSpecificOffsets(startOffsets)
        }
    }
    consumer
  }

}

/*
 * @param ctx
 * @param property
 */
class KafkaSource(@(transient@param) private[this] val ctx: StreamingContext, property: Properties = new Properties()) {
  /**
    *
    * commit offset 方式:<br/>
    * &nbsp;&nbsp;Flink kafka consumer commit offset 方式需要区分是否开启了 checkpoint。<br/>
    * &nbsp;&nbsp; 1) checkpoint 关闭: commit offset 要依赖于 kafka 客户端的 auto commit。
    * 需设置 enable.auto.commit，auto.commit.interval.ms 参数到 consumer properties，
    * 就会按固定的时间间隔定期 auto commit offset 到 kafka。<br/>
    * &nbsp;&nbsp; 2) checkpoint 开启: 这个时候作业消费的 offset 是 Flink 在 state 中自己管理和容错。
    * 此时提交 offset 到 kafka，一般都是作为外部进度的监控，想实时知道作业消费的位置和 lag 情况。
    * 此时需要 setCommitOffsetsOnCheckpoints 为 true 来设置当 checkpoint 成功时提交 offset 到 kafka。
    * 此时 commit offset 的间隔就取决于 checkpoint 的间隔
    *
    * 获取DStream 流
    *
    * @param topic        一组topic或者单个topic
    * @param alias        别名,区分不同的kafka连接实例
    * @param deserializer DeserializationSchema
    * @param strategy     Watermarks 策略
    * @tparam T
    */
  def getDataStream[T: TypeInformation](topic: java.io.Serializable = null,
                                        alias: String = "",
                                        deserializer: KafkaDeserializationSchema[T] = new KafkaStringDeserializationSchema().asInstanceOf[KafkaDeserializationSchema[T]],
                                        strategy: WatermarkStrategy[KafkaRecord[T]] = null
                                       ): DataStream[KafkaRecord[T]] = {

    val consumer = KafkaSource.getSource[T](this.ctx, property, topic, alias, deserializer, strategy)
    ctx.addSource(consumer)
  }

}




class KafkaDeserializer[T: TypeInformation](deserializer: KafkaDeserializationSchema[T]) extends KafkaDeserializationSchema[KafkaRecord[T]] {

  override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): KafkaRecord[T] = {
    val key = if (record.key() == null) null else new String(record.key(), "UTF-8")
    val value = deserializer.deserialize(record)
    val offset = record.offset()
    val partition = record.partition()
    val topic = record.topic()
    val timestamp = record.timestamp()
    new KafkaRecord[T](topic, partition, timestamp, offset, key, value)
  }

  override def getProducedType: TypeInformation[KafkaRecord[T]] = getForClass(classOf[KafkaRecord[T]])

  override def isEndOfStream(nextElement: KafkaRecord[T]): Boolean = false

}

class KafkaStringDeserializationSchema extends KafkaDeserializationSchema[String] {

  override def isEndOfStream(nextElement: String): Boolean = false

  override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): String = new String(record.value(), "UTF-8")

  override def getProducedType: TypeInformation[String] = BasicTypeInfo.STRING_TYPE_INFO

}


object StartFrom {

  def startForm(prop: Properties): Array[StartFrom] = {
    val startProp = prop.filter(_._1.startsWith(KEY_KAFKA_START_FROM))
    startProp.foreach(x => prop.remove(x._1))
    val topic = Try(startProp(s"$KEY_KAFKA_START_FROM.$KEY_KAFKA_START_FROM_OFFSET.$KEY_KAFKA_TOPIC").split(",")).getOrElse(Array.empty[String])
    if (topic.isEmpty) Array.empty[StartFrom] else {
      topic.map(x => {
        val offset = Try(Some(startProp(s"$KEY_KAFKA_START_FROM.$KEY_KAFKA_START_FROM_OFFSET.$x"))).getOrElse(None)
        offset match {
          case Some(o) =>
            Try(
              o.split(",").map(x => {
                val array = x.split(":")
                array.head.toInt -> array.last.toLong
              })
            ) match {
              case Success(v) => new StartFrom(x, v)
              case Failure(_) => throw new IllegalArgumentException(s"[StreamX] topic:$x start.form offset error, e.g: 1:10000,2:10000,3:10002")
            }
          case _ => null
        }
      }).filter(_ != null)
    }
  }

}

class StartFrom(val topic: String, val partitionOffset: Array[(Int, Long)])
