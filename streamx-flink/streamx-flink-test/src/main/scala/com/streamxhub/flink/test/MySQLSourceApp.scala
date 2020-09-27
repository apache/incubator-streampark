package com.streamxhub.flink.test


import java.util.Properties

import com.streamxhub.common.util.ConfigUtils
import com.streamxhub.flink.core.source.scala.MySQLSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.scala._

object MySQLSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop: Properties = ConfigUtils.getMySQLConf(context.parameter.toMap)
    val mysqlSource = new MySQLSource(context)
    mysqlSource.getDataStream[Orders](() => {
      "select * from tc_record where sync_time>'2020-07-20 23:00:00' limit 1000"
    },
      r => {
        r.map(x => {
          new Orders(x("market_id").toString, x("sync_time").toString)
        })
      }
    ).map(x => {
      x.timestamp
    }).print()

  }

}

class Orders(val values: String, val timestamp: String) extends Serializable {

  override def toString = s"Orders($values, $timestamp)"
}
