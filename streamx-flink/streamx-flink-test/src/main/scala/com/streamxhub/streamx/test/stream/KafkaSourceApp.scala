package com.streamxhub.streamx.test.stream

import com.streamxhub.streamx.flink.core.scala.source.KafkaSource
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object KafkaSourceApp extends FlinkStreaming {

  /**
   * 用户可覆盖次方法...
   *
   */
  override def ready(): Unit = super.ready()


  override def config(env: StreamExecutionEnvironment,
                      parameter: ParameterTool): Unit = {

  }

  override def handle(): Unit = {

    //one topic
    KafkaSource().getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        x.value
      })
      .print()

  }

}
