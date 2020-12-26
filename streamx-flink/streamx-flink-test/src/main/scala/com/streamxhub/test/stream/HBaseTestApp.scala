package com.streamxhub.test.stream

import com.streamxhub.flink.core.java.wrapper.HBaseQuery
import org.apache.hadoop.hbase.client.{Get, Scan}

import scala.collection.JavaConversions._
import java.util.Properties

object HBaseTestApp extends App {

  val prop:Properties = new Properties()
  prop.put("hbase.zookeeper.quorum","test1,test2,test6")
  prop.put("hbase.zookeeper.property.clientPort","2181")

  val query = new HBaseQuery("person", new Get("123322242".getBytes()))

  val table =  query.getTable(prop)

  val scanner = table.getScanner(query)
  if(scanner.isEmpty) {
    println("null....")
  }

}
