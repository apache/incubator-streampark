/*
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
package com.streamxhub.streamx.flink.core.scala.sink

import com.streamxhub.streamx.common.conf.ConfigConst
import com.streamxhub.streamx.common.util.{ConfigUtils, Logger, Utils}
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.serialization.{SerializationSchema, SimpleStringSchema}
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.{DEFAULT_KAFKA_PRODUCERS_POOL_SIZE, Semantic}
import org.apache.flink.streaming.connectors.kafka.internals.KeyedSerializationSchemaWrapper
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner

import java.util.concurrent.atomic.AtomicInteger
import java.util.{Optional, Properties}
import javax.annotation.Nullable
import scala.annotation.meta.param
import scala.util.Try

object KafkaSink {
  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): KafkaSink = new KafkaSink(ctx, property, parallelism, name, uid)
}

class KafkaSink(@(transient@param) val ctx: StreamingContext,
                property: Properties = new Properties(),
                parallelism: Int = 0,
                name: String = null,
                uid: String = null) extends Sink {

  /**
   * for scala
   *
   * @param stream
   * @param alias
   * @param topic
   * @param serializer 序列化Scheam,不指定默认使用SimpleStringSchema
   * @param partitioner         指定kafka分区器(默认使用<b>KafkaEqualityPartitioner</b>分区器,顾名思义,该分区器可以均匀的将数据写到各个分区中去,
   *                            注意:Flink中默认使用的是<span style="color:RED">FlinkFixedPartitioner</span>分区器,该分区器需要特别注意sink的并行度和kafka的分区数,不然会出现往一个分区写...
   *                            )
   * @tparam T
   * @return
   */
  def sink[T](stream: DataStream[T],
              alias: String = "",
              topic: String = "",
              serializer: SerializationSchema[T] = new SimpleStringSchema().asInstanceOf[SerializationSchema[T]],
              partitioner: FlinkKafkaPartitioner[T] = new KafkaEqualityPartitioner[T](ctx.getParallelism)): DataStreamSink[T] = {

    val producer = {
      val prop = ConfigUtils.getKafkaSinkConf(ctx.parameter.toMap, topic, alias)
      Utils.copyProperties(property, prop)
      val topicId = prop.remove(ConfigConst.KEY_KAFKA_TOPIC).toString
      /**
       * EXACTLY_ONCE语义下会使用到 kafkaProducersPoolSize
       */
      val semantic = Try(Some(prop.remove(ConfigConst.KEY_KAFKA_SEMANTIC).toString.toUpperCase)).getOrElse(None) match {
        case None => Semantic.AT_LEAST_ONCE //默认采用AT_LEAST_ONCE
        case Some("AT_LEAST_ONCE") => Semantic.AT_LEAST_ONCE
        case Some("EXACTLY_ONCE") => Semantic.EXACTLY_ONCE
        case Some("NONE") => Semantic.NONE
        case _ => throw new IllegalArgumentException("[StreamX] kafka.sink semantic error,must be (AT_LEAST_ONCE|EXACTLY_ONCE|NONE) ")
      }
      val schema = new KeyedSerializationSchemaWrapper[T](serializer)

      val customPartitioner = partitioner match {
        case null => Optional.ofNullable(null).asInstanceOf[Optional[FlinkKafkaPartitioner[T]]]
        case part => Optional.of(part)
      }
      new FlinkKafkaProducer[T](topicId, schema, prop, customPartitioner, semantic, DEFAULT_KAFKA_PRODUCERS_POOL_SIZE)
    }

    /**
     * versions 0.10+ allow attaching the records' event timestamp when writing them to Kafka;
     * this method is not available for earlier Kafka versions
     */
    producer.setWriteTimestampToKafka(true)

    val sink = stream.addSink(producer)
    afterSink(sink, parallelism, name, uid)
  }

}

/**
 *
 * <b>KafkaEqualityPartitioner</b>分区器,顾名思义,该分区器可以均匀的将数据写到各个分区中去
 *
 * @param parallelism
 * @tparam T
 */
class KafkaEqualityPartitioner[T](parallelism: Int) extends FlinkKafkaPartitioner[T] with Logger {

  private[this] var parallelInstanceId = 0

  private[this] val partitionIndex: AtomicInteger = new AtomicInteger(0)

  override def open(parallelInstanceId: Int, parallelInstances: Int): Unit = {
    logInfo(s"KafkaEqualityPartitioner: parallelism $parallelism")
    require(parallelInstanceId >= 0 && parallelInstances > 0, "[StreamX] KafkaEqualityPartitioner:Id of this subtask cannot be negative,Number of subtasks must be larger than 0.")
    this.parallelInstanceId = parallelInstanceId
  }

  override def partition(record: T, key: Array[Byte], value: Array[Byte], targetTopic: String, partitions: Array[Int]): Int = {
    require(partitions != null && partitions.length > 0, "[StreamX] KafkaEqualityPartitioner:Partitions of the target topic is empty.")
    (parallelism, partitions.length) match {
      //kafka have 1 partition
      case (_, 1) => 0
      case (x, y) if x % y == 0 => partitions(parallelInstanceId % partitions.length)
      case (_, y) =>
        partitionIndex.get() match {
          case x if x == y - 1 => partitionIndex.getAndSet(0)
          case _ => partitionIndex.incrementAndGet()
        }
    }
  }

  override def equals(o: Any): Boolean = this == o || o.isInstanceOf[KafkaEqualityPartitioner[T]]

  override def hashCode: Int = classOf[KafkaEqualityPartitioner[T]].hashCode

  def checkArgument(condition: Boolean, @Nullable errorMessage: String): Unit = if (!condition) throw new IllegalArgumentException(errorMessage)

}
