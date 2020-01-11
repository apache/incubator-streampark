package com.streamxhub.flink.test

import java.util.Date

import com.streamxhub.common.util.Logger
import com.streamxhub.flink.core.sink.ESSink
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import com.streamxhub.flink.core.util.EsIndexUtils
import org.elasticsearch.action.index.IndexRequest
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import org.apache.flink.streaming.api.scala._

object ConnectorApp extends FlinkStreaming with Logger {


  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def handler(context: StreamingContext): Unit = {
    val ds = context.fromCollection(List(
      OrderEntity(1, 1, 11.3d, 3.1d, new Date()),
      OrderEntity(2, 1, 12.3d, 3.2d, new Date()),
      OrderEntity(3, 1, 13.3d, 3.3d, new Date()),
      OrderEntity(4, 1, 14.3d, 3.4d, new Date()),
      OrderEntity(5, 1, 15.3d, 7.5d, new Date()),
      OrderEntity(6, 1, 16.3d, 3.6d, new Date()),
      OrderEntity(7, 1, 17.3d, 3.7d, new Date())
    ))

    // es sink.........

    //1)定义 Index的写入规则
    implicit def indexReq(x: OrderEntity): IndexRequest = EsIndexUtils.indexRequest(
      "flink_order",
      "_doc",
      s"${x.id}_${x.time.getTime}",
      Serialization.write(x)
    )
    //3)定义esSink并下沉=数据. done
    ESSink(context).sink6[OrderEntity](ds)
  }


  case class OrderEntity(id: Int, num: Int, price: Double, gmv: Double, time: Date) extends Serializable

}
