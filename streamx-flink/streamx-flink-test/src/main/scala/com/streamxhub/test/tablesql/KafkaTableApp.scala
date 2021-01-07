package com.streamxhub.test.tablesql

import com.streamxhub.flink.core.scala.table.descriptors.{Kafka, KafkaVer}
import com.streamxhub.flink.core.scala.{FlinkStreamTable, StreamTableContext}
import org.apache.flink.table.descriptors.Csv
import org.apache.flink.api.scala._
import org.apache.flink.table.api._

object KafkaTableApp extends FlinkStreamTable {

  override def handle(context: StreamTableContext): Unit = {


    //connect kafka data
    context
      .connect(Kafka("hello", KafkaVer.`010`))
      .withFormat(new Csv)
      .withSchema(
        "id" -> DataTypes.STRING(),
        "name" -> DataTypes.STRING()
      )
      .createTemporaryTable("kafka2Table")

    //kafka to table
    val table: Table = context.from("kafka2Table")

    //print sink
    table.>>[(String, String)].print("print==>")

    /**
     * 'key 等同于 $"key"
     */
    // select  where
    table.
      select($"id", $"name")
      .where($"id" === "flink")
      .>>[(String, String)].print("simple where==>")

    table.
      select('id, 'name)
      .where('id === "flink")
      .>>[(String, String)].print("simple where2==>")

    /**
     * 查询id=flink,
     * name like apache%
     */
    table.select("id", "name")
      .where("id" === "flink")
      .where("name" like "apache%")
      .>>[(String, String)].print("like where==>")

    /**
     * filter等同于where的操作
     */
    table.select("id", "name")
      .filter("id" === "flink")
      .>>[(String, String)].print("Select -> filter ==>")

    /**
     * groupBy
     */
    table
      .groupBy($"id")
      .select($"id", $"id".count as "count")
      .<<[(String, Long)].print("GroupBy ==>")

  }

}
