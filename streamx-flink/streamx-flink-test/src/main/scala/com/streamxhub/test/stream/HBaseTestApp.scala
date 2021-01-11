package com.streamxhub.test.stream

import com.streamxhub.common.util.ConfigUtils
import com.streamxhub.flink.core.java.wrapper.HBaseQuery
import com.streamxhub.flink.core.scala.{FlinkStreaming, StreamingContext}
import com.streamxhub.flink.core.scala.request.HBaseRequest
import org.apache.flink.api.scala._
import org.apache.hadoop.hbase.client.Get

object HBaseTestApp extends FlinkStreaming {

  override def handle(context: StreamingContext): Unit = {
    implicit val conf = ConfigUtils.getHBaseConfig(context.parameter.toMap)
    //one topic
    val source = context.fromCollection(Seq("123322242", "1111", "222"))

    source.print("source:>>>")

    HBaseRequest(source).requestOrdered[(String,Boolean)](x => {
      new HBaseQuery("person", new Get(x.getBytes()))
    }, timeout = 5000, resultFunc = (a, r) => {
      a -> !r.isEmpty
    }).print(" check.... ")


  }

}
