package com.streamxhub.streamx.test.stream

import com.streamxhub.streamx.common.util.ConfigUtils
import com.streamxhub.streamx.flink.core.scala.source.JdbcSource
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import org.apache.flink.api.scala._

import java.util.Properties

object MySQLSourceApp extends FlinkStreaming {

  override def handle(): Unit = {
    implicit val prop: Properties = ConfigUtils.getMySQLConf(context.parameter.toMap)
    val JdbcSource = new JdbcSource(context)
    JdbcSource.getDataStream[Orders](x => {
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
