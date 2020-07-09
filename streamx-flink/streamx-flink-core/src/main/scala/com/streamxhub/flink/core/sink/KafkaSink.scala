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
package com.streamxhub.flink.core.sink

import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger

import com.streamxhub.common.conf.ConfigConst
import com.streamxhub.common.util.{ConfigUtils, Logger}
import org.apache.flink.api.common.serialization.{SerializationSchema, SimpleStringSchema}
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011
import com.streamxhub.flink.core.StreamingContext
import javax.annotation.Nullable
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.api.datastream.{DataStream => JavaStream}
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner

import scala.annotation.meta.param
import scala.collection.Map

object KafkaSink {
  def apply(@(transient@param) ctx: StreamingContext,
            overrideParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null): KafkaSink = new KafkaSink(ctx, overrideParams, parallelism, name, uid)
}

class KafkaSink(@(transient@param) val ctx: StreamingContext,
                overrideParams: Map[String, String] = Map.empty[String, String],
                parallelism: Int = 0,
                name: String = null,
                uid: String = null) extends Sink {



  /**
   * for java
   *
   * @param stream
   * @param topic
   * @param serializationSchema
   * @param partitioner
   * @tparam T
   * @return
   */
  private[core] def javaSink[T](stream: JavaStream[T],
                                topic: String = "",
                                serializationSchema: SerializationSchema[T] = new SimpleStringSchema().asInstanceOf[SerializationSchema[T]],
                                partitioner: FlinkKafkaPartitioner[T] = new KafkaEqualityPartitioner[T](ctx.getParallelism)
                               ): DataStreamSink[T] = {
    sink(new DataStream[T](stream), topic, serializationSchema, partitioner)
  }


  /**
   * for scala
   *
   * @param stream
   * @param topic
   * @param serializationSchema 序列化Scheam,不指定默认使用SimpleStringSchema
   * @param partitioner         指定kafka分区器(默认使用<b>KafkaEqualityPartitioner</b>分区器,顾名思义,该分区器可以均匀的将数据写到各个分区中去,
   *                            注意:Flink中默认使用的是<span style="color:RED">FlinkFixedPartitioner</span>分区器,该分区器需要特别注意sink的并行度和kafka的分区数,不然会出现往一个分区写...
   *                            )
   * @tparam T
   * @return
   */
  def sink[T](stream: DataStream[T],
              topic: String = "",
              serializationSchema: SerializationSchema[T] = new SimpleStringSchema().asInstanceOf[SerializationSchema[T]],
              partitioner: FlinkKafkaPartitioner[T] = new KafkaEqualityPartitioner[T](ctx.getParallelism)): DataStreamSink[T] = {

    val prop = ConfigUtils.getKafkaSinkConf(ctx.parameter.toMap, topic)
    overrideParams.foreach(x => prop.put(x._1, x._2))
    val topicName = prop.remove(ConfigConst.KEY_KAFKA_TOPIC).toString
    val producer = partitioner match {
      case null => new FlinkKafkaProducer011[T](topicName, serializationSchema, prop, Optional.ofNullable(null).asInstanceOf[Optional[FlinkKafkaPartitioner[T]]])
      case other => new FlinkKafkaProducer011[T](topicName, serializationSchema, prop, Optional.of(other))
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
    logger.info(s"[StreamX-Flink] KafkaEqualityPartitioner: parallelism $parallelism")
    checkArgument(parallelInstanceId >= 0, "[StreamX-Flink] KafkaEqualityPartitioner:Id of this subtask cannot be negative.")
    checkArgument(parallelInstances > 0, "[StreamX-Flink] KafkaEqualityPartitioner:Number of subtasks must be larger than 0.")
    this.parallelInstanceId = parallelInstanceId
  }

  override def partition(record: T, key: Array[Byte], value: Array[Byte], targetTopic: String, partitions: Array[Int]): Int = {
    checkArgument(partitions != null && partitions.length > 0, "[StreamX-Flink] KafkaEqualityPartitioner:Partitions of the target topic is empty.")
    (parallelism, partitions.length) match {
      case (_, 1) => 0 //kafka only have 1 partition
      case (x, y) if x % y == 0 => partitions(parallelInstanceId % partitions.length)
      case (_, y) => if (partitionIndex.get() == y - 1) partitionIndex.getAndSet(0) else partitionIndex.getAndIncrement()
    }
  }

  override def equals(o: Any): Boolean = this == o || o.isInstanceOf[KafkaEqualityPartitioner[T]]

  override def hashCode: Int = classOf[KafkaEqualityPartitioner[T]].hashCode

  def checkArgument(condition: Boolean, @Nullable errorMessage: String): Unit = if (!condition) throw new IllegalArgumentException(errorMessage)

}