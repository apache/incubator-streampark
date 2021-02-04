package com.streamxhub.streamx.test.tablesql

import com.streamxhub.streamx.flink.core.scala.FlinkStreamTable
import org.apache.flink.api.scala._
import org.apache.flink.table.api._
import org.apache.flink.table.descriptors.{FileSystem, OldCsv, Schema}

object FileTableApp extends FlinkStreamTable {

  override def handle(): Unit = {
    context.connect(new FileSystem().path("data/in/order.txt"))
      .withFormat(new OldCsv())
      .withSchema(new Schema()
        .field("id", DataTypes.STRING())
        .field("count", DataTypes.INT())
        .field("amount", DataTypes.STRING())
      )
      .createTemporaryTable("orders")
    val orders = context.from("orders")
    orders.>>[(String, Int, String)].print()
  }

}
