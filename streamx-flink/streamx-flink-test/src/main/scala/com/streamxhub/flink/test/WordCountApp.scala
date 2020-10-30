package com.streamxhub.flink.test

import org.apache.flink.streaming.api.scala._

object WordCountApp extends App {

  val senv = StreamExecutionEnvironment.getExecutionEnvironment

  senv.socketTextStream("localhost", 9999, '\n')
    .flatMap(_.split("\\s+"))
    .map((_, 1))
    .keyBy(0)
    .sum(1)
    .print()

  senv.execute()


}

