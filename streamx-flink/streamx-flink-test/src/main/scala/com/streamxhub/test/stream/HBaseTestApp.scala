package com.streamxhub.test.stream

import com.streamxhub.flink.core.scala.source.KafkaSource
import com.streamxhub.flink.core.scala.{FlinkStreaming}

import com.streamxhub.common.util.ConfigUtils
import com.streamxhub.flink.core.java.wrapper.HBaseQuery
import com.streamxhub.flink.core.scala.StreamingContext
import com.streamxhub.flink.core.scala.request.HBaseRequest
import org.apache.hadoop.hbase.client.Get
import org.apache.flink.api.scala._

object HBaseTestApp extends FlinkStreaming {

  override def handle(context: StreamingContext): Unit = {
    implicit val conf = ConfigUtils.getHBaseConfig(context.parameter.toMap)
    //one topic
    val kafkaGoodsOrder = new KafkaSource(context).getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(_.value)

    kafkaGoodsOrder.print("source:>>>")

    HBaseRequest(kafkaGoodsOrder).requestOrdered(x => {
      new HBaseQuery("person", new Get(x.getBytes()))
    },timeout = 5000, resultFunc = (a, r) => {
      a -> !r.advance()
    }).print(" check.... ")


  }

}
