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
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.{StatementSet, Table}
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.descriptors.{ConnectorDescriptor, StreamTableDescriptor}
import org.apache.flink.table.sources.TableSource

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

  @deprecated override def connect(
      connectorDescriptor: ConnectorDescriptor): StreamTableDescriptor =
    tableEnv.connect(connectorDescriptor)

  def $getStreamGraph(jobName: String): StreamGraph =
    this.streamEnv.getStreamGraph(jobName)

  def $getStreamGraph(jobName: String, clearTransformations: Boolean): StreamGraph =
    this.streamEnv.getStreamGraph(jobName, clearTransformations)

  override def createStatementSet(): StatementSet =
    tableEnv.createStatementSet()

  @deprecated override def fromTableSource(source: TableSource[_]): Table =
    tableEnv.fromTableSource(source)

  @deprecated override def insertInto(
      table: Table,
      sinkPath: String,
      sinkPathContinued: String*): Unit =
    tableEnv.insertInto(table, sinkPath, sinkPathContinued: _*)

  @deprecated override def insertInto(targetPath: String, table: Table): Unit =
    tableEnv.insertInto(targetPath, table)

  @deprecated override def explain(table: Table): String =
    tableEnv.explain(table)

  @deprecated override def explain(table: Table, extended: Boolean): String =
    tableEnv.explain(table, extended)

  @deprecated override def explain(extended: Boolean): String =
    tableEnv.explain(extended)

  @deprecated override def sqlUpdate(stmt: String): Unit =
    tableEnv.sqlUpdate(stmt)
}
