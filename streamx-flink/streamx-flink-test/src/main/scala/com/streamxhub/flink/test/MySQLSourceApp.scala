package com.streamxhub.flink.test

import java.util.Properties

import com.streamxhub.flink.core.conf.ConfigConst.{KEY_MYSQL_DRIVER, KEY_MYSQL_INSTANCE, KEY_MYSQL_PASSWORD, KEY_MYSQL_URL, KEY_MYSQL_USER}
import com.streamxhub.flink.core.source.MySQLSource
import com.streamxhub.flink.core.util.JsonUtils
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.common.typeinfo.TypeInformation

object MySQLSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val prop = new Properties()
    prop.put(KEY_MYSQL_INSTANCE,"test")
    prop.put(KEY_MYSQL_DRIVER,"com.mysql.jdbc.Driver")
    prop.put(KEY_MYSQL_URL,"jdbc:mysql://localhost:3306/test")
    prop.put(KEY_MYSQL_USER,"root")
    prop.put(KEY_MYSQL_PASSWORD,"123322242")
    prop.put("readOnly","false")
    prop.put("idleTimeout","20000")

    implicit val typeInfo = TypeInformation.of[MyPerson](classOf[MyPerson])
    val mysqlSource = new MySQLSource(context)
    val ds = mysqlSource.getDataStream[MyPerson]("select * from student",x=>  JsonUtils.read[MyPerson](x))
    ds.print()
  }

}

case class MyPerson(name:String,age:Int,password:String)
