/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.flink.core.scala

import com.streamxhub.common.conf.ConfigConst.{KEY_APP_HOME, KEY_APP_NAME, KEY_FLINK_APP_NAME, KEY_FLINK_SQL, LOGO}
import com.streamxhub.common.util.{DeflaterUtils, Logger, SystemPropertyUtils}
import com.streamxhub.flink.core.scala.util.FlinkTableInitializer
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.api.{ExplainDetail, StatementSet, Table, TableConfig, TableEnvironment, TableResult}
import org.apache.flink.table.catalog.Catalog
import org.apache.flink.table.descriptors.{ConnectTableDescriptor, ConnectorDescriptor}
import org.apache.flink.table.expressions.Expression
import org.apache.flink.table.functions.{ScalarFunction, UserDefinedFunction}
import org.apache.flink.table.module.Module
import org.apache.flink.table.sinks.TableSink
import org.apache.flink.table.sources.TableSource
import org.apache.flink.table.types.AbstractDataType

import java.lang
import java.util.Optional
import scala.util.{Failure, Success, Try}

class TableContext(val parameter: ParameterTool,
                   private val tableEnv: TableEnvironment) extends TableEnvironment {


  /**
   * for scala
   *
   * @param args
   */
  def this(args: (ParameterTool, TableEnvironment)) = this(args._1, args._2)

  /**
   *
   * @param args
   */
  def this(args: Array[String]) = this(FlinkTableInitializer.initTable(args))

  /**
   * 推荐使用该Api启动任务...
   *
   * @return
   */
  def start(): JobExecutionResult = {
    val appName = (parameter.get(KEY_APP_NAME(), null), parameter.get(KEY_FLINK_APP_NAME, null)) match {
      case (appName: String, _) => appName
      case (null, appName: String) => appName
      case _ => ""
    }
    execute(appName)
  }

  override def execute(jobName: String): JobExecutionResult = {
    println(s"\033[95;1m$LOGO\033[1m\n")
    println(s"[StreamX] FlinkTable $jobName Starting...")
    tableEnv.execute(jobName)
  }

  private[flink] def getSQL(): String = {
    Try(DeflaterUtils.unzipString(parameter.get(KEY_FLINK_SQL()))) match {
      case Success(value) => value
      case Failure(exception) => {
        new ExceptionInInitializerError(s"[StreamX] init sql error.$exception")
        null
      }
    }
  }

  override def fromValues(values: Expression*): Table = tableEnv.fromValues(values)

  override def fromValues(rowType: AbstractDataType[_], values: Expression*): Table = tableEnv.fromValues(rowType, values: _*)

  override def fromValues(values: lang.Iterable[_]): Table = tableEnv.fromValues(values)

  override def fromValues(rowType: AbstractDataType[_], values: lang.Iterable[_]): Table = tableEnv.fromValues(rowType, values)

  override def registerCatalog(catalogName: String, catalog: Catalog): Unit = tableEnv.registerCatalog(catalogName, catalog)

  override def getCatalog(catalogName: String): Optional[Catalog] = tableEnv.getCatalog(catalogName)

  override def loadModule(moduleName: String, module: Module): Unit = tableEnv.loadModule(moduleName, module)

  override def unloadModule(moduleName: String): Unit = tableEnv.unloadModule(moduleName)

  override def createTemporarySystemFunction(name: String, functionClass: Class[_ <: UserDefinedFunction]): Unit = tableEnv.createTemporarySystemFunction(name, functionClass)

  override def createTemporarySystemFunction(name: String, functionInstance: UserDefinedFunction): Unit = tableEnv.createTemporarySystemFunction(name, functionInstance)

  override def dropTemporarySystemFunction(name: String): Boolean = tableEnv.dropTemporarySystemFunction(name)

  override def createFunction(path: String, functionClass: Class[_ <: UserDefinedFunction]): Unit = tableEnv.createFunction(path, functionClass)

  override def createFunction(path: String, functionClass: Class[_ <: UserDefinedFunction], ignoreIfExists: Boolean): Unit = tableEnv.createFunction(path, functionClass)

  override def dropFunction(path: String): Boolean = tableEnv.dropFunction(path)

  override def createTemporaryFunction(path: String, functionClass: Class[_ <: UserDefinedFunction]): Unit = tableEnv.createTemporaryFunction(path, functionClass)

  override def createTemporaryFunction(path: String, functionInstance: UserDefinedFunction): Unit = tableEnv.createTemporaryFunction(path, functionInstance)

  override def dropTemporaryFunction(path: String): Boolean = tableEnv.dropTemporaryFunction(path)

  override def createTemporaryView(path: String, view: Table): Unit = tableEnv.createTemporaryView(path, view)

  override def from(path: String): Table = tableEnv.from(path)

  override def listCatalogs(): Array[String] = tableEnv.listCatalogs()

  override def listModules(): Array[String] = tableEnv.listModules()

  override def listDatabases(): Array[String] = tableEnv.listDatabases()

  override def listTables(): Array[String] = tableEnv.listTables()

  override def listViews(): Array[String] = tableEnv.listViews()

  override def listTemporaryTables(): Array[String] = tableEnv.listTemporaryTables

  override def listTemporaryViews(): Array[String] = tableEnv.listTemporaryViews()

  override def listUserDefinedFunctions(): Array[String] = tableEnv.listUserDefinedFunctions()

  override def listFunctions(): Array[String] = tableEnv.listFunctions()

  override def dropTemporaryTable(path: String): Boolean = tableEnv.dropTemporaryTable(path)

  override def dropTemporaryView(path: String): Boolean = tableEnv.dropTemporaryView(path)

  override def explainSql(statement: String, extraDetails: ExplainDetail*): String = tableEnv.explainSql(statement, extraDetails: _*)

  override def sqlQuery(query: String): Table = tableEnv.sqlQuery(query)

  override def executeSql(statement: String): TableResult = tableEnv.executeSql(statement)

  override def getCurrentCatalog: String = tableEnv.getCurrentCatalog

  override def useCatalog(catalogName: String): Unit = tableEnv.useCatalog(catalogName)

  override def getCurrentDatabase: String = tableEnv.getCurrentDatabase

  override def useDatabase(databaseName: String): Unit = tableEnv.useDatabase(databaseName)

  override def getConfig: TableConfig = tableEnv.getConfig

  override def createStatementSet(): StatementSet = tableEnv.createStatementSet()


  /**
   * deprecated!!! what are you fucking for??? don't call this method
   *
   * @param name
   * @param dataStream
   * @tparam T
   */
  override def fromTableSource(source: TableSource[_]): Table = tableEnv.fromTableSource(source)

  override def registerFunction(name: String, function: ScalarFunction): Unit = tableEnv.registerFunction(name, function)

  override def registerTable(name: String, table: Table): Unit = tableEnv.registerTable(name, table)

  override def registerTableSource(name: String, tableSource: TableSource[_]): Unit = tableEnv.registerTableSource(name, tableSource)

  override def registerTableSink(name: String, fieldNames: Array[String], fieldTypes: Array[TypeInformation[_]], tableSink: TableSink[_]): Unit = tableEnv.registerTableSink(name, fieldNames, fieldTypes, tableSink)

  override def registerTableSink(name: String, configuredSink: TableSink[_]): Unit = tableEnv.registerTableSink(name, configuredSink)

  override def scan(tablePath: String*): Table = tableEnv.scan(tablePath: _*)

  override def insertInto(table: Table, sinkPath: String, sinkPathContinued: String*): Unit = tableEnv.insertInto(table, sinkPath, sinkPathContinued: _*)

  override def insertInto(targetPath: String, table: Table): Unit = tableEnv.insertInto(targetPath, table)

  override def explain(table: Table): String = tableEnv.explain(table)

  override def explain(table: Table, extended: Boolean): String = tableEnv.explain(table, extended)

  override def explain(extended: Boolean): String = tableEnv.explain(extended)

  override def getCompletionHints(statement: String, position: Int): Array[String] = tableEnv.getCompletionHints(statement, position)

  override def sqlUpdate(stmt: String): Unit = tableEnv.sqlUpdate(stmt)

  override def connect(connectorDescriptor: ConnectorDescriptor): ConnectTableDescriptor = tableEnv.connect(connectorDescriptor)
}

trait FlinkTable extends Logger {

  var jobExecutionResult: JobExecutionResult = _

  final implicit lazy val parameter: ParameterTool = context.parameter

  private[this] var context: TableContext = _

  def main(args: Array[String]): Unit = {
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkTable])
    context = new TableContext(FlinkTableInitializer.initTable(args))
    beforeStart(context)
    handle(context)
    jobExecutionResult = context.start()
  }

  /**
   * 用户可覆盖次方法...
   *
   */
  def beforeStart(context: TableContext): Unit = {}

  def handle(context: TableContext): Unit

}
