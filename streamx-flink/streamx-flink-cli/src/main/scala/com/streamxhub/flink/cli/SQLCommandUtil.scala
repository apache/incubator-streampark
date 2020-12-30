/**
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
package com.streamxhub.flink.cli

import java.util.List
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import enumeratum.EnumEntry

import java.util.regex.{Matcher, Pattern}

object SQLCommandUtil {

  def parseSQL(lines: List[String]): List[SQLCommandCall] = {
    val calls = new ArrayBuffer[SQLCommandCall]
    val stmt = new StringBuilder
    for (line <- lines if line.trim.nonEmpty && !line.startsWith("--")) {
      stmt.append("\n").append(line)
      if (line.trim.endsWith(";")) {
        parseSQL(stmt.toString.trim) match {
          case Some(x) => calls += x
          case _ => throw new RuntimeException("Unsupported command '" + stmt.toString + "'")
        }
        // clear string builder
        stmt.clear()
      }
    }
    calls
  }

  def parseSQL(sqlLine: String): Option[SQLCommandCall] = {
    // remove ';' at the end
    val stmt = sqlLine.trim.replaceFirst(";$", "")
    // parse
    val sqlCommands = SQLCommand.values.filter(_.matches(stmt))
    if (sqlCommands.isEmpty) None else {
      val sqlCommand = sqlCommands.head
      val matcher = sqlCommand.matcher
      val groups = new Array[String](matcher.groupCount)
      for (i <- 0 until groups.length) {
        groups(i) = matcher.group(i + 1)
      }
      sqlCommand.converter(groups).map(operands => SQLCommandCall(sqlCommand, operands))
    }
  }

}


sealed abstract class SQLCommand(private val regex: String, val converter: Array[String] => Option[Array[String]]) extends EnumEntry {
  var matcher: Matcher = null

  def matches(input: String): Boolean = {
    val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
    matcher = pattern.matcher(input)
    matcher.matches()
  }
}

object SQLCommand extends enumeratum.Enum[SQLCommand] {

  val values = findValues

  case object SET extends SQLCommand(
    "(\\s+(\\S+)\\s*=(.*))?",
    x =>
      x match {
        case a if a.length < 3 => None
        case a if a(0) == null => Some(new Array[String](0))
        case _ => Some(Array[String](x(1), x(2)))
      }
  )

  case object INSERT_INTO extends SQLCommand(
    "(INSERT\\s+INTO.*)",
    (x: Array[String]) => Some(Array[String](x(0)))
  )

  case object CREATE_TABLE extends SQLCommand(
    "(CREATE\\s+TABLE.*)",
    (x: Array[String]) => Some(Array[String](x(0)))
  )

  case object CREATE_VIEW extends SQLCommand(
    "(CREATE\\s+VIEW.*)",
    (x: Array[String]) => Some(Array[String](x(0)))
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
