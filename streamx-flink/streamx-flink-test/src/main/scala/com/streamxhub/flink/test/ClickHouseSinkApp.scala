package com.streamxhub.flink.test

import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import com.streamxhub.flink.core.sink.{AsyncClickHouseOutputFormat, ClickHouseSink, HttpSink}
import org.apache.flink.streaming.api.scala._

object ClickHouseSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    val createTable =
      """
        |create TABLE test.orders(
        |userId UInt16,
        |orderId UInt16,
        |siteId UInt8,
        |cityId UInt8,
        |orderStatus UInt8,
        |price Float64,
        |quantity UInt8,
        |timestamp UInt16
        |)ENGINE = TinyLog;
        |""".stripMargin.toString

    println(createTable)

    val source = context.addSource(new TestSource)

    var index = 0
    val httpDs = source.map(x => {
      index += 1
      s"""http://www.qq.com?id=$index"""
    })

    HttpSink(context).getSink(httpDs).setParallelism(1)

    //ClickHouseSink(context).sink[TestEntity](source, "test.orders").setParallelism(1)
  }

}

