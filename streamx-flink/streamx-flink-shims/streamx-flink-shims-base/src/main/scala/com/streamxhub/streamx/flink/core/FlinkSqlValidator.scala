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

import com.streamxhub.streamx.common.enums.FlinkSqlValidationFailedType
import com.streamxhub.streamx.common.util.{ExceptionUtils, Logger}
import com.streamxhub.streamx.flink.core.SqlCommand._
import org.apache.calcite.config.Lex
import org.apache.calcite.sql.parser.SqlParser
import org.apache.calcite.sql.parser.SqlParser.Config
import org.apache.flink.sql.parser.validate.FlinkSqlConformance
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.SqlDialect.{DEFAULT, HIVE}
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.planner.delegation.FlinkSqlParserFactories
import org.apache.flink.table.planner.utils.TableConfigUtils

import scala.util.{Failure, Try}

object FlinkSqlValidator extends Logger {

  private[this] val FLINK112_CALCITE_PARSER_CLASS = "org.apache.flink.table.planner.calcite.CalciteParser"

  private[this] val FLINK113_CALCITE_PARSER_CLASS = "org.apache.flink.table.planner.parse.CalciteParser"

  private[this] val SYNTAX_ERROR_REGEXP = ".*at\\sline\\s(\\d+),\\scolumn\\s(\\d+).*".r

  private[this] lazy val sqlParserConfig = {
    val tableConfig = StreamTableEnvironment.create(
      StreamExecutionEnvironment.getExecutionEnvironment,
      EnvironmentSettings
        .newInstance
        .useBlinkPlanner
        .inStreamingMode
        .build
    ).getConfig

    TableConfigUtils.getCalciteConfig(tableConfig).getSqlParserConfig.getOrElse {
      val conformance = tableConfig.getSqlDialect match {
        case HIVE => FlinkSqlConformance.HIVE
        case DEFAULT => FlinkSqlConformance.DEFAULT
        case _ =>
          throw new UnsupportedOperationException(s"unsupported dialect: ${tableConfig.getSqlDialect}")
      }
      SqlParser.config
        .withParserFactory(FlinkSqlParserFactories.create(conformance))
        .withConformance(conformance)
        .withLex(Lex.JAVA)
        .withIdentifierMaxLength(256)
    }
  }

  def verifySql(sql: String): FlinkSqlValidationResult = {
    val sqlCommands = SqlCommandParser.parseSQL(sql, r => return r)
    for (call <- sqlCommands) {
      lazy val args = call.operands.head
      lazy val command = call.command
      command match {
        case SET | RESET =>
          if (!FlinkSqlExecutor.tableConfigOptions.containsKey(args)) {
            if (command == RESET && "ALL" != args) {
              return FlinkSqlValidationResult(
                success = false,
                failedType = FlinkSqlValidationFailedType.VERIFY_FAILED,
                lineStart = call.lineStart,
                lineEnd = call.lineEnd,
                sql = sql.replaceFirst(";|$", ";"),
                exception = s"$args is not a valid table/sql config"
              )
            }
          }
        case
          SHOW_CATALOGS | SHOW_DATABASES | SHOW_CURRENT_CATALOG | SHOW_CURRENT_DATABASE |
          SHOW_TABLES | SHOW_VIEWS | SHOW_FUNCTIONS | SHOW_MODULES |
          CREATE_FUNCTION | CREATE_CATALOG | CREATE_TABLE | CREATE_VIEW | CREATE_DATABASE |
          DROP_CATALOG | DROP_DATABASE | DROP_TABLE | DROP_VIEW | DROP_FUNCTION |
          ALTER_DATABASE | ALTER_TABLE | ALTER_FUNCTION |
          USE | USE_CATALOG |
          SELECT | INSERT_INTO | INSERT_OVERWRITE |
          BEGIN_STATEMENT_SET | END_STATEMENT_SET |
          EXPLAIN | DESC | DESCRIBE =>
          Try {
            val calciteClass = Try(Class.forName(FLINK112_CALCITE_PARSER_CLASS)).getOrElse(Class.forName(FLINK113_CALCITE_PARSER_CLASS))
            val parser = calciteClass.getConstructor(Array(classOf[Config]): _*).newInstance(sqlParserConfig)
            val method = parser.getClass.getDeclaredMethod("parse", classOf[String])
            method.setAccessible(true)
            method.invoke(parser, call.originSql)
          } match {
            case Failure(e) =>
              val exception = ExceptionUtils.stringifyException(e)
              val causedBy = exception.drop(exception.indexOf("Caused by:"))
              val cleanUpError = exception.replaceAll("[\r\n]", "")
              if (SYNTAX_ERROR_REGEXP.findAllMatchIn(cleanUpError).nonEmpty) {
                val SYNTAX_ERROR_REGEXP(line, column) = cleanUpError
                return FlinkSqlValidationResult(
                  success = false,
                  failedType = FlinkSqlValidationFailedType.SYNTAX_ERROR,
                  lineStart = call.lineStart,
                  lineEnd = call.lineEnd,
                  errorLine = call.lineStart + line.toInt - 1,
                  errorColumn = column.toInt,
                  sql = call.originSql,
                  exception = causedBy
                )
              } else {
                return FlinkSqlValidationResult(
                  success = false,
                  failedType = FlinkSqlValidationFailedType.SYNTAX_ERROR,
                  lineStart = call.lineStart,
                  lineEnd = call.lineEnd,
                  sql = call.originSql,
                  exception = causedBy
                )
              }
            case _ =>
          }
        case _ =>
          return FlinkSqlValidationResult(
            success = false,
            failedType = FlinkSqlValidationFailedType.UNSUPPORTED_SQL,
            lineStart = call.lineStart,
            lineEnd = call.lineEnd,
            sql = sql.replaceFirst(";|$", ";"),
            exception = s"unsupported sql: ${call.originSql}"
          )
      }
    }
    FlinkSqlValidationResult()
  }

}
