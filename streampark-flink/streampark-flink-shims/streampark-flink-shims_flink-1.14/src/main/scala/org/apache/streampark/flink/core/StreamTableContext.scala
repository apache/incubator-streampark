/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streampark.flink.core

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.graph.StreamGraph
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.{Schema, Table, TableDescriptor}
import org.apache.flink.table.api.bridge.scala.{StreamStatementSet, StreamTableEnvironment}
import org.apache.flink.table.connector.ChangelogMode
import org.apache.flink.table.module.ModuleEntry
import org.apache.flink.table.sources.TableSource
import org.apache.flink.table.types.AbstractDataType
import org.apache.flink.types.Row

/**
 * Integration api of stream and table
 *
 * @param parameter
 *   parameter
 * @param streamEnv
 *   streamEnv
 * @param tableEnv
 *   tableEnv
 */
class StreamTableContext(
    override val parameter: ParameterTool,
    private val streamEnv: StreamExecutionEnvironment,
    private val tableEnv: StreamTableEnvironment)
  extends FlinkStreamTableTrait(parameter, streamEnv, tableEnv) {

  /** for scala */
  def this(args: (ParameterTool, StreamExecutionEnvironment, StreamTableEnvironment)) =
    this(args._1, args._2, args._3)

  /** for Java */
  def this(args: StreamTableEnvConfig) =
    this(FlinkTableInitializer.initialize(args))

  override def fromDataStream[T](dataStream: DataStream[T], schema: Schema): Table =
    tableEnv.fromDataStream[T](dataStream, schema)

  override def fromChangelogStream(dataStream: DataStream[Row]): Table =
    tableEnv.fromChangelogStream(dataStream)

  override def fromChangelogStream(dataStream: DataStream[Row], schema: Schema): Table =
    tableEnv.fromChangelogStream(dataStream, schema)

  override def fromChangelogStream(
      dataStream: DataStream[Row],
      schema: Schema,
      changelogMode: ChangelogMode): Table =
    tableEnv.fromChangelogStream(dataStream, schema, changelogMode)

  override def createTemporaryView[T](
      path: String,
      dataStream: DataStream[T],
      schema: Schema): Unit =
    tableEnv.createTemporaryView[T](path, dataStream, schema)

  override def toDataStream(table: Table): DataStream[Row] = {
    isConvertedToDataStream = true
    tableEnv.toDataStream(table)
  }

  override def toDataStream[T](table: Table, targetClass: Class[T]): DataStream[T] = {
    isConvertedToDataStream = true
    tableEnv.toDataStream[T](table, targetClass)
  }

  override def toDataStream[T](table: Table, targetDataType: AbstractDataType[_]): DataStream[T] = {
    isConvertedToDataStream = true
    tableEnv.toDataStream[T](table, targetDataType)
  }

  override def toChangelogStream(table: Table): DataStream[Row] = {
    isConvertedToDataStream = true
    tableEnv.toChangelogStream(table)
  }

  override def toChangelogStream(table: Table, targetSchema: Schema): DataStream[Row] = {
    isConvertedToDataStream = true
    tableEnv.toChangelogStream(table, targetSchema)
  }

  override def toChangelogStream(
      table: Table,
      targetSchema: Schema,
      changelogMode: ChangelogMode): DataStream[Row] = {
    isConvertedToDataStream = true
    tableEnv.toChangelogStream(table, targetSchema, changelogMode)
  }

  override def useModules(strings: String*): Unit =
    tableEnv.useModules(strings: _*)

  override def listFullModules(): Array[ModuleEntry] =
    tableEnv.listFullModules()

  /**
   * flink 1.14, need to implement
   *
   * @param path
   * @param descriptor
   */
  def createTable(path: String, descriptor: TableDescriptor): Unit = {
    tableEnv.createTable(path, descriptor)
  }

  def createTemporaryTable(path: String, descriptor: TableDescriptor): Unit = {
    tableEnv.createTemporaryTable(path, descriptor)
  }

  def from(descriptor: TableDescriptor): org.apache.flink.table.api.Table =
    tableEnv.from(descriptor)

  def $getStreamGraph(clearTransformations: Boolean): StreamGraph =
    this.streamEnv.getStreamGraph(clearTransformations)

  override def createStatementSet(): StreamStatementSet =
    tableEnv.createStatementSet()

  @deprecated def fromTableSource(source: TableSource[_]): Table =
    tableEnv.fromTableSource(source)

  @deprecated def insertInto(table: Table, sinkPath: String, sinkPathContinued: String*): Unit =
    tableEnv.insertInto(table, sinkPath, sinkPathContinued: _*)

  @deprecated def insertInto(targetPath: String, table: Table): Unit =
    tableEnv.insertInto(targetPath, table)

  @deprecated def explain(table: Table): String = tableEnv.explain(table)

  @deprecated def explain(table: Table, extended: Boolean): String =
    tableEnv.explain(table, extended)

  @deprecated def explain(extended: Boolean): String =
    tableEnv.explain(extended)

  @deprecated def sqlUpdate(stmt: String): Unit = tableEnv.sqlUpdate(stmt)

}
