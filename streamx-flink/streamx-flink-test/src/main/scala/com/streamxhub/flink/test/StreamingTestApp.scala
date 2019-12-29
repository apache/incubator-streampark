package com.streamxhub.flink.test

import com.streamxhub.flink.core.{StreamingContext, XStreaming}
import org.apache.flink.api.scala._

import scala.util.Try

object StreamingTestApp extends XStreaming {

  override def handler(context: StreamingContext): Unit = {
    val host = context.parameter.get("host")
    val port = Try(context.parameter.get("port").toInt).getOrElse(7070)

    val source = context.socketTextStream(host, port)

    source.flatMap(_.split("\\s+"))
      .map(_ -> 1)
      .keyBy(0)
      .sum(1)
      .print()

  }
}
