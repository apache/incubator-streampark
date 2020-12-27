package com.streamxhub.test.stream

import com.mongodb.BasicDBObject
import com.streamxhub.common.util.DateUtils
import com.streamxhub.flink.core.scala.source.MongoSource
import com.streamxhub.flink.core.scala.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.scala._

import scala.collection.mutable.ListBuffer

object MongoSourceApp extends FlinkStreaming {

  override def handle(context: StreamingContext): Unit = {
    implicit val prop = context.parameter.getProperties
    val source = new MongoSource(context)
    source.getDataStream[String]("shop", (a, d) => {
      val cond = new BasicDBObject().append("updateTime", new BasicDBObject("$gte", DateUtils.parse("2019-09-27 00:00:00")))
      d.find(cond)
    }, x => {
      val list = new ListBuffer[String]
      while (x.hasNext) {
        list += x.next().toJson()
      }
      list.toList
    }).print()
  }

}
