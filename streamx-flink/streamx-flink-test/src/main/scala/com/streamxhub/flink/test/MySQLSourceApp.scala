package com.streamxhub.flink.test

import java.util.Properties

import com.streamxhub.common.util.JsonUtils
import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.flink.core.source.MySQLSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object MySQLSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop = new Properties()
    prop.put(KEY_INSTANCE, "test")
    prop.put(KEY_JDBC_DRIVER, "com.mysql.jdbc.Driver")
    prop.put(KEY_JDBC_URL, "jdbc:mysql://localhost:3306/test")
    prop.put(KEY_JDBC_USER, "root")
    prop.put(KEY_JDBC_PASSWORD, "123322242")
    prop.put("readOnly", "false")
    prop.put("idleTimeout", "20000")

    val mysqlSource = new MySQLSource(context)
    val ds = mysqlSource.getDataStream[MyPerson]("select * from student", x => x.map(r => JsonUtils.read[MyPerson](r)))
    ds.print()
  }

}

case class MyPerson(name: String, age: Int, password: String)
