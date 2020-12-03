package com.streamxhub.tablesql.test

import com.streamxhub.flink.core.scala.{FlinkTableSQL, TableSQLContext}
import org.apache.flink.table.api.{DataTypes}
import org.apache.flink.table.descriptors._
import org.apache.flink.api.scala._

object FileTableApp extends FlinkTableSQL {

  override def handler(context: TableSQLContext): Unit = {
    context.connect(new FileSystem().path("data/in/order.txt"))
      .withFormat(new OldCsv())
      .withSchema(new Schema()
        .field("id", DataTypes.STRING())
        .field("count", DataTypes.INT())
        .field("amount", DataTypes.STRING())
      )
      .createTemporaryTable("orders")
    val orders = context.from("orders")
    orders.$append[(String, Int, String)].print()
  }

}
