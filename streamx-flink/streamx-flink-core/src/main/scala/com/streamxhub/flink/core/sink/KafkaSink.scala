package com.streamxhub.flink.core.sink

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011
import com.streamxhub.flink.core.conf.{ConfigConst, Config}
import com.streamxhub.flink.core.StreamingContext

import scala.collection.JavaConversions._
import scala.collection.Map

object KafkaSink {
  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null): KafkaSink = new KafkaSink(ctx, overwriteParams, parallelism, name, uid)
}

class KafkaSink(@transient val ctx: StreamingContext,
                overwriteParams: Map[String, String] = Map.empty[String, String],
                parallelism: Int = 0,
                name: String = null,
                uid: String = null) extends Sink {

  def sink[T](stream: DataStream[String])(implicit topic: String = ""): DataStreamSink[String] = {
    val prop = Config.getKafkaSink(ctx.parameter, topic)
    prop.putAll(overwriteParams)
    val topicName = prop.getProperty(ConfigConst.TOPIC)
    val producer = new FlinkKafkaProducer011[String](topicName, new SimpleStringSchema, prop)
    val sink = stream.addSink(producer)
    afterSink(sink, parallelism, name, uid)
  }

}
