package com.streamxhub.test.stream

import com.streamxhub.common.util.ConfigUtils
import com.streamxhub.flink.core.scala.source.MySQLSource
import com.streamxhub.flink.core.scala.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.scala._

import java.util.Properties

object MySQLSourceApp extends FlinkStreaming {

  override def handle(context: StreamingContext): Unit = {
    implicit val prop: Properties = ConfigUtils.getMySQLConf(context.parameter.toMap)
    val mysqlSource = new MySQLSource(context)
    mysqlSource.getDataStream[Orders](x => {
      val offset = if (x == null) "2020-10-10 23:00:00" else x.timestamp
      s"select * from orders where create_time>'$offset' order by sync_time asc limit 1000 "
    },
      r => {
        r.map(x => {
          new Orders(x("market_id").toString, x("create_time").toString)
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
