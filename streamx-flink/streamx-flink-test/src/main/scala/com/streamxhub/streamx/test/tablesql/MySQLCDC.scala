package com.streamxhub.streamx.test.tablesql

import com.alibaba.ververica.cdc.connectors.mysql.MySQLSource
import com.alibaba.ververica.cdc.debezium.StringDebeziumDeserializationSchema
import com.streamxhub.streamx.flink.core.scala.{FlinkStreamTable, StreamTableContext}
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment

object MySQLCDC extends FlinkStreamTable {

  override def handle(context: StreamTableContext): Unit = {

    val sourceDDL =
      """
        |create table t_user(
        |USER_ID BIGINT,
        |SEX STRING,
        |USERNAME STRING,
        |PASSWORD STRING,
        |EMAIL STRING,
        |MOBILE STRING,
        |CREATE_TIME TIMESTAMP(0)
        |) with (
        |'connector' = 'mysql-cdc',
        |'hostname' = 'localhost',
        |'port' = '3306',
        |'username' = 'root',
        |'password' = '123322242',
        |'database-name' = 'test',
        |'table-name' = 't_user'
        |)
        |""".stripMargin

    val sinkDDL =
      """
        |create table print_sink(
        |USER_ID BIGINT,
        |SEX STRING,
        |USERNAME STRING,
        |PASSWORD STRING,
        |EMAIL STRING,
        |MOBILE STRING,
        |CREATE_TIME TIMESTAMP(0)
        |) with (
        |'connector' = 'print'
        |)
        |""".stripMargin

    //注册source和sink
    context.executeSql(sourceDDL)

    context.executeSql(sinkDDL)

    //数据提取
    val sourceTab = context.from("t_user")
    //这里我们暂时先使用 标注了 deprecated 的API, 因为新的异步提交测试有待改进...
    sourceTab.executeInsert("print_sink")

  }


  def sqlCDC(): Unit = {


  }

  def apiCDC(): Unit = {
    val sourceFunction = MySQLSource.builder[String]
      .hostname("localhost")
      .port(3306)
      .databaseList("test")
      //.tableList("t_user")
      .username("root")
      .password("123322242")
      .deserializer(new StringDebeziumDeserializationSchema)
      .build

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.addSource(sourceFunction)
      .print("cdc")
      .setParallelism(1)

    env.execute("mysql cdc")
  }

}
