package com.streamxhub.streamx.test.tablesql

import com.streamxhub.streamx.flink.core.scala.FlinkStreamTable
import com.streamxhub.streamx.flink.core.scala.table.descriptors.{Kafka, KafkaVer}
import org.apache.flink.api.scala._
import org.apache.flink.table.api._
import org.apache.flink.table.descriptors.Csv

object KafkaTableApp extends FlinkStreamTable {

  override def handle(): Unit = {

    //connect kafka data
    context
      .connect(Kafka("hello", KafkaVer.UNIVERSAL))
      .withFormat(new Csv)
      .withSchema(
        "id" -> DataTypes.STRING(),
        "name" -> DataTypes.STRING()
      )
      .createTemporaryTable("kafka2Table")

    val ds = context.$fromCollection(
      List(
        "flink,apapche flink",
        "kafka,apapche kafka",
        "spark,spark",
        "zookeeper,apapche zookeeper",
        "hadoop,apapche hadoop"
      )
    ).map(x => {
      val array = x.split(",")
      Entity(array.head, array.last)
    })

    context.createTemporaryView("kafkaSource", ds)

    //kafka to table
    val table1: Table = context.from("kafkaSource")
    table1.>>[Entity].print("stream print==>")

    val table: Table = context.from("kafka2Table")

    //print sink
    table.>>[Entity].print("print==>")

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

case class Entity(id: String, name: String)
