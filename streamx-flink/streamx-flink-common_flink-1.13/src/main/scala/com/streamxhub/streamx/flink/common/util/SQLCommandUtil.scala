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
package com.streamxhub.streamx.flink.common.util

import com.streamxhub.streamx.common.enums.SQLErrorType
import com.streamxhub.streamx.common.util.Logger
import enumeratum.EnumEntry
import org.apache.calcite.config.Lex
import org.apache.calcite.sql.parser.SqlParser
import org.apache.flink.sql.parser.validate.FlinkSqlConformance
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.SqlDialect.{DEFAULT, HIVE}
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, TableException}
import org.apache.flink.table.planner.parse.CalciteParser
import org.apache.flink.table.planner.delegation.FlinkSqlParserFactories
import org.apache.flink.table.planner.utils.TableConfigUtils

import java.util.Scanner
import java.util.regex.{Matcher, Pattern}
import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

object SQLCommandUtil extends Logger {

  private[this] val WITH_REGEXP = "(WITH|with)\\s*\\(\\s*\\n+((.*)\\s*=(.*)(,|)\\s*\\n+)+\\)".r

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
            SQLError(
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

  def verifySQL(sql: String): SQLError = {
    var sqlCommands: List[SQLCommandCall] = List.empty[SQLCommandCall]
    try {
      sqlCommands = parseSQL(sql)
    } catch {
      case exception: Exception =>
        val separator = "\001"
        val error = exception.getLocalizedMessage
        val array = error.split(separator)
        return SQLError(
          SQLErrorType.of(array.head.toInt),
          if (array(1) == "null") null else array(1),
          array.last
        )
    }

    for (call <- sqlCommands) {
      val sql = call.operands.head
      import com.streamxhub.streamx.flink.common.util.SQLCommand._
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
              return SQLError(
                SQLErrorType.SYNTAX_ERROR,
                e.getLocalizedMessage,
                sql.trim.replaceFirst(";|$", ";")
              )
            case _: Throwable =>
          }
        case _ => return SQLError(
          SQLErrorType.UNSUPPORTED_SQL,
          sql = sql.replaceFirst(";|$", ";")
        )
      }
    }
    null
  }

  def parseSQL(sql: String): List[SQLCommandCall] = {
    val sqlEmptyError = SQLError(SQLErrorType.VERIFY_FAILED, "sql is empty", sql).toString
    require(sql != null && sql.trim.nonEmpty, sqlEmptyError)
    val lines = sql.split("\\n").filter(_!=null).filter(_.trim.nonEmpty).filter(!_.startsWith("--"))
    lines match {
      case x if x.isEmpty => throw new RuntimeException(sqlEmptyError)
      case x =>
        val calls = new ArrayBuffer[SQLCommandCall]
        val stmt = new StringBuilder
        for (line <- x) {
          stmt.append("\n").append(line)
          if (line.trim.endsWith(";")) {
            parseLine(stmt.toString.trim) match {
              case Some(x) => calls += x
              case _ => throw new RuntimeException(SQLError(SQLErrorType.UNSUPPORTED_SQL, sql = stmt.toString).toErrorString)
            }
            // clear string builder
            stmt.clear()
          }
        }
        calls.toList match {
          case Nil => throw new RuntimeException(SQLError(SQLErrorType.ENDS_WITH, sql = sql).toErrorString)
          case r => r
        }
    }
  }

  private[this] def parseLine(sqlLine: String): Option[SQLCommandCall] = {
    // remove ';' at the end
    val stmt = sqlLine.trim.replaceFirst(";$", "")
    // parse
    val sqlCommands = SQLCommand.values.filter(_.matches(stmt))
    if (sqlCommands.isEmpty) None else {
      val sqlCommand = sqlCommands.head
      val matcher = sqlCommand.matcher
      val groups = new Array[String](matcher.groupCount)
      for (i <- groups.indices) {
        groups(i) = {
          val segment = matcher.group(i + 1)
          val withMatcher = WITH_REGEXP.pattern.matcher(segment)
          if (!withMatcher.find()) segment else {
            /**
             * 解决with里的属性参数必须加单引号'的问题,从此可以不用带'了,更可读(手指多动一下是可耻的,scala语言之父说的.)
             */
            val withSegment = withMatcher.group()
            val scanner = new Scanner(withSegment)
            val buffer = new StringBuffer()
            while (scanner.hasNextLine) {
              val line = scanner.nextLine().replaceAll("--(.*)$", "").trim
              val propReg = "\\s*(.*)\\s*=(.*)(,|)\\s*"
              if (line.matches(propReg)) {
                var newLine = line
                  .replaceAll("^'|^", "'")
                  .replaceAll("('|)\\s*=\\s*('|)", "' = '")
                  .replaceAll("('|),$", "',")
                if (!line.endsWith(",")) {
                  newLine = newLine.replaceFirst("('|)\\s*$", "'")
                }
                buffer.append(newLine).append("\n")
              } else {
                buffer.append(line).append("\n")
              }
            }
            segment.replace(withSegment, buffer.toString.trim)
          }
        }
      }
      sqlCommand.converter(groups).map(operands => SQLCommandCall(sqlCommand, operands))
    }
  }

}

case class SQLError(
                     errorType: SQLErrorType,
                     exception: String = null,
                     sql: String = null
                   ) {
  //不可见分隔符.
  private[util] val separator = "\001"

  private[util] def toErrorString: String = s"${errorType.errorType}$separator$exception$separator$sql"
}

sealed abstract class SQLCommand(
                                  val name: String,
                                  private val regex: String,
                                  val converter: Array[String] => Option[Array[String]] = (x: Array[String]) => Some(Array[String](x.head))
                                ) extends EnumEntry {
  var matcher: Matcher = _

  def matches(input: String): Boolean = {
    if (regex == null) false else {
      val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
      matcher = pattern.matcher(input)
      matcher.matches()
    }
  }
}

object SQLCommand extends enumeratum.Enum[SQLCommand] {

  val values: immutable.IndexedSeq[SQLCommand] = findValues

  private[this] val NO_OPERANDS = (_: Array[String]) => Some(Array.empty[String])

  case object SHOW_CATALOGS extends SQLCommand(
    "show catalogs",
    "SHOW\\s+CATALOGS",
    NO_OPERANDS
  )

  case object SHOW_DATABASES extends SQLCommand(
    "show databases",
    "SHOW\\s+DATABASES",
    NO_OPERANDS
  )

  case object SHOW_TABLES extends SQLCommand(
    "show tables",
    "SHOW\\s+TABLES",
    NO_OPERANDS
  )

  case object SHOW_FUNCTIONS extends SQLCommand(
    "show functions",
    "SHOW\\s+FUNCTIONS",
    NO_OPERANDS
  )

  case object SHOW_MODULES extends SQLCommand(
    "show modules",
    "SHOW\\s+MODULES",
    NO_OPERANDS
  )

  case object USE_CATALOG extends SQLCommand(
    "use catalog",
    "USE\\s+CATALOG\\s+(.*)"
  )

  case object USE extends SQLCommand(
    "use",
    "USE\\s+(?!CATALOG)(.*)"
  )

  case object CREATE_CATALOG extends SQLCommand(
    "create catalog",
    "(CREATE\\s+CATALOG\\s+.*)"
  )

  case object DROP_CATALOG extends SQLCommand(
    "drop catalog",
    "(DROP\\s+CATALOG\\s+.*)"
  )

  case object DESC extends SQLCommand(
    "desc",
    "DESC\\s+(.*)"
  )

  case object DESCRIBE extends SQLCommand(
    "describe",
    "DESCRIBE\\s+(.*)"
  )

  case object EXPLAIN extends SQLCommand(
    "explain",
    "EXPLAIN\\s+(SELECT|INSERT)\\s+(.*)",
    (x: Array[String]) => Some(Array[String](x(1), x(2)))
  )

  case object CREATE_DATABASE extends SQLCommand(
    "create database",
    "(CREATE\\s+DATABASE\\s+.*)"
  )

  case object DROP_DATABASE extends SQLCommand(
    "drop database",
    "(DROP\\s+DATABASE\\s+.*)"
  )

  case object ALTER_DATABASE extends SQLCommand(
    "alter database",
    "(ALTER\\s+DATABASE\\s+.*)"
  )

  case object CREATE_TABLE extends SQLCommand(
    "create table",
    "(CREATE\\s+TABLE\\s+.*)"
  )

  case object DROP_TABLE extends SQLCommand(
    "drop table",
    "(DROP\\s+TABLE\\s+.*)"
  )

  case object ALTER_TABLE extends SQLCommand(
    "alter table",
    "(ALTER\\s+TABLE\\s+.*)"
  )

  case object DROP_VIEW extends SQLCommand(
    "drop view",
    "DROP\\s+VIEW\\s+(.*)"
  )

  case object CREATE_VIEW extends SQLCommand(
    "create view",
    "(CREATE\\s+VIEW.*)", {
      case a if a.length < 2 => None
      case x => Some(Array[String](x(1), x(2)))
    }
  )

  case object CREATE_FUNCTION extends SQLCommand(
    "create function",
    "(CREATE\\s+FUNCTION\\s+.*)"
  )

  case object DROP_FUNCTION extends SQLCommand(
    "drop function",
    "DROP\\s+FUNCTION\\s+(.*)"
  )

  case object ALTER_FUNCTION extends SQLCommand(
    "alter function",
    "(ALTER\\s+FUNCTION.*)"
  )

  case object SELECT extends SQLCommand(
    "select",
    "(SELECT.*)"
  )

  case object INSERT_INTO extends SQLCommand(
    "insert into",
    "(INSERT\\s+INTO.*)"
  )

  case object INSERT_OVERWRITE extends SQLCommand(
    "insert overwrite",
    "(INSERT\\s+OVERWRITE.*)"
  )

  case object SET extends SQLCommand(
    "set",
    "(\\s+(\\S+)\\s*=(.*))?", {
      case a if a.length < 3 => None
      case a if a(0) == null => Some(new Array[String](0))
      case x => Some(Array[String](x(1), x(2)))
    }
  )

}

/**
 * Call of SQL command with operands and command type.
 */
case class SQLCommandCall(command: SQLCommand, operands: Array[String]) {
  def this(command: SQLCommand) {
    this(command, new Array[String](0))
  }
}
