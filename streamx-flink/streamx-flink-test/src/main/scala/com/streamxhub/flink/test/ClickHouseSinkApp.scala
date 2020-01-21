package com.streamxhub.flink.test

import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import com.streamxhub.flink.core.sink.ClickHouseSink
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
    ClickHouseSink(context).sink[TestEntity](source,"test.orders")(x => {
      s"(${x.userId},${x.orderId},${x.siteId},${x.cityId},${x.orderStatus},${x.price},${x.quantity},${x.timestamp})"
    })
  }

}

