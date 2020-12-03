package com.streamxhub.test.tablesql

import com.streamxhub.flink.core.scala.{FlinkTableSQL, TableContext}
import org.apache.flink.table.api.{DataTypes, Table}
import org.apache.flink.table.descriptors.{Csv, Kafka, Schema}

object KafkaTableApp extends FlinkTableSQL {

  override def handler(context: TableContext): Unit = {
    //connect kafka data
    context.connect(
      new Kafka()
        .version("0.11")
        .topic("hello")
        .property("zookeeper.connect", "localhost:2181")
        .property("bootstrap.servers", "localhost:9092")
    ).withFormat(new Csv)
      .withSchema(
        new Schema()
          .field("id", DataTypes.STRING())
          .field("name", DataTypes.STRING())
      ).createTemporaryTable("kafka2Table")

    //kafka to table
    val table: Table = context.from("kafka2Table")

    //print sink
    table.>>[(String, String)].print("print==>")

    /**
     * 'key 等同于 $"key"
     */
    // select  where
    table.
      select($"id", $"name").
      where($"id" === "flink")
      .>>[(String, String)].print("simple where==>")

    /**
     * 查询id=flink,
     * name like apache%
     */
    table.select('id, 'name)
      .where('id === "flink")
      .where('name like "apache%")
      .>>[(String, String)].print("like where==>")

    /**
     * filter等同于where的操作
     */
    table.select($"id", $"name")
      .filter($"id" === "flink")
      .>>[(String, String)].print("Select -> filter ==>")
  }

}
