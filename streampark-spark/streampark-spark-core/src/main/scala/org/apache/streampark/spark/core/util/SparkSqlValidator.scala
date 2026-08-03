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

package org.apache.streampark.spark.core.util

import org.apache.streampark.common.enums.SparkSqlValidationFailedType
import org.apache.streampark.common.util.{ExceptionUtils, Logger}

import scala.util.{Failure, Try}

object SparkSqlValidator extends Logger {

  private[this] val SPARK_SQL_PARSER_CLASS =
    "org.apache.spark.sql.execution.SparkSqlParser"

  private[this] val SYNTAX_ERROR_REGEXP =
    ".*\\(line\\s(\\d+),\\spos\\s(\\d+)\\).*".r

  def verifySql(sql: String): SparkSqlValidationResult = {
    val sqlCommands = SqlCommandParser.parseSQL(sql, r => return r)
    Try {
      val parserClass = Try(Class.forName(SPARK_SQL_PARSER_CLASS)).get
      val parser = parserClass
        .getConstructor()
        .newInstance()
      val method =
        parser.getClass.getMethod("parsePlan", classOf[String])
      method.setAccessible(true)
      for (call <- sqlCommands) {
        Try {
          method.invoke(parser, call.originSql)
        } match {
          case Failure(e) =>
            val exception = ExceptionUtils.stringifyException(e)
            val causedBy = exception.drop(exception.indexOf("Caused by:"))
            val cleanUpError = exception.replaceAll("[\r\n]", "")
            if (SYNTAX_ERROR_REGEXP.findAllMatchIn(cleanUpError).nonEmpty) {
              val SYNTAX_ERROR_REGEXP(line, column) = cleanUpError
              val errorLine = call.lineStart + line.toInt - 1
              return SparkSqlValidationResult(
                success = false,
                failedType = SparkSqlValidationFailedType.SYNTAX_ERROR,
                lineStart = call.lineStart,
                lineEnd = call.lineEnd,
                errorLine = errorLine,
                errorColumn = column.toInt,
                sql = call.originSql,
                exception = causedBy.replaceAll(s"at\\sline\\s$line", s"at line $errorLine"))
            } else {
              return SparkSqlValidationResult(
                success = false,
                failedType = SparkSqlValidationFailedType.SYNTAX_ERROR,
                lineStart = call.lineStart,
                lineEnd = call.lineEnd,
                sql = call.originSql,
                exception = causedBy)
            }
          case _ =>
        }
      }
    } match {
      case Failure(e) =>
        return SparkSqlValidationResult(
          success = false,
          failedType = SparkSqlValidationFailedType.CLASS_ERROR,
          exception = ExceptionUtils.stringifyException(e))
      case _ =>
    }
    SparkSqlValidationResult()
  }

}
