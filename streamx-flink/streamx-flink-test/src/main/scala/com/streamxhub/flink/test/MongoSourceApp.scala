package com.streamxhub.flink.test


import com.mongodb.client.model.Filters
import com.streamxhub.flink.core.source.{MongoSource}
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object MongoSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop = context.parameter.getProperties
    val source = new MongoSource(context)
    source.getDataStream[String](
      _.getCollection("shop").find(Filters.eq("updateTime", "1585152000000")),
      _.toJson(),
      1000L)
      .print()
  }

}
