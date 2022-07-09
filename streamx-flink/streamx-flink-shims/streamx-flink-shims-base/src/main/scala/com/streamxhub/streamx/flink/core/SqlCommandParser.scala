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
import com.streamxhub.streamx.common.util.SqlSplitter.SqlSegment
import com.streamxhub.streamx.common.util.{Logger, SqlSplitter}
import enumeratum.EnumEntry

import java.util.regex.{Matcher, Pattern}
import java.lang.{Boolean => JavaBool}
import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks.{break, breakable}

object SqlCommandParser extends Logger {

  def parseSQL(sql: String, validationCallback: FlinkSqlValidationResult => Unit = null): List[SqlCommandCall] = {
    val sqlEmptyError = "verify failed: flink sql cannot be empty."
    require(sql != null && sql.trim.nonEmpty, sqlEmptyError)
    val sqlSegments = SqlSplitter.splitSql(sql)
    sqlSegments match {
      case s if s.isEmpty =>
        if (validationCallback != null) {
          validationCallback(
            FlinkSqlValidationResult(
              success = false,
              failedType = FlinkSqlValidationFailedType.VERIFY_FAILED,
              exception = sqlEmptyError
            )
          )
          null
        } else {
          throw new IllegalArgumentException(sqlEmptyError)
        }
      case segments =>
        val calls = new ArrayBuffer[SqlCommandCall]
        for (segment <- segments) {
          parseLine(segment) match {
            case Some(x) => calls += x
            case _ =>
              if (validationCallback != null) {
                validationCallback(
                  FlinkSqlValidationResult(
                    success = false,
                    failedType = FlinkSqlValidationFailedType.UNSUPPORTED_SQL,
                    lineStart = segment.start,
                    lineEnd = segment.end,
                    exception = s"unsupported sql",
                    sql = segment.sql
                  )
                )
              } else {
                throw new UnsupportedOperationException(s"unsupported sql: ${segment.sql}")
              }
          }
        }

        calls.toList match {
          case Nil =>
            if (validationCallback != null) {
              validationCallback(
                FlinkSqlValidationResult(
                  success = false,
                  failedType = FlinkSqlValidationFailedType.VERIFY_FAILED,
                  exception = "flink sql syntax error, no executable sql"
                )
              )
              null
            } else {
              throw new UnsupportedOperationException("flink sql syntax error, no executable sql")
            }
          case r => r
        }
    }
  }

  private[this] def parseLine(sqlSegment: SqlSegment): Option[SqlCommandCall] = {
    // parse
    val sqlCommand = SqlCommand.get(sqlSegment.sql.trim)
    if (sqlCommand == null) None else {
      val matcher = sqlCommand.matcher
      val groups = new Array[String](matcher.groupCount)
      for (i <- groups.indices) {
        groups(i) = matcher.group(i + 1)
      }
      sqlCommand.converter(groups).map(x => SqlCommandCall(sqlSegment.start, sqlSegment.end, sqlCommand, x, sqlSegment.sql.trim))
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

  def get(stmt: String): SqlCommand = {
    var cmd: SqlCommand = null
    breakable {
      this.values.foreach(x => {
        if (x.matches(stmt)) {
          cmd = x
          break()
        }
      })
    }
    cmd
  }

  val values: immutable.IndexedSeq[SqlCommand] = findValues

  //---- SELECT Statements
  case object SELECT extends SqlCommand(
    "select",
    "(SELECT\\s+.*)"
  )


  //----CREATE Statements----------------


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
    "CREATE\\s+(TEMPORARY\\s+|)VIEW\\s+(\\S+)\\s+AS\\s+(.*)"
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


  //----DROP Statements----------------

  /**
   * <pre>
   * DROP statements are used to remove a catalog with the given catalog name or to remove a registered table/view/function from the current or specified Catalog.
   *
   * Flink SQL supports the following DROP statements for now:
   * * DROP CATALOG
   * * DROP TABLE
   * * DROP DATABASE
   * * DROP VIEW
   * * DROP FUNCTION
   * </pre>
   */

  /**
   * <strong>DROP CATALOG [IF EXISTS] catalog_name</strong>
   */
  case object DROP_CATALOG extends SqlCommand(
    "drop catalog",
    "(DROP\\s+CATALOG\\s+.*)"
  )

  /**
   * <strong>DROP DATABASE [IF EXISTS] [catalog_name.]db_name [ (RESTRICT | CASCADE) ]</strong>
   */
  case object DROP_DATABASE extends SqlCommand(
    "drop database",
    "(DROP\\s+DATABASE\\s+.*)"
  )

  /**
   * <strong>DROP [TEMPORARY] TABLE [IF EXISTS] [catalog_name.][db_name.]table_name</strong>
   */
  case object DROP_TABLE extends SqlCommand(
    "drop table",
    "(DROP\\s+(TEMPORARY\\s+|)TABLE\\s+.*)"
  )

  /**
   * <strong>DROP [TEMPORARY] VIEW  [IF EXISTS] [catalog_name.][db_name.]view_name</strong>
   */
  case object DROP_VIEW extends SqlCommand(
    "drop view",
    "(DROP\\s+(TEMPORARY\\s+|)VIEW\\s+.*)"
  )

  /**
   * <strong>DROP [TEMPORARY|TEMPORARY SYSTEM] FUNCTION [IF EXISTS] [catalog_name.][db_name.]function_name</strong>
   */
  case object DROP_FUNCTION extends SqlCommand(
    "drop function",
    "(DROP\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+.*)"
  )


  //----ALTER Statements

  /**
   * <strong>ALTER DATABASE [catalog_name.]db_name SET (key1=val1, key2=val2, ...)</strong>
   */
  case object ALTER_DATABASE extends SqlCommand(
    "alter database",
    "(ALTER\\s+DATABASE\\s+.*)"
  )

  /**
   * <strong>ALTER TABLE [catalog_name.][db_name.]table_name RENAME TO new_table_name</strong>
   *
   * <strong>ALTER TABLE [catalog_name.][db_name.]table_name SET (key1=val1, key2=val2, ...)</strong>
   */
  case object ALTER_TABLE extends SqlCommand(
    "alter table",
    "(ALTER\\s+TABLE\\s+.*)"
  )


  /**
   * <strong>ALTER VIEW [catalog_name.][db_name.]view_name RENAME TO new_view_name</strong>
   *
   * <strong>ALTER VIEW [catalog_name.][db_name.]view_name AS new_query_expression</strong>
   */
  case object ALTER_VIEW extends SqlCommand(
    "alter view",
    "(ALTER\\s+VIEW\\s+.*)"
  )

  /**
   * <strong>
   * ALTER [TEMPORARY|TEMPORARY SYSTEM] FUNCTION
   * [IF EXISTS] [catalog_name.][db_name.]function_name
   * AS identifier [LANGUAGE JAVA|SCALA|PYTHON]
   * </strong>
   */
  case object ALTER_FUNCTION extends SqlCommand(
    "alter function",
    "(ALTER\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+.*)"
  )


  //---- INSERT Statement

  /**
   * INSERT { INTO | OVERWRITE } [catalog_name.][db_name.]table_name [PARTITION part_spec] [column_list] select_statement
   * INSERT { INTO | OVERWRITE } [catalog_name.][db_name.]table_name VALUES values_row [, values_row ...]
   */
  case object INSERT extends SqlCommand(
    "insert",
    "(INSERT\\s+(INTO|OVERWRITE)\\s+.*)"
  )

  //---- DESCRIBE Statement

  /**
   * { DESCRIBE | DESC } [catalog_name.][db_name.]table_name
   */
  case object DESC extends SqlCommand(
    "desc",
    "(DESC\\s+.*)"
  )

  /**
   * { DESCRIBE | DESC } [catalog_name.][db_name.]table_name
   */
  case object DESCRIBE extends SqlCommand(
    "describe",
    "(DESCRIBE\\s+.*)"
  )


  //---- DESCRIBE Statement

  /**
   * <pre>
   * EXPLAIN PLAN FOR <query_statement_or_insert_statement>
   * </pre>
   */
  case object EXPLAIN extends SqlCommand(
    "explain plan for",
    "(EXPLAIN\\s+PLAN\\s+FOR\\s+(SELECT\\s+.*|INSERT\\s+.*))"
  )


  //---- USE Statements

  /**
   * USE CATALOG catalog_name
   */
  case object USE_CATALOG extends SqlCommand(
    "use catalog",
    "(USE\\s+CATALOG\\s+.*)"
  )

  /**
   * USE MODULES module_name1[, module_name2, ...]
   */
  case object USE_MODULES extends SqlCommand(
    "use modules",
    "(USE\\s+MODULES\\s+.*)"
  )

  /**
   * USE [catalog_name.]database_name
   */
  case object USE extends SqlCommand(
    "use",
    "USE\\s+.*"
  )

  //----SHOW Statements

  /**
   * SHOW CATALOGS
   */
  case object SHOW_CATALOGS extends SqlCommand(
    "show catalogs",
    "SHOW\\s+CATALOGS",
    Converters.NO_OPERANDS
  )

  /**
   * SHOW CURRENT CATALOG
   */
  case object SHOW_CURRENT_CATALOG extends SqlCommand(
    "show current catalog",
    "SHOW\\s+CURRENT\\s+CATALOG",
    Converters.NO_OPERANDS
  )

  /**
   * SHOW DATABASES
   */
  case object SHOW_DATABASES extends SqlCommand(
    "show databases",
    "SHOW\\s+DATABASES",
    Converters.NO_OPERANDS
  )

  /**
   * SHOW CURRENT DATABASE
   */
  case object SHOW_CURRENT_DATABASE extends SqlCommand(
    "show current database",
    "SHOW\\s+CURRENT\\s+DATABASE",
    Converters.NO_OPERANDS
  )

  /**
   * SHOW TABLES
   */
  case object SHOW_TABLES extends SqlCommand(
    "show tables",
    "SHOW\\s+TABLES",
    Converters.NO_OPERANDS
  )

  /**
   * SHOW VIEWS
   */
  case object SHOW_VIEWS extends SqlCommand(
    "show views",
    "SHOW\\s+VIEWS",
    Converters.NO_OPERANDS
  )

  /**
   * SHOW [USER] FUNCTIONS
   */
  case object SHOW_FUNCTIONS extends SqlCommand(
    "show functions",
    "SHOW\\s+(USER\\s+|)FUNCTIONS",
    Converters.NO_OPERANDS
  )

  /**
   * SHOW [FULL] MODULES
   */
  case object SHOW_MODULES extends SqlCommand(
    "show modules",
    "SHOW\\s+(FULL\\s+|)MODULES",
    Converters.NO_OPERANDS
  )


  //----LOAD Statements

  /**
   * LOAD MODULE module_name [WITH ('key1' = 'val1', 'key2' = 'val2', ...)]
   */
  case object LOAD_MODULE extends SqlCommand(
    "load modules",
    "(LOAD\\s+MODULE\\s+.*)",
    Converters.NO_OPERANDS
  )


  //----UNLOAD Statements

  /**
   * UNLOAD MODULE module_name
   */
  case object UNLOAD_MODULE extends SqlCommand(
    "unload module",
    "(UNLOAD\\s+MODULE\\s+.*)",
    Converters.NO_OPERANDS
  )


  // ----SET Statements

  /**
   * SET ('key' = 'value')
   */
  case object SET extends SqlCommand(
    "set",
    "SET(\\s+(\\S+)\\s*=(.*))?", {
      case a if a.length < 3 => None
      case a if a.head == null => Some(Array[String](cleanUp(a.head)))
      case a => Some(Array[String](cleanUp(a(1)), cleanUp(a(2))))
    }
  )

  // ----SET Statements --------------------------------------------------------------------------------------------------------------------

  /**
   * RESET ('key')
   */
  case object RESET extends SqlCommand(
    "reset",
    "RESET\\s+(.*)", {
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


  private[this] def cleanUp(sql: String): String = sql.trim.replaceAll("^(['\"])|(['\"])$", "")

}

/**
 * Call of SQL command with operands and command type.
 */
case class SqlCommandCall(lineStart: Int,
                          lineEnd: Int,
                          command: SqlCommand,
                          operands: Array[String],
                          originSql: String) {
}

case class FlinkSqlValidationResult(success: JavaBool = true,
                                    failedType: FlinkSqlValidationFailedType = null,
                                    lineStart: Int = 0,
                                    lineEnd: Int = 0,
                                    errorLine: Int = 0,
                                    errorColumn: Int = 0,
                                    sql: String = null,
                                    exception: String = null
                                   )
