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
import org.apache.spark.streaming.dstream.DStream

import java.util.Properties

import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * Import this object in this form:
 * {{{
 *   import org.cloudera.spark.streaming.kafka.KafkaWriter._
 * }}}
 *
 * Once this is done, the `writeToKafka` can be called on the [[DStream]] object in this form:
 * {{{
 *   dstream.writeToKafka(producerConfig, f)
 * }}}
 */
private[kafka] object KafkaWriter {

  private val producers = new mutable.HashMap[Properties, Any]()

  def getProducer[K, V](config: Properties): KafkaProducer[K, V] = {
    producers
      .getOrElse(
        config, {
          val producer = new KafkaProducer[K, V](config)
          producers(config) = producer
          producer
        })
      .asInstanceOf[KafkaProducer[K, V]]
  }

  /**
   * This implicit method allows the user to call dstream.writeToKafka(..)
   *
   * @param dstream
   *   \- DStream to write to Kafka
   * @tparam T
   *   \- The type of the DStream
   * @tparam K
   *   \- The type of the key to serialize to
   * @tparam V
   *   \- The type of the value to serialize to
   * @return
   */
  implicit def createKafkaOutputWriter[T: ClassTag, K, V](dstream: DStream[T]): KafkaWriter[T] = {
    new DStreamKafkaWriter[T](dstream)
  }

  implicit def createKafkaOutputWriter[T: ClassTag, K, V](rdd: RDD[T]): KafkaWriter[T] = {
    new RDDKafkaWriter[T](rdd)
  }

  implicit def createKafkaOutputWriter[T: ClassTag, K, V](msg: Iterator[T]): KafkaWriter[T] = {
    new IterKafkaWriter[T](msg)
  }

  implicit def createKafkaOutputWriter[T: ClassTag, K, V](msg: T): KafkaWriter[T] = {
    new SimpleKafkaWriter[T](msg)
  }
}

/**
 * This class can be used to write data to Kafka from Spark Streaming. To write data to Kafka simply
 * `import org.cloudera.spark.streaming.kafka.KafkaWriter._` in your application and call
 * `dstream.writeToKafka(producerConf, func)`
 *
 * Here is an example:
 * {{{
 * // Adding this line allows the user to call dstream.writeDStreamToKafka(..)
 * import org.apache.spark.streaming.kafka.KafkaWriter._
 *
 * class ExampleWriter {
 *   val instream = ssc.queueStream(toBe)
 *
 *   val props = new Properties()
 *   props.put("bootstrap.servers", "localhost:9092")
 *   props.put("acks", "all")
 *   props.put("retries", 0)
 *   props.put("batch.size", 16384)
 *   props.put("linger.ms", 1)
 *   props.put("buffer.memory", 33554432)
 *   props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
 *   props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
 *
 *   instream.writeToKafka(props,
 *   x => new ProducerRecord[String, String](sinkTopic, UUID.randomUUID().toString, x.toString))
 *
 *   ssc.start()
 * }
 *
 * }}}
 */
abstract class KafkaWriter[T: ClassTag]() extends Serializable {

  /**
   * To write data from a DStream to Kafka, call this function after creating the DStream. Once the
   * DStream is passed into this function, all data coming from the DStream is written out to Kafka.
   * The properties instance takes the configuration required to connect to the Kafka brokers in the
   * standard Kafka format. The serializerFunc is a function that converts each element of the RDD
   * to a Kafka [[ProducerRecord]]. This closure should be serializable - so it should use only
   * instances of Serializables.
   *
   * @param producerConfig
   *   The configuration that can be used to connect to Kafka
   * @param serializerFunc
   *   The function to convert the data from the stream into Kafka [[ProducerRecord]]s.
   * @tparam K
   *   The type of the key
   * @tparam V
   *   The type of the value
   */
  def writeToKafka[K, V](
      producerConfig: Properties,
      serializerFunc: T => ProducerRecord[K, V]): Unit
}
