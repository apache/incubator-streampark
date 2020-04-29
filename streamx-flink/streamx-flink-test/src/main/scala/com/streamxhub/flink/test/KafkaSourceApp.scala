package com.streamxhub.flink.test

import com.streamxhub.flink.core.source.KafkaSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object KafkaSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {

    //one topic
    new KafkaSource(context).getDataStream[String]("kfk1")
      .uid("kfkSource1")
      .name("kfkSource1")
      .print()

    //more topic
    new KafkaSource(context).getDataStream[String](List("kfk1", "kfk2"))
      .uid("kfkSource2")
      .name("kfkSource2")
      .print()


  }

}
