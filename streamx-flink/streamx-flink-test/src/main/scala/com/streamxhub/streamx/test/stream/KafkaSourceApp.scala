package com.streamxhub.streamx.test.stream

import com.streamxhub.streamx.flink.core.scala.source.KafkaSource
import com.streamxhub.streamx.flink.core.scala.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.scala._

object KafkaSourceApp extends FlinkStreaming {

  /**
   * 用户可覆盖次方法...
   *
   */
  override def beforeStart(context: StreamingContext): Unit = super.beforeStart(context)

  override def handle(context: StreamingContext): Unit = {

    //one topic
    KafkaSource(context).getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        x.topic
      })
      .print()

  }

}
