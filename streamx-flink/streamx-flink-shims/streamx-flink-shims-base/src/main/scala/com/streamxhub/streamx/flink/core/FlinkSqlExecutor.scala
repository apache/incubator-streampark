/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.flink.core

import com.streamxhub.streamx.common.conf.ConfigConst.KEY_FLINK_SQL
import com.streamxhub.streamx.common.enums.SqlErrorType
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.core.SqlCommand._
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.{ConfigOption, Configuration}
import org.apache.flink.table.api.config.{ExecutionConfigOptions, OptimizerConfigOptions, TableConfigOptions}
import org.apache.flink.table.api.{SqlDialect, TableEnvironment}
import org.apache.flink.table.catalog.hive.HiveCatalog
import org.apache.flink.table.functions.UserDefinedFunction
import org.apache.flink.table.module.hive.HiveModule

import java.util
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.Matcher
import java.util.{Objects, HashMap => JavaHashMap, Map => JavaMap}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

object FlinkSqlExecutor extends Logger {

  private[this] val lock = new ReentrantReadWriteLock().writeLock

  /**
   * all the available sql config options. see: https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/dev/table/config
   */
  lazy val tableConfigOptions: JavaMap[String, ConfigOption[_]] = {
    def extractConfig(clazz: Class[_]): JavaMap[String, ConfigOption[_]] = {
      val configOptions = new JavaHashMap[String, ConfigOption[_]]
      clazz.getDeclaredFields.foreach(field => {
        if (field.getType.isAssignableFrom(classOf[ConfigOption[_]])) {
          Try {
            val configOption = field.get(classOf[ConfigOption[_]]).asInstanceOf[ConfigOption[_]]
            configOptions.put(configOption.key, configOption)
          } match {
            case Success(_) =>
            case Failure(e) => logError("Fail to get ConfigOption", e)
          }
        }
      })
      configOptions
    }

    val configOptions = new JavaHashMap[String, ConfigOption[_]]
    val configList = List(
      //classOf[PythonOptions],
      classOf[ExecutionConfigOptions],
      classOf[OptimizerConfigOptions],
      classOf[TableConfigOptions]
    )
    configList.foreach(x => configOptions.putAll(extractConfig(x)))
    configOptions
  }

  private[streamx] def executeSql(sql: String, parameter: ParameterTool, context: TableEnvironment)(implicit callbackFunc: String => Unit = null): Unit = {
    val flinkSql: String = if (sql == null || sql.isEmpty) parameter.get(KEY_FLINK_SQL()) else parameter.get(sql)
    val sqlEmptyError = SqlError(SqlErrorType.VERIFY_FAILED, "sql is empty", sql).toString
    require(flinkSql != null && flinkSql.trim.nonEmpty, sqlEmptyError)

    def callback(r: String): Unit = {
      callbackFunc match {
        case null => logInfo(r)
        case x => x(r)
      }
    }

    val insertArray = new ArrayBuffer[String]()
    SqlCommandParser.parseSQL(flinkSql).foreach(x => {
      val args = if (x.operands.isEmpty) null else x.operands.head
      val command = x.command.name
      x.command match {
        case USE =>
          context.useDatabase(args)
          logInfo(s"$command: $args")
        case CREATE_CATALOG =>
          val matcher: Matcher = x.command.matcher
          val catalogName = matcher.group(1).trim
          // [k1, v1, k2, v2]
          val split: Array[String] = matcher.group(2).trim().replaceAll(",", " ").replace("=", " ").replace("'", " ").split("\\s+")
          val catalogConf = mutable.HashMap[String, String]()
          for (i <- split.indices) {
            if (i % 2 == 0) catalogConf.put(split(i).trim, split(i + 1).trim)
          }
          val hiveCatalog = new HiveCatalog(catalogName, "default", catalogConf.getOrElse("conf", null), catalogConf.getOrElse("conf", null), catalogConf.getOrElse("version", null))
          context.registerCatalog(catalogName, hiveCatalog)
          context.loadModule(catalogName, if (catalogConf.contains("version")) new HiveModule(catalogConf("version")) else new HiveModule())
          context.useCatalog(catalogName)
        case CREATE_FUNCTION =>
          val matcher = x.command.matcher
          val funcType = matcher.group(1).toLowerCase().trim
          val hasIfNotExist = Objects.equals(matcher.group(2).trim, "is not exists")
          // 解析出注册的自定义函数的所有信息
          val funcNameInfo = matcher.group(3).trim
          var catalogName: String = ""
          var databaseName: String = ""
          var funcName: String = ""
          if (funcNameInfo.contains(".")) {
            val split = funcNameInfo.split("\\.")
            if (split.size == 3) {
              catalogName = split(0)
              databaseName = split(1)
              funcName = split(2)
            } else if (split.size == 2) {
              databaseName = split(0)
              funcName = split(1)
            }
          } else {
            funcName = funcNameInfo
          }

          // 如果原始 sql 语句中包含 if not exists 语句，则需要判断是否已经包含有对应函数名
          val canCreate: Boolean = if (hasIfNotExist) {
            if (context.listUserDefinedFunctions().contains(funcName)) false else true
          } else {
            true
          }
          if (canCreate) {
            // 获取当前的 catalog 和数据库，注册完成之后，需要重新设置回去
            val currentCatalog = context.getCurrentCatalog
            val currentDatabase = context.getCurrentDatabase
            val identifier = matcher.group(4).trim
            if (catalogName.nonEmpty) context.useCatalog(catalogName)
            if (databaseName.nonEmpty) context.useDatabase(databaseName)
            val subClass = Class.forName(identifier).asSubclass(classOf[UserDefinedFunction])
            // 注册函数
            if (funcType.contains("temporary")) {
              if (funcType.contains("system")) {
                context.createTemporarySystemFunction(funcName, subClass)
              } else {
                context.createTemporaryFunction(funcName, subClass)
              }
            } else {
              context.createFunction(funcName, subClass)
            }
            context.useCatalog(currentCatalog)
            context.useDatabase(currentDatabase)
          }
        case USE_CATALOG =>
          context.useCatalog(args)
          logInfo(s"$command: $args")
        case SHOW_CATALOGS =>
          val catalogs = context.listCatalogs
          callback(s"$command: ${catalogs.mkString("\n")}")
        case SHOW_CURRENT_CATALOG =>
          val catalog = context.getCurrentCatalog
          callback(s"$command: $catalog")
        case SHOW_DATABASES =>
          val databases = context.listDatabases
          callback(s"$command: ${databases.mkString("\n")}")
        case SHOW_CURRENT_DATABASE =>
          val database = context.getCurrentDatabase
          callback(s"$command: $database")
        case SHOW_TABLES =>
          val tables = context.listTables().filter(!_.startsWith("UnnamedTable"))
          callback(s"$command: ${tables.mkString("\n")}")
        case SHOW_FUNCTIONS =>
          val functions = context.listUserDefinedFunctions()
          callback(s"$command: ${functions.mkString("\n")}")
        case SHOW_MODULES =>
          val modules = context.listModules()
          callback(s"$command: ${modules.mkString("\n")}")
        case SET =>
          if (!tableConfigOptions.containsKey(args)) {
            throw new IllegalArgumentException(s"$args is not a valid table/sql config, please check link: https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/dev/table/config")
          }
          val operand = x.operands(1)
          if (TableConfigOptions.TABLE_SQL_DIALECT.key().equalsIgnoreCase(args)) {
            Try(SqlDialect.valueOf(operand.toUpperCase()))
              .map(context.getConfig.setSqlDialect(_))
              .getOrElse(throw new IllegalArgumentException(s"$operand is not a valid dialect"))
          } else {
            context.getConfig.getConfiguration.setString(args, operand)
          }
          logInfo(s"$command: $args --> $operand")
        case RESET =>
          val confDataField = classOf[Configuration].getDeclaredField("confData")
          confDataField.setAccessible(true)
          val confData = confDataField.get(context.getConfig.getConfiguration).asInstanceOf[util.HashMap[String, AnyRef]]
          if (args.toUpperCase == "ALL") {
            confData.synchronized {
              confData.clear()
            }
          } else {
            confData.synchronized {
              confData.remove(args)
            }
          }
          logInfo(s"$command: $args")
        case DESC | DESCRIBE
        =>
          val schema = context.scan(args).getSchema
          val builder = new StringBuilder()
          builder.append("Column\tType\n")
          for (i <- 0 to schema.getFieldCount) {
            builder.append(schema.getFieldName(i).get() + "\t" + schema.getFieldDataType(i).get() + "\n")
          }
          callback(builder.toString())
        case EXPLAIN =>
          val tableResult = context.executeSql(x.originSql)
          val r = tableResult.collect().next().getField(0).toString
          callback(r)
        case INSERT_INTO | INSERT_OVERWRITE
        => insertArray += x.originSql
        case SELECT =>
          throw new Exception(s"[StreamX] Unsupported SELECT in current version.")
        case BEGIN_STATEMENT_SET | END_STATEMENT_SET
        =>
          logWarn(s"SQL Client Syntax: ${x.command.name} ")
        case INSERT_INTO | INSERT_OVERWRITE |
             DROP_FUNCTION | ALTER_FUNCTION |
             DROP_CATALOG |
             CREATE_TABLE | DROP_TABLE | ALTER_TABLE |
             CREATE_VIEW | DROP_VIEW |
             CREATE_DATABASE | DROP_DATABASE | ALTER_DATABASE
        =>
          try {
            lock.lock()
            val result = context.executeSql(x.originSql)
            logInfo(s"$command:$args")
          } finally {
            if (lock.isHeldByCurrentThread) {
              lock.unlock()
            }
          }
        case _ => throw new Exception(s"[StreamX] Unsupported command: ${x.command}")
      }
    }
    )

    if (insertArray.nonEmpty) {
      val statementSet = context.createStatementSet()
      insertArray.foreach(statementSet.addInsertSql)
      statementSet.execute() match {
        case t if t != null =>
          Try(t.getJobClient.get.getJobID).getOrElse(null) match {
            case x if x != null => logInfo(s"jobId:$x")
            case _ =>
          }
        case _ =>
      }
    }

    logInfo(s"\n\n\n==============flinkSql==============\n\n $flinkSql\n\n============================\n\n\n")
  }
}
