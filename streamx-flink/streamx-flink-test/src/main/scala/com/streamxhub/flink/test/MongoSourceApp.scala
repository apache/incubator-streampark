package com.streamxhub.flink.test


import com.mongodb.client.model.Filters
import com.streamxhub.flink.core.source.MongoSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object MongoSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop = context.parameter.getProperties
    val source = new MongoSource(context)
    source.getDataStream[String](
      _.getCollection("shop").find(Filters.gt("updateTime", "2020-03-27 00:00:00")),
      _.toJson(),
      10000L)
      .print()
  }

}
