package com.streamxhub.flink.test

import java.util.Properties

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.flink.core.source.MySQLSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object MySQLSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop = new Properties()
    prop.put(KEY_INSTANCE, "test")
    prop.put(KEY_JDBC_DRIVER, "com.mysql.cj.jdbc.Driver")
    prop.put(KEY_JDBC_URL, "jdbc:mysql://localhost:3306")
    prop.put("readOnly", "false")
    prop.put("idleTimeout", "20000")

    val mysqlSource = new MySQLSource(context)
    mysqlSource.getDataStream[Orders]({
    ""
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
