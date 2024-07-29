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

import org.apache.streampark.common.util.Utils

import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.api.{Table, TableDescriptor, TableEnvironment}
import org.apache.flink.table.module.ModuleEntry
import org.apache.flink.table.sources.TableSource

class TableContext(override val parameter: ParameterTool, private val tableEnv: TableEnvironment)
  extends FlinkTableTrait(parameter, tableEnv) {

  /**
   * for scala
   *
   * @param args
   */
  def this(args: (ParameterTool, TableEnvironment)) = this(args._1, args._2)

  /**
   * for java
   *
   * @param args
   */
  def this(args: TableEnvConfig) = {
    this(FlinkTableInitializer.initialize(args))
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

  def from(descriptor: TableDescriptor): Table = tableEnv.from(descriptor)

  @deprecated override def execute(jobName: String): JobExecutionResult = {
    Utils.printLogo(s"FlinkTable $jobName Starting...")
    null
  }

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
