package com.streamxhub.streamx.test.tablesql

import com.streamxhub.streamx.flink.core.scala.FlinkStreamTable
import org.apache.flink.api.scala._
import org.apache.flink.table.api.Table

object MixinTableApp extends FlinkStreamTable {

  override def handle(): Unit = {

    val mySource = context.$fromCollection(
      List(
        "kafka,apapche kafka",
        "spark,spark",
        "flink,apapche flink",
        "zookeeper,apapche zookeeper",
        "hadoop,apapche hadoop"
      )
    ).map(x => {
      val array = x.split(",")
      Entity(array.head, array.last)
    })

    context.createTemporaryView("mySource", mySource)

    //kafka to table
    val table1: Table = context.from("mySource")
    table1.toAppendStream[Entity].print("stream print==>")

  }
}
