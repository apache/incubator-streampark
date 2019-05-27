package com.streamxhub.spark.core.support.kafka.writer

import java.util.Properties

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.spark.streaming.dstream.DStream

import scala.annotation.meta.param
import scala.reflect.ClassTag

class DStreamKafkaWriter[T: ClassTag](@(transient@param) dstream: DStream[T]) extends KafkaWriter[T] {

  /**
    *
    * @param producerConfig The configuration that can be used to connect to Kafka
    * @param serializerFunc The function to convert the data from the stream into Kafka
    *                       [[ProducerRecord]]s.
    * @tparam K The type of the key
    * @tparam V The type of the value
    *
    */
  override def writeToKafka[K, V](producerConfig: Properties,
                                  serializerFunc: T => ProducerRecord[K, V]): Unit = {
    dstream.foreachRDD { rdd =>
      val rddWriter = new RDDKafkaWriter[T](rdd)
      rddWriter.writeToKafka(producerConfig, serializerFunc)
    }
  }
}
