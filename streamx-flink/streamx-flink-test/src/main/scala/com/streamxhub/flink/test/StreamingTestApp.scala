package com.streamxhub.flink.test

import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.api.scala._

import scala.util.Try

object StreamingTestApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    val host = context.parameter.get("host")
    val port = Try(context.parameter.get("port").toInt).getOrElse(7070)

    val source = context.socketTextStream(host, port)

    source.flatMap(_.split("\\s+"))
      .map(x => {
        (x, 1, 2)
      })
      .keyBy(0)
      .reduce(new ReduceFunction[(String, Int, Int)] {
        override def reduce(v1: (String, Int, Int), v2: (String, Int, Int)): (String, Int, Int) = {
          (v1._1, v1._2 + v2._2, v1._3 + v2._3)
        }
      })


  }
}
