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

import com.streamxhub.streamx.common.enums.SqlErrorType
import com.streamxhub.streamx.common.util.{ExceptionUtils, Logger}
import com.streamxhub.streamx.flink.core.SqlCommand._
import org.apache.calcite.config.Lex
import org.apache.calcite.sql.parser.SqlParser
import org.apache.flink.sql.parser.validate.FlinkSqlConformance
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.SqlDialect.{DEFAULT, HIVE}
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, TableException}
import org.apache.flink.table.planner.delegation.FlinkSqlParserFactories
import org.apache.flink.table.planner.parse.CalciteParser
import org.apache.flink.table.planner.utils.TableConfigUtils

object FlinkSqlValidator extends Logger {

  private[this] lazy val parser = {
    val tableConfig = StreamTableEnvironment.create(
      StreamExecutionEnvironment.getExecutionEnvironment,
      EnvironmentSettings
        .newInstance
        .useBlinkPlanner
        .inStreamingMode
        .build
    ).getConfig

    val sqlParserConfig = TableConfigUtils.getCalciteConfig(tableConfig).getSqlParserConfig.getOrElse {
      val conformance = tableConfig.getSqlDialect match {
        case HIVE => FlinkSqlConformance.HIVE
        case DEFAULT => FlinkSqlConformance.DEFAULT
        case _ =>
          throw new TableException(
            SqlError(
              SqlErrorType.UNSUPPORTED_DIALECT,
              s"Unsupported SQL dialect:${tableConfig.getSqlDialect}").toString
          )
      }
      SqlParser.config
        .withParserFactory(FlinkSqlParserFactories.create(conformance))
        .withConformance(conformance)
        .withLex(Lex.JAVA)
        .withIdentifierMaxLength(256)
    }
    new CalciteParser(sqlParserConfig)
  }


  def verifySql(sql: String): SqlError = {
    try {
      val sqlCommands = SqlCommandParser.parseSQL(sql)
      for (call <- sqlCommands) {
        lazy val args = call.operands.head
        lazy val command = call.command
        lazy val last = call.operands.last
        command match {
          case SET =>
            if (!FlinkSqlExecutor.tableConfigOptions.containsKey(args)) {
              return SqlError(
                SqlErrorType.SYNTAX_ERROR,
                exception = s"$args is not a valid table/sql config",
                sql = sql.replaceFirst(";|$", ";")
              )
            }
          case RESET =>
            if (args != "ALL" && !FlinkSqlExecutor.tableConfigOptions.containsKey(args)) {
              return SqlError(
                SqlErrorType.SYNTAX_ERROR,
                exception = s"$args is not a valid table/sql config",
                sql = sql.replaceFirst(";|$", ";")
              )
            }
          case
            SHOW_CATALOGS | SHOW_CURRENT_CATALOG | SHOW_DATABASES | SHOW_CURRENT_DATABASE |
            SHOW_TABLES | SHOW_VIEWS | SHOW_FUNCTIONS | SHOW_MODULES |
            CREATE_FUNCTION | CREATE_CATALOG | CREATE_TABLE | CREATE_VIEW | CREATE_DATABASE |
            DROP_CATALOG | DROP_DATABASE | DROP_TABLE | DROP_VIEW | DROP_FUNCTION |
            ALTER_DATABASE | ALTER_TABLE | ALTER_FUNCTION |
            USE | USE_CATALOG |
            SELECT | INSERT_INTO | INSERT_OVERWRITE |
            BEGIN_STATEMENT_SET | END_STATEMENT_SET |
            EXPLAIN | DESC | DESCRIBE =>
            try {
              command match {
                case CREATE_VIEW => parser.parse(last)
                case _ => parser.parse(args)
              }
            } catch {
              case e: Throwable =>
                logError(s"verify error:${ExceptionUtils.stringifyException(e)}")
                return SqlError(
                  SqlErrorType.SYNTAX_ERROR,
                  e.getLocalizedMessage,
                  args.trim.replaceFirst(";|$", ";")
                )
            }
          case _ => return SqlError(
            SqlErrorType.UNSUPPORTED_SQL,
            sql = sql.replaceFirst(";|$", ";")
          )
        }
      }
      null
    } catch {
      case exception: Exception =>
        logError(s"verify error:${ExceptionUtils.stringifyException(exception)}")
        SqlError.fromString(exception.getMessage)
    }
  }

}
