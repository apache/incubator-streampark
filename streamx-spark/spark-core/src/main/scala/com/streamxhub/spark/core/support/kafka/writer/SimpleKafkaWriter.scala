package com.streamxhub.spark.core.support.kafka.writer

import java.util.Properties

import com.streamxhub.spark.core.support.kafka.ProducerCache
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.annotation.meta.param
import scala.reflect.ClassTag

/**
  *
  * A simple Kafka producers
  */
class SimpleKafkaWriter[T: ClassTag](@(transient@param) msg: T) extends KafkaWriter[T] {
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
    val producer: KafkaProducer[K, V] = ProducerCache.getProducer(producerConfig)
    producer.send(serializerFunc(msg))
  }
}
