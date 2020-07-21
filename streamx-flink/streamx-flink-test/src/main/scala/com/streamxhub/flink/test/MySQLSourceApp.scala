package com.streamxhub.flink.test

import java.util.Properties

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.flink.core.source.MySQLSource
import com.streamxhub.flink.core.wrapper.MySQLQuery
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object MySQLSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop = new Properties()
    prop.put(KEY_INSTANCE, "test")
    prop.put(KEY_JDBC_DRIVER, "com.mysql.cj.jdbc.Driver")
    prop.put(KEY_JDBC_URL, "jdbc:mysql://rm-2zer0v9g25bgu4rx43o.mysql.rds.aliyuncs.com:3306/hopsonone_park_sh_real_time?user=hopsononebi_ro&password=7RH1UX0bisVDot0v&useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&autoReconnect=true&failOverReadOnly=false")
    prop.put("readOnly", "false")
    prop.put("idleTimeout", "20000")

    val mysqlSource = new MySQLSource(context)
    mysqlSource.getDataStream[Orders](q => {
      Thread.sleep(1000)
      //没有上次记录的消费信息
      if (q != null) q else {
        println("init...................")
        val query = new MySQLQuery()
        val time = "2020-07-19 00:00:00"
        query.setTable("tc_record")
        query.setField("sync_time")
        query.setOption(">=")
        query.setTimestamp(time)
        query.setFetchSize(100)
        query
      }
    },
      r => {
        r.map(x => {
          new Orders(x("market_id").toString, x("sync_time").toString)
        })
      }
    ).map(x=>{
      x.timestamp
    }).print()

  }

}

class Orders(val values: String, val timestamp: String) extends Serializable {

  override def toString = s"Orders($values, $timestamp)"
}
