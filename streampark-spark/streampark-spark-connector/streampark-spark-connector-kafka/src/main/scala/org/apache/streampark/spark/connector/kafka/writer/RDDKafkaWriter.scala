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
package org.apache.streampark.spark.connector.kafka.writer

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.rdd.RDD

import java.util.Properties

import scala.annotation.meta.param
import scala.reflect.ClassTag

class RDDKafkaWriter[T: ClassTag](@(transient @param) rdd: RDD[T]) extends KafkaWriter[T] {

  /**
   * @param producerConfig
   *   The configuration that can be used to connect to Kafka
   * @param serializerFunc
   *   The function to convert the data from the stream into Kafka [[ProducerRecord]]s.
   * @tparam K
   *   The type of the key
   * @tparam V
   *   The type of the value
   */
  override def writeToKafka[K, V](
      producerConfig: Properties,
      serializerFunc: (T) => ProducerRecord[K, V]): Unit = {
    rdd.foreachPartition(events => {
      val producer: KafkaProducer[K, V] =
        KafkaWriter.getProducer(producerConfig)
      events.map(serializerFunc).foreach(producer.send)
    })
  }
}
