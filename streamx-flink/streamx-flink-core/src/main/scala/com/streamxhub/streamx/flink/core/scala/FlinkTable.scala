/*
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
package com.streamxhub.streamx.flink.core.scala

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{Logger, SystemPropertyUtils}
import com.streamxhub.streamx.flink.core.scala.ext.TableExt
import com.streamxhub.streamx.flink.core.scala.util.{FlinkTableInitializer, FlinkTableTrait, TableEnvConfig}
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.api.{ExplainDetail, StatementSet, Table, TableConfig, TableEnvironment, TableResult}
import org.apache.flink.table.catalog.Catalog
import org.apache.flink.table.descriptors.{ConnectTableDescriptor, ConnectorDescriptor}
import org.apache.flink.table.expressions.Expression
import org.apache.flink.table.functions.{ScalarFunction, UserDefinedFunction}
import org.apache.flink.table.module.Module
import org.apache.flink.table.sources.TableSource
import org.apache.flink.table.types.AbstractDataType

import java.lang
import java.util.Optional

class TableContext(val parameter: ParameterTool,
                   private val tableEnv: TableEnvironment) extends TableEnvironment with FlinkTableTrait {


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
  def this(args: TableEnvConfig) = this(FlinkTableInitializer.initJavaTable(args))

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

  /**
   *
   * @param sql 配置文件中的sql名称,或者一段sql
   */
  def sql(sql: String = null): Unit = super.callSql(sql, parameter, this)

  private[flink] def sqlWithCallBack(sql: String = null)(implicit callback:Unit => String = null): Unit = super.callSql(sql, parameter, this)

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
  @Deprecated override def fromTableSource(source: TableSource[_]): Table = tableEnv.fromTableSource(source)

  @Deprecated override def registerFunction(name: String, function: ScalarFunction): Unit = tableEnv.registerFunction(name, function)

  @Deprecated override def registerTable(name: String, table: Table): Unit = tableEnv.registerTable(name, table)

  @Deprecated override def scan(tablePath: String*): Table = tableEnv.scan(tablePath: _*)

  @Deprecated override def insertInto(table: Table, sinkPath: String, sinkPathContinued: String*): Unit = tableEnv.insertInto(table, sinkPath, sinkPathContinued: _*)

  @Deprecated override def insertInto(targetPath: String, table: Table): Unit = tableEnv.insertInto(targetPath, table)

  @Deprecated override def explain(table: Table): String = tableEnv.explain(table)

  @Deprecated override def explain(table: Table, extended: Boolean): String = tableEnv.explain(table, extended)

  @Deprecated override def explain(extended: Boolean): String = tableEnv.explain(extended)

  @Deprecated override def getCompletionHints(statement: String, position: Int): Array[String] = tableEnv.getCompletionHints(statement, position)

  @Deprecated override def sqlUpdate(stmt: String): Unit = tableEnv.sqlUpdate(stmt)

  @Deprecated override def connect(connectorDescriptor: ConnectorDescriptor): ConnectTableDescriptor = tableEnv.connect(connectorDescriptor)
}

trait FlinkTable extends Logger {

  var jobExecutionResult: JobExecutionResult = _

  final implicit def descriptorExt(table: ConnectTableDescriptor): TableExt.ConnectTableDescriptor = new TableExt.ConnectTableDescriptor(table)

  final implicit lazy val parameter: ParameterTool = context.parameter

  implicit var context: TableContext = _

  def main(args: Array[String]): Unit = {
    init(args)
    ready()
    handle()
    jobExecutionResult = context.start()
    destroy()
  }

  private[this] def init(args: Array[String]): Unit = {
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkTable])
    context = new TableContext(FlinkTableInitializer.initTable(args, config))
  }

  /**
   * 用户可覆盖次方法...
   *
   */
  def ready(): Unit = {}

  def config(tableConfig: TableConfig, parameter: ParameterTool): Unit = {}

  def handle(): Unit

  def destroy(): Unit = {}

}
