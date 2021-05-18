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
package com.streamxhub.streamx.flink.core

import com.streamxhub.streamx.common.enums.SQLErrorType
import com.streamxhub.streamx.common.util.Logger
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

object SqlValidator extends Logger {

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
              SQLErrorType.UNSUPPORTED_DIALECT,
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

  def verifySQL(sql: String): SqlError = {
    var sqlCommands: List[SqlCommandCall] = List.empty[SqlCommandCall]
    try {
      sqlCommands = SqlCommandParser.parseSQL(sql)
    } catch {
      case exception: Exception =>
        val separator = "\001"
        val error = exception.getLocalizedMessage
        val array = error.split(separator)
        return SqlError(
          SQLErrorType.of(array.head.toInt),
          if (array(1) == "null") null else array(1),
          array.last
        )
    }

    for (call <- sqlCommands) {
      val sql = call.operands.head
      import com.streamxhub.streamx.flink.core.SqlCommand._
      call.command match {
        case USE | USE_CATALOG | SET |
             SELECT | INSERT_INTO | INSERT_OVERWRITE |
             EXPLAIN | DESC | DESCRIBE |
             SHOW_MODULES | SHOW_FUNCTIONS | SHOW_TABLES | SHOW_DATABASES | SHOW_CATALOGS |
             CREATE_FUNCTION | DROP_FUNCTION | ALTER_FUNCTION |
             CREATE_CATALOG | DROP_CATALOG |
             CREATE_TABLE | DROP_TABLE | ALTER_TABLE |
             CREATE_VIEW | DROP_VIEW |
             CREATE_DATABASE | DROP_DATABASE | ALTER_DATABASE =>
          try {
            parser.parse(sql)
          } catch {
            case e: Exception =>
              return SqlError(
                SQLErrorType.SYNTAX_ERROR,
                e.getLocalizedMessage,
                sql.trim.replaceFirst(";|$", ";")
              )
            case _: Throwable =>
          }
        case _ => return SqlError(
          SQLErrorType.UNSUPPORTED_SQL,
          sql = sql.replaceFirst(";|$", ";")
        )
      }
    }
    null
  }

}
