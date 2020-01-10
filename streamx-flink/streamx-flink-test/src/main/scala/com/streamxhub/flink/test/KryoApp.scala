package com.streamxhub.flink.test


import com.esotericsoftware.kryo.Kryo
import java.io.FileInputStream
import java.io.FileOutputStream
import java.sql.Connection
import java.util.Properties

import com.esotericsoftware.kryo.io.{Input, Output}
import com.mysql.jdbc.JDBC4Connection
import com.streamxhub.flink.core.conf.ConfigConst.{KEY_MYSQL_DRIVER, KEY_MYSQL_INSTANCE, KEY_MYSQL_PASSWORD, KEY_MYSQL_URL, KEY_MYSQL_USER}
import com.streamxhub.flink.core.util.MySQLUtils
import com.zaxxer.hikari.pool.HikariProxyConnection

object KryoApp {

  def main(args: Array[String]): Unit = {

    implicit val prop = new Properties()
    prop.put(KEY_MYSQL_INSTANCE,"test")
    prop.put(KEY_MYSQL_DRIVER,"com.mysql.jdbc.Driver")
    prop.put(KEY_MYSQL_URL,"jdbc:mysql://localhost:3306/test")
    prop.put(KEY_MYSQL_USER,"root")
    prop.put(KEY_MYSQL_PASSWORD,"123322242")
    prop.put("readOnly","false")
    prop.put("idleTimeout","20000")

    val conn = MySQLUtils.getConnection(prop)

    val kryo = new Kryo
    kryo.register(classOf[Connection])

    val output = new Output(new FileOutputStream("file.bin"))
    kryo.writeObject(output,conn)
    output.close()

    val input = new Input(new FileInputStream("file.bin"))
    val conn2 = kryo.readObject(input, classOf[Connection])
    println(conn2.getAutoCommit)

  }

}
