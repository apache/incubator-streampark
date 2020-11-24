package com.streamxhub.flink.test



import com.mongodb.BasicDBObject
import org.apache.flink.streaming.api.scala._
import com.streamxhub.common.util.DateUtils
import com.streamxhub.flink.core.scala.source.MongoSource
import com.streamxhub.flink.core.scala.{FlinkStreaming, StreamingContext}

import scala.collection.mutable.ListBuffer

object MongoSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop = context.parameter.getProperties
    val source = new MongoSource(context)
    source.getDataStream[String](x => {
      val collection = x.getCollection("shop")
      val cond = new BasicDBObject().append("updateTime", new BasicDBObject("$gte", DateUtils.parse("2019-09-27 00:00:00")))
      collection.find(cond)
    }, x => {
      val list = new ListBuffer[String]
      while (x.hasNext) {
        list += x.next().toJson()
      }
      list.toList
    }).print()
  }

}
