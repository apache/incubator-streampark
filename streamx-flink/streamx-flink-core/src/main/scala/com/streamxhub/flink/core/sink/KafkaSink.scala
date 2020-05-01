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
import com.streamxhub.common.util.ConfigUtils
import org.apache.flink.api.common.serialization.{SerializationSchema, SimpleStringSchema}
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011
import com.streamxhub.flink.core.StreamingContext
import org.apache.flink.streaming.connectors.kafka.partitioner.{FlinkFixedPartitioner, FlinkKafkaPartitioner}

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
  def sink[T](stream: DataStream[T], topic: String = "", serializationSchema: SerializationSchema[T] = new SimpleStringSchema().asInstanceOf[SerializationSchema[T]])(
    implicit customPartitioner: FlinkKafkaPartitioner[T] = new FlinkFixedPartitioner[T]): DataStreamSink[T] = {
    val prop = ConfigUtils.getKafkaSinkConf(ctx.paramMap, topic)
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