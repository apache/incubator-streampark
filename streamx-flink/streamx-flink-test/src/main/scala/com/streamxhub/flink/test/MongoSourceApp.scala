package com.streamxhub.flink.test


import com.mongodb.client.model.Filters
import com.streamxhub.flink.core.source.MongoSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._
import com.streamxhub.common.util.DateUtils

object MongoSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop = context.parameter.getProperties
    val source = new MongoSource(context)
    source.getDataStream[String](x => {
      val collection = x.getCollection("shop")
      collection.find(Filters.gte("updateTime", DateUtils.parse("2019-09-27 00:00:00")))
    }, x => {
      x.toJson()
    },
      1000000L)
      .print()
  }

}
