package com.streamxhub.flink.test

import org.apache.flink.streaming.api.scala._

object WordCountApp extends App {

  val env = StreamExecutionEnvironment.getExecutionEnvironment

  env.fromElements("hello world", "hello flink", "hello hadoop")
    .flatMap(_.split("\\s+"))
    .map(x => WoldCount(x, 1))
    .keyBy(_.wold)
    .sum("count")
    .print()

  env.execute()

}

case class WoldCount(wold: String, count: Int)
