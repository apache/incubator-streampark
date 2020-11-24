package com.streamxhub.flink.test

import com.streamxhub.flink.core.scala.source.KafkaSource
import com.streamxhub.flink.core.scala.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object KafkaSourceApp extends FlinkStreaming {

  /**
   * 用户可覆盖次方法...
   *
   */
  override def beforeStart(context: StreamingContext): Unit = super.beforeStart(context)

  override def handler(context: StreamingContext): Unit = {

    //one topic
    new KafkaSource(context).getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        x.topic
      })
      .print()

  }

}

