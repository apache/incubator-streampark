package com.streamxhub.streamx.test.tablesql

import com.streamxhub.streamx.flink.core.scala.FlinkStreamTable

object HelloFlinkSQL extends FlinkStreamTable {

  override def handle(): Unit = {
    context.sql("sql")
  }
}
