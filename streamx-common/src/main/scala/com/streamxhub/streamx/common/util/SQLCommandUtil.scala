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
package com.streamxhub.streamx.common.util

import enumeratum.EnumEntry

import java.util.regex.{Matcher, Pattern}
import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

object SQLCommandUtil {

  def parseSQL(sql: String): List[SQLCommandCall] = {
    require(sql != null && !sql.trim.isEmpty, s"$sql must be not empty")
    if (!sql.endsWith(";")) {
      throw new RuntimeException(s"Unsupported command, must be endsWith ';', $sql")
    } else {
      val lines = sql.split("\\n").filter(_.trim.nonEmpty).filter(!_.startsWith("--"))
      lines match {
        case x if x.isEmpty =>
          throw new RuntimeException(s"Unsupported command,must be not empty,$sql")
        case x =>
          val calls = new ArrayBuffer[SQLCommandCall]
          val stmt = new StringBuilder
          for (line <- x) {
            stmt.append("\n").append(line)
            if (line.trim.endsWith(";")) {
              parseLine(stmt.toString.trim) match {
                case Some(x) => calls += x
                case _ => throw new RuntimeException(s"Unsupported command '${stmt.toString()}'")
              }
              // clear string builder
              stmt.clear()
            }
          }
          calls.toList
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
        groups(i) = matcher.group(i + 1)
      }
      sqlCommand.converter(groups).map(operands => SQLCommandCall(sqlCommand, operands))
    }
  }

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
    })

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
    })

  case object RESET extends SQLCommand(
    "reset",
    "RESET",
    NO_OPERANDS
  )

  case object SOURCE extends SQLCommand(
    "source",
    "SOURCE\\s+(.*)"
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
