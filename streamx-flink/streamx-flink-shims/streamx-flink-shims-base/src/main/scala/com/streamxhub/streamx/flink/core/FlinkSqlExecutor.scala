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
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.core.SqlCommand._
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.{ConfigOption, Configuration}
import org.apache.flink.table.api.TableEnvironment
import org.apache.flink.table.api.config.{ExecutionConfigOptions, OptimizerConfigOptions, TableConfigOptions}

import java.util
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.{HashMap => JavaHashMap, Map => JavaMap}
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
    require(flinkSql != null && flinkSql.trim.nonEmpty, "verify failed: flink sql cannot be empty")

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
        // For display sql statement result information
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
        case DESC | DESCRIBE =>
          val schema = context.scan(args).getSchema
          val builder = new mutable.StringBuilder()
          builder.append("Column\tType\n")
          for (i <- 0 to schema.getFieldCount) {
            builder.append(schema.getFieldName(i).get() + "\t" + schema.getFieldDataType(i).get() + "\n")
          }
          callback(builder.toString())
        case EXPLAIN =>
          val tableResult = context.executeSql(x.originSql)
          val r = tableResult.collect().next().getField(0).toString
          callback(r)

        //For specific statement, such as: SET、RESET、INSERT、SELECT
        case SET =>
          if (!tableConfigOptions.containsKey(args)) {
            throw new IllegalArgumentException(s"$args is not a valid table/sql config, please check link: https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/dev/table/config")
          }
          val operand = x.operands(1)
          context.getConfig.getConfiguration.setString(args, operand)
          logInfo(s"$command: $args --> $operand")
        case RESET | RESET_ALL =>
          val confDataField = classOf[Configuration].getDeclaredField("confData")
          confDataField.setAccessible(true)
          val confData = confDataField.get(context.getConfig.getConfiguration).asInstanceOf[util.HashMap[String, AnyRef]]
          confData.synchronized {
            if (x.command == RESET) {
              confData.remove(args)
            } else {
              confData.clear()
            }
          }
          logInfo(s"$command: $args")
        case BEGIN_STATEMENT_SET | END_STATEMENT_SET =>
          logWarn(s"SQL Client Syntax: ${x.command.name} ")
        case INSERT => insertArray += x.originSql
        case SELECT =>
          logError("StreamX dose not support 'SELECT' statement now!")
          throw new RuntimeException("StreamX dose not support 'select' statement now!")
        case _ => try {
          lock.lock()
          val result = context.executeSql(x.originSql)
          logInfo(s"$command:$args")
        } finally {
          if (lock.isHeldByCurrentThread) {
            lock.unlock()
          }
        }
      }
    })

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
    } else {
      logError("No 'INSERT' statement to trigger the execution of the Flink job.")
      throw new RuntimeException("No 'INSERT' statement to trigger the execution of the Flink job.")
    }

    logInfo(s"\n\n\n==============flinkSql==============\n\n $flinkSql\n\n============================\n\n\n")
  }


}
