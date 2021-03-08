package com.streamxhub.streamx.test.tablesql

import com.streamxhub.streamx.flink.core.scala.{FlinkStreamTable, FlinkTable}

object HelloFlinkSQL extends FlinkStreamTable {

  override def handle(): Unit = {
    val sourceDDL =
      """
        |create table kafka_source (
        |msg STRING
        |) with (
        |'connector' = 'kafka-0.11',
        |'topic' = 'cdc_log',
        |'properties.bootstrap.servers' = 'localhost:9092',
        |'format' = 'json',
        |'scan.startup.mode' = 'latest-offset'
        |)
        |""".stripMargin

    val sinkDDL =
      """
        |create table print_sink(
        |msg STRING
        |) with (
        |'connector' = 'print'
        |)
        |""".stripMargin

    //注册source和sink
    context.executeSql(sourceDDL)
    context.executeSql(sinkDDL)

    //数据提取
    val sourceTab = context.from("kafka_source")
    //这里我们暂时先使用 标注了 deprecated 的API, 因为新的异步提交测试有待改进...
    sourceTab.executeInsert("print_sink")

  }
}
