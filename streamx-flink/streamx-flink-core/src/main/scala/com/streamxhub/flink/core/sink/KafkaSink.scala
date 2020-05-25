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

import com.streamxhub.common.conf.ConfigConst
import com.streamxhub.common.util.{ConfigUtils, Logger}
import org.apache.flink.api.common.serialization.{SerializationSchema, SimpleStringSchema}
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011
import com.streamxhub.flink.core.StreamingContext
import javax.annotation.Nullable
import org.apache.flink.streaming.connectors.kafka.partitioner.{FlinkFixedPartitioner, FlinkKafkaPartitioner}
import org.apache.flink.util.Preconditions

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
   *
   * @param stream
   * @param topic
   * @param serializationSchema 序列化Scheam,不指定默认使用SimpleStringSchema
   * @param customPartitioner   指定kafka分区器(默认使用FlinkFixedPartitioner分区器,注意sink的并行度的设置和kafka的分区数有关,不然会出现往一个分区写...)
   * @tparam T
   * @return
   */
  def sink[T](stream: DataStream[T],
              topic: String = "",
              serializationSchema: SerializationSchema[T] = new SimpleStringSchema().asInstanceOf[SerializationSchema[T]],
              customPartitioner: FlinkKafkaPartitioner[T] = new BalancePartitioner[T](ctx.getParallelism)): DataStreamSink[T] = {

    val prop = ConfigUtils.getKafkaSinkConf(ctx.parameter.toMap, topic)
    overrideParams.foreach(x => prop.put(x._1, x._2))
    val topicName = prop.remove(ConfigConst.KEY_KAFKA_TOPIC).toString
    val producer = new FlinkKafkaProducer011[T](topicName, serializationSchema, prop, Optional.of(customPartitioner))

    /**
     * versions 0.10+ allow attaching the records' event timestamp when writing them to Kafka;
     * this method is not available for earlier Kafka versions
     */
    producer.setWriteTimestampToKafka(true)
    val sink = stream.addSink(producer)
    afterSink(sink, parallelism, name, uid)
  }

}

class BalancePartitioner[T](parallelism: Int) extends FlinkKafkaPartitioner[T] with Logger {

  private var parallelInstanceId = 0

  var partitionIndex: Int = 0

  override def open(parallelInstanceId: Int, parallelInstances: Int): Unit = {
    logger.info(s"[StreamX-Flink] BalancePartitioner: parallelism $parallelism")
    checkArgument(parallelInstanceId >= 0, "[StreamX-Flink] BalancePartitioner:Id of this subtask cannot be negative.")
    checkArgument(parallelInstances > 0, "[StreamX-Flink] BalancePartitioner:Number of subtasks must be larger than 0.")
    this.parallelInstanceId = parallelInstanceId
  }

  override def partition(record: T, key: Array[Byte], value: Array[Byte], targetTopic: String, partitions: Array[Int]):Int = {
    checkArgument(partitions != null && partitions.length > 0, "[StreamX-Flink] BalancePartitioner:Partitions of the target topic is empty.")
    if (parallelism % partitions.length == 0) partitions(parallelInstanceId % partitions.length) else {
      if (partitionIndex == partitions.length) partitionIndex = 1 else partitionIndex += 1
      partitionIndex
    }
  }

  override def equals(o: Any):Boolean = this == o || o.isInstanceOf[BalancePartitioner[T]]

  override def hashCode:Int = classOf[BalancePartitioner[T]].hashCode

  def checkArgument(condition: Boolean, @Nullable errorMessage: String): Unit =  if (!condition) throw new IllegalArgumentException(errorMessage)

}