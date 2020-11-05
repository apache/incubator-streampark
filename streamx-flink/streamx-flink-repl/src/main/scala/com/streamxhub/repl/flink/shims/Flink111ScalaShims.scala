package com.streamxhub.repl.flink.shims

import org.apache.flink.api.scala.DataSet
import org.apache.flink.table.api.Table
import org.apache.flink.table.api.bridge.scala.BatchTableEnvironment
import org.apache.flink.types.Row
import org.apache.flink.api.scala._

object Flink111ScalaShims {

  def fromDataSet(btenv: BatchTableEnvironment, ds: DataSet[_]): Table = {
    btenv.fromDataSet(ds)
  }

  def toDataSet(btenv: BatchTableEnvironment, table: Table): DataSet[Row] = {
    btenv.toDataSet[Row](table)
  }
}
