/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.spark.connector.kafka.writer

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.util.Properties
import scala.annotation.meta.param
import scala.reflect.ClassTag

/**
  *
  *
  * A simple Kafka producers
  */
class IterKafkaWriter[T: ClassTag](@(transient@param) msg: Iterator[T]) extends KafkaWriter[T] {
  /**
    *
    * @param producerConfig The configuration that can be used to connect to Kafka
    * @param serializerFunc The function to convert the data from the stream into Kafka
    *                       [[ProducerRecord]]s.
    * @tparam K The type of the key
    * @tparam V The type of the value
    *
    */
  override def writeToKafka[K, V](producerConfig: Properties, serializerFunc: (T) => ProducerRecord[K, V]): Unit = {
    val producer: KafkaProducer[K, V] = KafkaWriter.getProducer(producerConfig)
    msg.map(serializerFunc).foreach(producer.send)
  }
}
