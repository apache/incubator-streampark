package com.streamxhub.test.tablesql

import com.streamxhub.flink.core.scala.{FlinkTableSQL, TableContext}
import org.apache.flink.table.api.DataTypes
import org.apache.flink.table.descriptors.{FileSystem, OldCsv, Schema}

object FileTableApp extends FlinkTableSQL {

  override def handler(context: TableContext): Unit = {
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
