package com.streamxhub.flink.test

import com.streamxhub.flink.core.source.{KafkaRecord, KafkaSource}
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object KafkaSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {

    //one topic
    new KafkaSource(context).getDataStream[String]("aa")
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        println(x.topic)
        x.value
      })
      .print()

  }

}
