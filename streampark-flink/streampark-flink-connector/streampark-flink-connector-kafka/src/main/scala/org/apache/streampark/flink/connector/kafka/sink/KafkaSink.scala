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

package org.apache.streampark.flink.connector.kafka.sink

import org.apache.streampark.common.conf.ConfigConst
import org.apache.streampark.common.util.{ConfigUtils, Utils}
import org.apache.streampark.flink.connector.kafka.bean.KafkaEqualityPartitioner
import org.apache.streampark.flink.connector.sink.Sink
import org.apache.streampark.flink.core.scala.StreamingContext
import org.apache.flink.api.common.serialization.{SerializationSchema, SimpleStringSchema}
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.{DEFAULT_KAFKA_PRODUCERS_POOL_SIZE, Semantic}
import org.apache.flink.streaming.connectors.kafka.internals.KeyedSerializationSchemaWrapper
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner

import java.util.{Optional, Properties}
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
   * @param serializer  serializer, if not specified, used <b>SimpleStringSchema<b>
   * @param partitioner kafka partitioner, used <b>KafkaEqualityPartitioner</b> as default partitioner,
   *                    the partitioner can evenly write data to each partition.
   *                    Note: The default used in Flink is <span style="color:RED">FlinkFixedPartitioner</span>,
   *                    which is need to pay attention to the parallelism of sink and the number of kafka partitions,
   *                    otherwise it will appear to write to a partition.)
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
       * kafkaProducersPoolSize will be used under EXACTLY_ONCE semantics
       */
      val semantic = Try(Some(prop.remove(ConfigConst.KEY_KAFKA_SEMANTIC).toString.toUpperCase)).getOrElse(None) match {
        case None => Semantic.AT_LEAST_ONCE
        case Some("AT_LEAST_ONCE") => Semantic.AT_LEAST_ONCE
        case Some("EXACTLY_ONCE") => Semantic.EXACTLY_ONCE
        case Some("NONE") => Semantic.NONE
        case _ => throw new IllegalArgumentException("[StreamPark] kafka.sink semantic error,must be (AT_LEAST_ONCE|EXACTLY_ONCE|NONE) ")
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


