package com.streamxhub.streamx.test.tablesql

import com.streamxhub.streamx.flink.core.scala.FlinkTable

object HelloFlinkSQL extends FlinkTable {

  override def handle(): Unit = {
    context.sql("sql1")
  }
}
