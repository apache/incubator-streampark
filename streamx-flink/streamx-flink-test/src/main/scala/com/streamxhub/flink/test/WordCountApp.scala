package com.streamxhub.flink.test

import org.apache.flink.streaming.api.scala._

object WordCountApp extends App {


  val senv = StreamExecutionEnvironment.getExecutionEnvironment

  case class WoldCount(wold: String,count: Int)
  senv.fromElements("hello world", "hello flink", "hello hadoop").flatMap(_.split("\\s+")).map(WoldCount(_, 1)).keyBy("wold").sum("count").print()

  senv.execute()

}

case class WoldCount(wold: String,count: Int)
