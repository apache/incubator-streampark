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
import enumeratum.EnumEntry

import java.util.Scanner
import java.util.regex.{Matcher, Pattern}
import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

object SqlCommandParser extends Logger {

  private[this] val WITH_REGEXP = "(WITH|with)\\s*\\(\\s*\\n+((.*)\\s*=(.*)(,|)\\s*\\n+)+\\)".r

  def parseSQL(sql: String): List[SqlCommandCall] = {
    val sqlEmptyError = SqlError(SQLErrorType.VERIFY_FAILED, "sql is empty", sql).toString
    require(sql != null && sql.trim.nonEmpty, sqlEmptyError)
    val lines = sql.split("\\n").filter(_ != null).filter(_.trim.nonEmpty).filter(!_.startsWith("--"))
    lines match {
      case x if x.isEmpty => throw new RuntimeException(sqlEmptyError)
      case x =>
        val calls = new ArrayBuffer[SqlCommandCall]
        val stmt = new StringBuilder
        for (line <- x) {
          stmt.append("\n").append(line)
          if (line.trim.endsWith(";")) {
            parseLine(stmt.toString.trim) match {
              case Some(x) => calls += x
              case _ => throw new RuntimeException(SqlError(SQLErrorType.UNSUPPORTED_SQL, sql = stmt.toString).toErrorString)
            }
            // clear string builder
            stmt.clear()
          }
        }
        calls.toList match {
          case Nil => throw new RuntimeException(SqlError(SQLErrorType.ENDS_WITH, sql = sql).toErrorString)
          case r => r
        }
    }
  }

  private[this] def parseLine(sqlLine: String): Option[SqlCommandCall] = {
    // remove ';' at the end
    val stmt = sqlLine.trim.replaceFirst(";$", "")
    // parse
    val sqlCommands = SqlCommand.values.filter(_.matches(stmt))
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
      sqlCommand.converter(groups).map(operands => SqlCommandCall(sqlCommand, operands))
    }
  }

}


sealed abstract class SqlCommand(
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

object SqlCommand extends enumeratum.Enum[SqlCommand] {

  val values: immutable.IndexedSeq[SqlCommand] = findValues

  private[this] val NO_OPERANDS = (_: Array[String]) => Some(Array.empty[String])

  case object SHOW_CATALOGS extends SqlCommand(
    "show catalogs",
    "SHOW\\s+CATALOGS",
    NO_OPERANDS
  )

  case object SHOW_DATABASES extends SqlCommand(
    "show databases",
    "SHOW\\s+DATABASES",
    NO_OPERANDS
  )

  case object SHOW_TABLES extends SqlCommand(
    "show tables",
    "SHOW\\s+TABLES",
    NO_OPERANDS
  )

  case object SHOW_FUNCTIONS extends SqlCommand(
    "show functions",
    "SHOW\\s+FUNCTIONS",
    NO_OPERANDS
  )

  case object SHOW_MODULES extends SqlCommand(
    "show modules",
    "SHOW\\s+MODULES",
    NO_OPERANDS
  )

  case object USE_CATALOG extends SqlCommand(
    "use catalog",
    "USE\\s+CATALOG\\s+(.*)"
  )

  case object USE extends SqlCommand(
    "use",
    "USE\\s+(?!CATALOG)(.*)"
  )

  case object CREATE_CATALOG extends SqlCommand(
    "create catalog",
    "(CREATE\\s+CATALOG\\s+.*)"
  )

  case object DROP_CATALOG extends SqlCommand(
    "drop catalog",
    "(DROP\\s+CATALOG\\s+.*)"
  )

  case object DESC extends SqlCommand(
    "desc",
    "DESC\\s+(.*)"
  )

  case object DESCRIBE extends SqlCommand(
    "describe",
    "DESCRIBE\\s+(.*)"
  )

  case object EXPLAIN extends SqlCommand(
    "explain",
    "EXPLAIN\\s+(SELECT|INSERT)\\s+(.*)",
    (x: Array[String]) => Some(Array[String](x(1), x(2)))
  )

  case object CREATE_DATABASE extends SqlCommand(
    "create database",
    "(CREATE\\s+DATABASE\\s+.*)"
  )

  case object DROP_DATABASE extends SqlCommand(
    "drop database",
    "(DROP\\s+DATABASE\\s+.*)"
  )

  case object ALTER_DATABASE extends SqlCommand(
    "alter database",
    "(ALTER\\s+DATABASE\\s+.*)"
  )

  case object CREATE_TABLE extends SqlCommand(
    "create table",
    "(CREATE\\s+TABLE\\s+.*)"
  )

  case object DROP_TABLE extends SqlCommand(
    "drop table",
    "(DROP\\s+TABLE\\s+.*)"
  )

  case object ALTER_TABLE extends SqlCommand(
    "alter table",
    "(ALTER\\s+TABLE\\s+.*)"
  )

  case object DROP_VIEW extends SqlCommand(
    "drop view",
    "DROP\\s+VIEW\\s+(.*)"
  )

  case object CREATE_VIEW extends SqlCommand(
    "create view",
    "(CREATE\\s+VIEW.*)", {
      case a if a.length < 2 => None
      case x => Some(Array[String](x(1), x(2)))
    }
  )

  case object CREATE_FUNCTION extends SqlCommand(
    "create function",
    "(CREATE\\s+FUNCTION\\s+.*)"
  )

  case object DROP_FUNCTION extends SqlCommand(
    "drop function",
    "DROP\\s+FUNCTION\\s+(.*)"
  )

  case object ALTER_FUNCTION extends SqlCommand(
    "alter function",
    "(ALTER\\s+FUNCTION.*)"
  )

  case object SELECT extends SqlCommand(
    "select",
    "(SELECT.*)"
  )

  case object INSERT_INTO extends SqlCommand(
    "insert into",
    "(INSERT\\s+INTO.*)"
  )

  case object INSERT_OVERWRITE extends SqlCommand(
    "insert overwrite",
    "(INSERT\\s+OVERWRITE.*)"
  )

  case object SET extends SqlCommand(
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
case class SqlCommandCall(command: SqlCommand, operands: Array[String]) {
  def this(command: SqlCommand) {
    this(command, new Array[String](0))
  }
}


case class SqlError(
                     errorType: SQLErrorType,
                     exception: String = null,
                     sql: String = null
                   ) {
  //不可见分隔符.
  private[core] val separator = "\001"

  private[core] def toErrorString: String = s"${errorType.errorType}$separator$exception$separator$sql"
}

