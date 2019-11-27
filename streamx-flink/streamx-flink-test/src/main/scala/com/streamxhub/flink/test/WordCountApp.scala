package com.streamxhub.flink.test

import org.apache.flink.streaming.api.scala._

object WordCountApp extends App {


  val env = StreamExecutionEnvironment.getExecutionEnvironment

  val stream = env.readTextFile("data/in/wordcount.txt")

  val ds = stream.flatMap(x => x.split("\\s+"))

  val wc1 = ds.flatMap(x => Some(x -> 1))
    .keyBy("_1")
    .sum("_2")

  val wc2 = ds.flatMap(x => Some(WC(x)))
    .keyBy(0)
    .sum("count")

  wc2.print()

  env.execute()

}


case class WC(word: String, count: Int = 1)