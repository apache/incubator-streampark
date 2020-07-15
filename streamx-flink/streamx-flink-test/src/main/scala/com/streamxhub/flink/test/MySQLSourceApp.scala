package com.streamxhub.flink.test

import java.util.Properties
import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.flink.core.request.MySQLRequest
import com.streamxhub.flink.core.source.MySQLSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object MySQLSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop = new Properties()
    prop.put(KEY_INSTANCE, "test")
    prop.put(KEY_JDBC_DRIVER, "com.mysql.cj.jdbc.Driver")
    prop.put(KEY_JDBC_URL, "jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true")
    prop.put(KEY_JDBC_USER, "root")
    prop.put(KEY_JDBC_PASSWORD, "123322242")
    prop.put("readOnly", "false")
    prop.put("idleTimeout","20000")

    val mysqlSource = new MySQLSource(context)
    val ds = mysqlSource.getDataStream[Orders](
      "select * from orders limit 10",
      r => {
        r.map(x => {
          val values =  x("values").toString
          val timestamp = x("timestamp").toString.toLong
          new Orders(values,timestamp)
        })
      }
    )

    val ds1 = MySQLRequest(ds).requestUnordered[Orders](
      x => s"select * from orders where timestamp='${x.timestamp}'",
      x => {
        val values =  x("values").toString
        val timestamp = x("timestamp").toString.toLong
        new Orders(values,timestamp)
      }
    )

    ds1.print()

  }

}

class Orders(val values: String,val timestamp: Long) extends Serializable {

  override def toString = s"Orders($values, $timestamp)"
}
