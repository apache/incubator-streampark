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

import org.apache.streampark.common.enums.FlinkSqlValidationFailedType
import org.apache.streampark.common.util.{ExceptionUtils, Logger}
import org.apache.streampark.flink.core.SqlCommand._
import org.apache.calcite.config.Lex
import org.apache.calcite.sql.parser.SqlParser
import org.apache.calcite.sql.parser.SqlParser.Config
import org.apache.flink.api.common.RuntimeExecutionMode
import org.apache.flink.configuration.ExecutionOptions
import org.apache.flink.sql.parser.validate.FlinkSqlConformance
import org.apache.flink.table.api.SqlDialect.{DEFAULT, HIVE}
import org.apache.flink.table.api.{SqlDialect, TableConfig}
import org.apache.flink.table.api.config.TableConfigOptions
import org.apache.flink.table.planner.delegation.FlinkSqlParserFactories

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Try}

object FlinkSqlValidator extends Logger {

  private[this] val FLINK112_CALCITE_PARSER_CLASS = "org.apache.flink.table.planner.calcite.CalciteParser"

  private[this] val FLINK113_CALCITE_PARSER_CLASS = "org.apache.flink.table.planner.parse.CalciteParser"

  private[this] val SYNTAX_ERROR_REGEXP = ".*at\\sline\\s(\\d+),\\scolumn\\s(\\d+).*".r

  private[this] lazy val sqlParserConfigMap: Map[String, SqlParser.Config] = {
    def getConfig(sqlDialect: SqlDialect): Config = {
      val tableConfig = new TableConfig()
      tableConfig.getConfiguration.set(ExecutionOptions.RUNTIME_MODE, RuntimeExecutionMode.STREAMING)
      tableConfig.getConfiguration.set(TableConfigOptions.TABLE_SQL_DIALECT, sqlDialect.name().toLowerCase())
      val conformance = sqlDialect match {
        case HIVE => FlinkSqlConformance.HIVE
        case DEFAULT => FlinkSqlConformance.DEFAULT
        case _ => throw new UnsupportedOperationException(s"Unsupported sqlDialect: $sqlDialect")
      }
      SqlParser.config
        .withParserFactory(FlinkSqlParserFactories.create(conformance))
        .withConformance(conformance)
        .withLex(Lex.JAVA)
        .withIdentifierMaxLength(256)
    }

    Map(
      SqlDialect.DEFAULT.name() -> getConfig(SqlDialect.DEFAULT),
      SqlDialect.HIVE.name() -> getConfig(SqlDialect.HIVE)
    )
  }

  def verifySql(sql: String): FlinkSqlValidationResult = {
    val sqlCommands = SqlCommandParser
      .parseSQL(sql, r => return r)
      .filter(x => x.command != BEGIN_STATEMENT_SET && x.command != END_STATEMENT_SET)

    var result: FlinkSqlValidationResult = null
    val arrayBuffer = new ArrayBuffer[SqlCommandCall]()
    var dialect: String = "DEFAULT"

    sqlCommands.foreach(call => {
      if (call.command == SET) {
        val setKey = call.operands.head
        val setValue = call.operands.last
        if (!FlinkSqlExecutor.tableConfigOptions.containsKey(setKey)) {
          return FlinkSqlValidationResult(
            success = false,
            failedType = FlinkSqlValidationFailedType.VERIFY_FAILED,
            lineStart = call.lineStart,
            lineEnd = call.lineEnd,
            sql = sql.replaceFirst(";|$", ";"),
            exception = s"$setKey is not a valid table/sql config"
          )
        }
        if (setKey == TableConfigOptions.TABLE_SQL_DIALECT.key()) {
          if (arrayBuffer.nonEmpty) {
            result = doVerify(arrayBuffer.toList, dialect)
            arrayBuffer.clear()
            arrayBuffer += call
          }
          dialect = setValue
        } else {
          arrayBuffer += call
        }
      } else {
        arrayBuffer += call
      }
    })

    if (result == null || (result.success && arrayBuffer.nonEmpty)) {
      result = doVerify(arrayBuffer.toList, dialect)
    }

    if (sqlCommands.find(_.command == INSERT).nonEmpty) {
      result
    } else {
      FlinkSqlValidationResult(
        success = false,
        failedType = FlinkSqlValidationFailedType.SYNTAX_ERROR,
        lineStart = sqlCommands.head.lineStart,
        lineEnd = sqlCommands.last.lineEnd,
        exception = "No 'INSERT' statement to trigger the execution of the Flink job."
      )
    }
  }

  private[this] def doVerify(sqlCommands: List[SqlCommandCall], sqlDialect: String): FlinkSqlValidationResult = {
    val lineStart = sqlCommands.head.lineStart
    val lineEnd = sqlCommands.last.lineEnd
    val originSql = sqlCommands.map(_.originSql).mkString("", ";\r\n", ";")
    Try {
      val calciteClass = Try(Class.forName(FLINK112_CALCITE_PARSER_CLASS)).getOrElse(Class.forName(FLINK113_CALCITE_PARSER_CLASS))
      sqlDialect.toUpperCase() match {
        case "HIVE" | "DEFAULT" =>
        case _ =>
          throw new UnsupportedOperationException(s"unsupported dialect: ${sqlDialect}")
      }
      val parser = calciteClass.getConstructor(Array(classOf[Config]): _*).newInstance(sqlParserConfigMap(sqlDialect.toUpperCase()))
      val method = parser.getClass.getDeclaredMethod("parse", classOf[String])
      method.setAccessible(true)
      method.invoke(parser, originSql)
    } match {
      case Failure(e) =>
        val exception = ExceptionUtils.stringifyException(e)
        val causedBy = exception.drop(exception.indexOf("Caused by:"))
        val cleanUpError = exception.replaceAll("[\r\n]", "")
        if (SYNTAX_ERROR_REGEXP.findAllMatchIn(cleanUpError).nonEmpty) {
          val SYNTAX_ERROR_REGEXP(line, column) = cleanUpError
          val errorLine = lineStart + line.toInt - 1
          return FlinkSqlValidationResult(
            success = false,
            failedType = FlinkSqlValidationFailedType.SYNTAX_ERROR,
            lineStart = lineStart,
            lineEnd = lineEnd,
            errorLine = errorLine,
            errorColumn = column.toInt,
            sql = originSql,
            exception = causedBy.replaceAll(s"at\\sline\\s$line", s"at line $errorLine")
          )
        } else {
          return FlinkSqlValidationResult(
            success = false,
            failedType = FlinkSqlValidationFailedType.SYNTAX_ERROR,
            lineStart = lineStart,
            lineEnd = lineEnd,
            sql = originSql,
            exception = causedBy
          )
        }
      case _ =>
    }
    FlinkSqlValidationResult()
  }


}
