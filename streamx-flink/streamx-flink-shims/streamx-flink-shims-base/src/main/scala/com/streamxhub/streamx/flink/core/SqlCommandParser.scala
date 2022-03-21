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
import com.streamxhub.streamx.common.util.{Logger, SqlSplitter}
import enumeratum.EnumEntry

import java.util.regex.{Matcher, Pattern}
import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

object SqlCommandParser extends Logger {

  def parseSQL(sql: String): List[SqlCommandCall] = {
    val sqlEmptyError = SqlError(SqlErrorType.VERIFY_FAILED, "sql is empty", sql).toString
    require(sql != null && sql.trim.nonEmpty, sqlEmptyError)
    val lines = SqlSplitter.splitSql(sql)
    lines match {
      case stmts if stmts.isEmpty => throw new IllegalArgumentException(sqlEmptyError)
      case stmts =>
        val calls = new ArrayBuffer[SqlCommandCall]
        for (stmt <- stmts) {
          parseLine(stmt) match {
            case Some(x) => calls += x
            case _ => throw new IllegalArgumentException(SqlError(SqlErrorType.UNSUPPORTED_SQL, exception = s"unsupported sql", sql = stmt).toString)
          }
        }
        calls.toList match {
          case Nil => throw new IllegalArgumentException(SqlError(SqlErrorType.SYNTAX_ERROR, exception = "no executable sql", sql = "").toString)
          case r => r
        }
    }
  }

  private[this] def parseLine(sqlLine: String): Option[SqlCommandCall] = {
    val stmt = sqlLine.trim
    // parse
    val sqlCommands = SqlCommand.values.filter(_.matches(stmt))
    if (sqlCommands.isEmpty) None else {
      val sqlCommand = sqlCommands.head
      val matcher = sqlCommand.matcher
      val groups = new Array[String](matcher.groupCount)
      for (i <- groups.indices) {
        groups(i) = matcher.group(i + 1)
      }
      sqlCommand.converter(groups).map(x => SqlCommandCall(sqlCommand, x, sqlLine))
    }
  }

}

object Converters {
  val NO_OPERANDS = (_: Array[String]) => Some(Array.empty[String])
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

  //----CREATE Statements----

  /**
   * <pre>
   * CREATE CATALOG catalog_name
   * WITH (key1=val1, key2=val2, ...)
   * </pre>
   */
  case object CREATE_CATALOG extends SqlCommand(
    "create catalog",
    "(CREATE\\s+CATALOG\\s+.*)"
  )

  /**
   * <pre>
   * CREATE DATABASE [IF NOT EXISTS] [catalog_name.]db_name<br>
   * [COMMENT database_comment]<br>
   * WITH (key1=val1, key2=val2, ...)<br>
   * </pre>
   */
  case object CREATE_DATABASE extends SqlCommand(
    "create database",
    "(CREATE\\s+DATABASE\\s+.*)"
  )

  /**
   * <pre>
   * CREATE TABLE [IF NOT EXISTS] [catalog_name.][db_name.]table_name
   * (
   * { <physical_column_definition> | <metadata_column_definition> | <computed_column_definition> }[ , ...n]
   * [ <watermark_definition> ]
   * [ <table_constraint> ][ , ...n]
   * )
   * [COMMENT table_comment]
   * [PARTITIONED BY (partition_column_name1, partition_column_name2, ...)]
   * WITH (key1=val1, key2=val2, ...)
   * [ LIKE source_table [( <like_options> )] ]
   * </pre
   */
  case object CREATE_TABLE extends SqlCommand(
    "create table",
    "(CREATE\\s+(TEMPORARY\\s+|)TABLE\\s+.*)"
  )

  /**
   * <pre>
   * CREATE [TEMPORARY] VIEW [IF NOT EXISTS] [catalog_name.][db_name.]view_name
   * [( columnName [, columnName ]* )] [COMMENT view_comment]
   * AS query_expression<
   * </pre
   */
  case object CREATE_VIEW extends SqlCommand(
    "create view",
    "CREATE\\s+(TEMPORARY\\s+|)VIEW\\s+(\\S+)\\s+AS\\s+(.*)", {
      case a if a.length < 2 => None
      case x => Some(Array[String](x.head, x.last))
    }
  )

  /**
   * <pre>
   * CREATE [TEMPORARY|TEMPORARY SYSTEM] FUNCTION
   * [IF NOT EXISTS] [catalog_name.][db_name.]function_name
   * AS identifier [LANGUAGE JAVA|SCALA|PYTHON]
   * </pre
   */
  case object CREATE_FUNCTION extends SqlCommand(
    "create function",
    "(CREATE\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+.*)"
  )

  //----ALTER Statements ---
  case object ALTER_DATABASE extends SqlCommand(
    "alter database",
    "(ALTER\\s+DATABASE\\s+.*)"
  )

  case object ALTER_TABLE extends SqlCommand(
    "alter table",
    "(ALTER\\s+TABLE\\s+.*)"
  )

  case object ALTER_FUNCTION extends SqlCommand(
    "alter function",
    "(ALTER\\s+FUNCTION.*)"
  )

  //----DROP Statements----

  case object DROP_CATALOG extends SqlCommand(
    "drop catalog",
    "(DROP\\s+CATALOG\\s+.*)"
  )

  case object DROP_DATABASE extends SqlCommand(
    "drop database",
    "(DROP\\s+DATABASE\\s+.*)"
  )

  case object DROP_TABLE extends SqlCommand(
    "drop table",
    "(DROP\\s+TABLE\\s+.*)"
  )

  case object DROP_VIEW extends SqlCommand(
    "drop view",
    "DROP\\s+VIEW\\s+(.*)"
  )


  case object DROP_FUNCTION extends SqlCommand(
    "drop function",
    "DROP\\s+FUNCTION\\s+(.*)"
  )

  //----SHOW Statements ---
  case object SHOW_CATALOGS extends SqlCommand(
    "show catalogs",
    "SHOW\\s+CATALOGS",
    Converters.NO_OPERANDS
  )

  case object SHOW_CURRENT_CATALOG extends SqlCommand(
    "show current catalogs",
    "SHOW\\s+CURRENT\\s+CATALOG",
    Converters.NO_OPERANDS
  )

  case object SHOW_DATABASES extends SqlCommand(
    "show databases",
    "SHOW\\s+DATABASES",
    Converters.NO_OPERANDS
  )

  case object SHOW_CURRENT_DATABASE extends SqlCommand(
    "show current database",
    "SHOW\\s+CURRENT\\s+DATABASE",
    Converters.NO_OPERANDS
  )

  case object SHOW_TABLES extends SqlCommand(
    "show tables",
    "SHOW\\s+TABLES",
    Converters.NO_OPERANDS
  )

  case object SHOW_VIEWS extends SqlCommand(
    "show views",
    "SHOW\\s+VIEWS",
    Converters.NO_OPERANDS
  )

  case object SHOW_FUNCTIONS extends SqlCommand(
    "show functions",
    "SHOW\\s+FUNCTIONS",
    Converters.NO_OPERANDS
  )

  case object SHOW_MODULES extends SqlCommand(
    "show modules",
    "SHOW\\s+MODULES",
    Converters.NO_OPERANDS
  )


  //---- INSERT Statement #
  case object INSERT_INTO extends SqlCommand(
    "insert into",
    "(INSERT\\s+INTO.*)"
  )

  case object INSERT_OVERWRITE extends SqlCommand(
    "insert overwrite",
    "(INSERT\\s+OVERWRITE.*)"
  )


  //---- SELECT Statements #
  case object SELECT extends SqlCommand(
    "select",
    "(SELECT.*)"
  )

  //---- USE Statements #
  case object USE_CATALOG extends SqlCommand(
    "use catalog",
    "USE\\s+CATALOG\\s+(.*)"
  )

  case object USE extends SqlCommand(
    "use",
    "USE\\s+(?!CATALOG)(.*)"
  )


  case object DESC extends SqlCommand(
    "desc",
    "DESC\\s+(.*)"
  )

  case object DESCRIBE extends SqlCommand(
    "describe",
    "DESCRIBE\\s+(.*)"
  )

  /**
   * <pre>
   * EXPLAIN PLAN FOR <query_statement_or_insert_statement>
   * </pre>
   */
  case object EXPLAIN extends SqlCommand(
    "explain plan for",
    "(EXPLAIN\\s+PLAN\\s+FOR\\s+(SELECT\\s+.*|INSERT\\s+.*))"
  )

  case object SET extends SqlCommand(
    "set",
    "SET(\\s+(\\S+)\\s*=(.*))?", {
      case a if a.length < 3 => None
      case a if a.head == null => Some(Array[String](a.head))
      case a => Some(Array[String](a(1), a(2)))
    }
  )

  case object RESET extends SqlCommand(
    "reset",
    "RESET\\s*(.*)", {
      case x if x.head.nonEmpty => Some(Array[String](x.head))
      case _ => Some(Array[String]("ALL"))
    }
  )

  /**
   * <pre>
   * SQL Client execute each INSERT INTO statement as a single Flink job. However,
   * this is sometimes not optimal because some part of the pipeline can be reused.
   * SQL Client supports STATEMENT SET syntax to execute a set of SQL statements.
   * This is an equivalent feature with StatementSet in Table API.
   * The STATEMENT SET syntax encloses one or more INSERT INTO statements.
   * All statements in a STATEMENT SET block are holistically optimized and executed as a single Flink job.
   * Joint optimization and execution allows for reusing common intermediate results and can therefore significantly
   * improve the efficiency of executing multiple queries.
   * </pre>
   */
  case object BEGIN_STATEMENT_SET extends SqlCommand(
    "begin statement set",
    "BEGIN\\s+STATEMENT\\s+SET",
    Converters.NO_OPERANDS
  )

  case object END_STATEMENT_SET extends SqlCommand(
    "end statement set",
    "END",
    Converters.NO_OPERANDS
  )

}

/**
 * Call of SQL command with operands and command type.
 */
case class SqlCommandCall(command: SqlCommand, operands: Array[String], originSql: String) {
  def this(command: SqlCommand) {
    this(command, new Array[String](0), null)
  }
}


case class SqlError(
                     errorType: SqlErrorType,
                     exception: String = null,
                     sql: String = null
                   ) {

  override def toString: String = SqlError.toString(this)

}

object SqlError {

  //不可见分隔符.
  private[core] val separator = "\001"

  def toString(sqlError: SqlError): String = {
    s"${sqlError.errorType.getValue}${SqlError.separator}${sqlError.exception}${SqlError.separator}${sqlError.sql}"
  }

  def fromString(string: String): SqlError = {
    string match {
      case null => null
      case x => x.split(separator) match {
        case Array(a, b, c) => SqlError(SqlErrorType.of(a.toInt), b, c)
      }
    }
  }

}
