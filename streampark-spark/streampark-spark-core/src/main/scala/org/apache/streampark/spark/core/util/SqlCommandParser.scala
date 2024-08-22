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

import org.apache.streampark.common.conf.ConfigKeys.PARAM_PREFIX
import org.apache.streampark.common.enums.SparkSqlValidationFailedType
import org.apache.streampark.common.util.Logger

import enumeratum.EnumEntry
import org.apache.commons.lang3.StringUtils

import java.lang.{Boolean => JavaBool}
import java.util.Scanner
import java.util.regex.{Matcher, Pattern}

import scala.annotation.tailrec
import scala.collection.{immutable, mutable}
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks.{break, breakable}

object SqlCommandParser extends Logger {

  def parseSQL(
      sql: String,
      validationCallback: SparkSqlValidationResult => Unit = null): List[SqlCommandCall] = {
    val sqlEmptyError = "verify failed: spark sql cannot be empty."
    require(StringUtils.isNotBlank(sql), sqlEmptyError)
    val sqlSegments = SqlSplitter.splitSql(sql)
    sqlSegments match {
      case s if s.isEmpty =>
        if (validationCallback != null) {
          validationCallback(
            SparkSqlValidationResult(
              success = false,
              failedType = SparkSqlValidationFailedType.VERIFY_FAILED,
              exception = sqlEmptyError))
          null
        } else {
          throw new IllegalArgumentException(sqlEmptyError)
        }
      case segments =>
        val calls = new ListBuffer[SqlCommandCall]
        for (segment <- segments) {
          parseLine(segment) match {
            case Some(x) => calls += x
            case _ =>
              if (validationCallback != null) {
                validationCallback(
                  SparkSqlValidationResult(
                    success = false,
                    failedType = SparkSqlValidationFailedType.UNSUPPORTED_SQL,
                    lineStart = segment.start,
                    lineEnd = segment.end,
                    exception = s"unsupported sql",
                    sql = segment.sql))
              } else {
                throw new UnsupportedOperationException(s"unsupported sql: ${segment.sql}")
              }
          }
        }

        calls.toList match {
          case c if c.isEmpty =>
            if (validationCallback != null) {
              validationCallback(
                SparkSqlValidationResult(
                  success = false,
                  failedType = SparkSqlValidationFailedType.VERIFY_FAILED,
                  exception = "spark sql syntax error, no executable sql"))
              null
            } else {
              throw new UnsupportedOperationException("spark sql syntax error, no executable sql")
            }
          case r => r
        }
    }
  }

  private[this] def parseLine(sqlSegment: SqlSegment): Option[SqlCommandCall] = {
    val sqlCommand = SqlCommand.get(sqlSegment.sql.trim)
    if (sqlCommand == null) None
    else {
      val matcher = sqlCommand.matcher
      val groups = new Array[String](matcher.groupCount)
      for (i <- groups.indices) {
        groups(i) = matcher.group(i + 1)
      }
      sqlCommand
        .converter(groups)
        .map(x =>
          SqlCommandCall(sqlSegment.start, sqlSegment.end, sqlCommand, x, sqlSegment.sql.trim))
    }
  }

}

object Converters {
  val NO_OPERANDS = (_: Array[String]) => Some(Array.empty[String])
}

sealed abstract class SqlCommand(
    val name: String,
    private val regex: String,
    val converter: Array[String] => Option[Array[String]] = (x: Array[String]) =>
      Some(Array[String](x.head)))
  extends EnumEntry {
  var matcher: Matcher = _

  def matches(input: String): Boolean = {
    if (StringUtils.isBlank(regex)) false
    else {
      val pattern =
        Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
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

  // ---- ALTER Statements--------------------------------------------------------------------------------------------------------------------------------
  /**
   * <pre> ALTER { DATABASE | SCHEMA | NAMESPACE } database_name SET { DBPROPERTIES | PROPERTIES } ( property_name = property_value [ , ... ] )
   * ALTER { DATABASE | SCHEMA | NAMESPACE } database_name SET LOCATION 'new_location'</pre>
   */
  case object ALTER_DATABASE extends SqlCommand("alter database", "(ALTER\\s+(DATABASE\\s+|SCHEMA\\s+|NAMESPACE\\s+)\\s+.+)")

  /**
   * <pre> ALTER TABLE table_identifier ....</pre>
   */
  case object ALTER_TABLE extends SqlCommand("alter table", "(ALTER\\s+TABLE\\s+.+)")

  /**
   * <pre> ALTER VIEW view_identifier ....</pre>
   */
  case object ALTER_VIEW extends SqlCommand("alter view", "(ALTER\\s+VIEW\\s+.+)")

  // ---- CREATE Statements--------------------------------------------------------------------------------------------------------------------------------

  /**
   * <pre> CREATE { DATABASE | SCHEMA } [ IF NOT EXISTS ] database_name<br> [ COMMENT database_comment ]
   * [ LOCATION database_directory ]<br> [ WITH DBPROPERTIES ( property_name = property_value [ , ... ] ) ]</pre>
   */
  case object CREATE_DATABASE extends SqlCommand("create database", "(CREATE\\s+(DATABASE\\s+|SCHEMA\\s+)\\s+.+)")

  /**
   * <pre> CREATE [ OR REPLACE ] [ TEMPORARY ] FUNCTION [ IF NOT EXISTS ]
   * function_name AS class_name [ resource_locations ]</pre
   */
  case object CREATE_FUNCTION
    extends SqlCommand(
      "create function",
      "(CREATE\\s+(OR\\s+REPLACE\\s+|)(TEMPORARY\\s+|)FUNCTION\\s+(IF\\s+NOT\\s+EXISTS\\s+|)(\\S+)\\s+AS\\s+.*)")

  /**
   * <pre> CREATE TABLE [ IF NOT EXISTS ] table_identifier
   *     [ ( col_name1 col_type1 [ COMMENT col_comment1 ], ... ) ]
   *     USING data_source ....</pre
   *
   * <pre> CREATE [ EXTERNAL ] TABLE [ IF NOT EXISTS ] table_identifier
   *     [ ( col_name1[:] col_type1 [ COMMENT col_comment1 ], ... ) ]
   *     [ COMMENT table_comment ] ....</pre>
   *
   * <pre> CREATE TABLE [IF NOT EXISTS] table_identifier LIKE source_table_identifier
   *     USING data_source ....</pre>
   */
  case object CREATE_TABLE
    extends SqlCommand("create table", "(CREATE\\s+(EXTERNAL\\s+|)TABLE\\s+(IF\\s+NOT\\s+EXISTS\\s+|).+)")

  /**
   * <pre> CREATE [TEMPORARY] VIEW [IF NOT EXISTS] [catalog_name.][db_name.]view_name
   * [( columnName[, columnName ]* )] [COMMENT view_comment] AS query_expression </pre>
   */
  case object CREATE_VIEW
    extends SqlCommand(
      "create view",
      "(CREATE\\s+(OR\\s+REPLACE\\s+|)((GLOBAL\\s+|)TEMPORARY\\s+|)VIEW\\s+(IF\\s+NOT\\s+EXISTS\\s+|)(\\S+)\\s+AS\\s+SELECT\\s+.+)")

  // ----DROP Statements--------------------------------------------------------------------------------------------------------------------------------

  /** <pre> DROP { DATABASE | SCHEMA } [ IF EXISTS ] dbname [ RESTRICT | CASCADE ]</pre> */
  case object DROP_DATABASE extends SqlCommand("drop database", "(DROP\\s+(DATABASE\\s+|SCHEMA\\s+)(IF\\s+EXISTS\\s+|).+)")

  /** <pre> DROP [ TEMPORARY ] FUNCTION [ IF EXISTS ] function_name</pre> */
  case object DROP_FUNCTION
    extends SqlCommand(
      "drop function",
      "(DROP\\s+(TEMPORARY\\s+|)FUNCTION\\s+(IF\\s+EXISTS\\s+|).+)")

  /** <pre> DROP TABLE [ IF EXISTS ] table_identifier [ PURGE ]</pre> */
  case object DROP_TABLE extends SqlCommand("drop table", "(DROP\\s+TABLE\\s+(IF\\s+EXISTS\\s+|).+)")

  /** <pre> DROP VIEW [ IF EXISTS ] view_identifier</pre> */
  case object DROP_VIEW extends SqlCommand("drop view", "(DROP\\s+VIEW\\s+(IF\\s+EXISTS\\s+|).+)")

  // ---- REPAIR Statements--------------------------------------------------------------------------------------------------------------------------------

  /** <pre> [MSCK] REPAIR TABLE table_identifier [{ADD|DROP|SYNC} PARTITIONS]</pre> */
  case object REPAIR_TABLE extends SqlCommand("repair table", "((MSCK\\s+|)DROP\\s+TABLE\\s+.+)")

  // ---- TRUNCATE Statements-----------------------------------------------------------------------------------------------------------------------------

  /** <pre> TRUNCATE TABLE table_identifier [ partition_spec ]</pre> */
  case object TRUNCATE_TABLE extends SqlCommand("truncate table", "(TRUNCATE\\s+TABLE\\s+.+)")

  // ---- TRUNCATE Statements-----------------------------------------------------------------------------------------------------------------------------

  /** <pre> USE database_name</pre> */
  case object USE_DATABASE extends SqlCommand("use database", "(USE\\s+.+)")

  // ---- INSERT Statement--------------------------------------------------------------------------------------------------------------------------------

  /**
   * <pre> INSERT [ INTO | OVERWRITE ] [ TABLE ] table_identifier [ partition_spec ] [ ( column_list ) ]
   * { VALUES ( { value | NULL } [ , ... ] ) [ , ( ... ) ] | query }</pre>
   */
  case object INSERT extends SqlCommand("insert", "(INSERT\\s+(INTO|OVERWRITE)\\s+.+)")

  /**
   * <pre> INSERT INTO [ TABLE ] table_identifier REPLACE WHERE boolean_expression query
   */
  case object INSERT_REPLACE extends SqlCommand("insert replace", "(INSERT\\s+INTO\\s+(TABLE|)\\s+(\\S+)\\s+REPLACE\\s+WHERE\\s+.+)")

  /**
   * <pre> INSERT OVERWRITE [ LOCAL ] DIRECTORY [ directory_path ]
   * { spark_format | hive_format }
   * { VALUES ( { value | NULL } [ , ... ] ) [ , ( ... ) ] | query }<pre>
   */
  case object INSERT_OVERWRITE_DIRECTORY extends SqlCommand("insert overwrite directory", "(INSERT\\s+OVERWRITE\\s+(LOCAL|)\\s+DIRECTORY\\s+.+)")

  // ----LOAD Statements--------------------------------------------------------------------------------------------------------------------------------
  /**
   * <pre> LOAD DATA [ LOCAL ] INPATH path [ OVERWRITE ] INTO TABLE table_identifier [ partition_spec ]</pre>
   */
  case object LOAD_DATE extends SqlCommand("load data", "(LOAD\\s+DATA\\s+(LOCAL|)\\s+INPATH\\s+.+)")

  // ---- SELECT Statements--------------------------------------------------------------------------------------------------------------------------------
  case object SELECT extends SqlCommand("select", "(SELECT\\s+.+)")

  case object WITH_SELECT extends SqlCommand("with select", "(WITH\\s+.+)")

  // ---- EXPLAIN Statement--------------------------------------------------------------------------------------------------------------------------------

  /** <pre>EXPLAIN [ EXTENDED | CODEGEN | COST | FORMATTED ] statement<pre> */
  case object EXPLAIN extends SqlCommand("explain", "(EXPLAIN\\s+(EXTENDED|CODEGEN|COST|FORMATTED)\\s.+)")

  // ---- ADD Statement--------------------------------------------------------------------------------------------------------------------------------
  /** <pre> ADD { FILE | FILES } resource_name [ ... ]</pre> */
  case object ADD_FILE extends SqlCommand("add file", "(ADD\\s+(FILE|FILES)\\s+.+)")

  /** <pre> ADD { JAR | JARS } file_name [ ... ]</pre> */
  case object ADD_JAR extends SqlCommand("add jar", "(ADD\\s+(JAR|JARS)\\s+.+)")

  // ---- ANALYZE Statement--------------------------------------------------------------------------------------------------------------------------------

  /**
   * <pre> ANALYZE TABLE table_identifier [ partition_spec ]
   * COMPUTE STATISTICS [ NOSCAN | FOR COLUMNS col [ , ... ] | FOR ALL COLUMNS ]
   *
   * ANALYZE TABLES [ { FROM | IN } database_name ] COMPUTE STATISTICS [ NOSCAN ]</pre>
   */
  case object ANALYZE_TABLE extends SqlCommand("analyze table", "(ANALYZE\\s+(TABLES|TABLE)\\s+.+)")

  // ---- CACHE Statement--------------------------------------------------------------------------------------------------------------------------------

  /**
   * <pre> CACHE [ LAZY ] TABLE table_identifier
   * [ OPTIONS ( 'storageLevel' [ = ] value ) ] [ [ AS ] query ]</pre>
   */
  case object CACHE_TABLE extends SqlCommand("cache table", "(CACHE\\s+(LAZY\\s+|)TABLE\\s+.+)")

  /** <pre>UNCACHE TABLE [ IF EXISTS ] table_identifier</pre> */
  case object UNCACHE_TABLE extends SqlCommand("uncache table", "(UNCACHE\\s+TABLE\\s+.+)")

  /** <pre>CLEAR CACHE</pre> */
  case object CLEAR_CACHE extends SqlCommand("clear cache", "(CLEAR\\s+CACHE\\s*)")

  // ---- DESCRIBE Statement--------------------------------------------------------------------------------------------------------------------------------

  case object DESCRIBE extends SqlCommand("describe", "((DESCRIBE|DESC)\\s+.+)")

  // ---- LIST Statement--------------------------------------------------------------------------------------------------------------------------------

  /** <pre>LIST { FILE | FILES } file_name [ ... ]</pre> */
  case object LIST_FILE extends SqlCommand("list file", "(LIST\\s+(FILE|FILES)\\s+.+)")

  /** <pre>LIST { JAR | JARS } file_name [ ... ]</pre> */
  case object LIST_JAR extends SqlCommand("list jar", "(LIST\\s+(JAR|JARS)\\s+.+)")

  // ---- REFRESH Statement--------------------------------------------------------------------------------------------------------------------------------
  case object REFRESH extends SqlCommand("refresh", "(REFRESH\\s+.+)")

  // ----SET Statements--------------------------------------------------------------------------------------------------------------------------------

  /**
   * <pre> SET
   * SET [ -v ]
   * SET property_key[ = property_value ]</pre>
   */
  case object SET
    extends SqlCommand("set", "(SET(|\\s+.+))")

  // ----RESET Statements--------------------------------------------------------------------------------------------------------------------------------

  /**
   * <pre> RESET;
   * RESET configuration_key;</pre>
   */
  case object RESET extends SqlCommand("reset", "RESET\\s*(.*)?")

  // ----SHOW Statements--------------------------------------------------------------------------------------------------------------------------------

  /** <pre>SHOW COLUMNS table_identifier [ database ]</pre> */
  case object SHOW_COLUMNS extends SqlCommand("show columns", "(SHOW\\s+COLUMNS\\s+.+)")

  /** <pre>SHOW CREATE TABLE table_identifier [ AS SERDE ]</pre> */
  case object SHOW_CREATE_TABLE extends SqlCommand("show create table", "(SHOW\\s+CREATE\\s+TABLE\\s+.+)")

  /** <pre>SHOW { DATABASES | SCHEMAS } [ LIKE regex_pattern ]</pre> */
  case object SHOW_DATABASES extends SqlCommand("show databases", "(SHOW\\s+(DATABASES|SCHEMAS)\\s+.+)")

  /** <pre>SHOW [ function_kind ] FUNCTIONS [ { FROM | IN } database_name ] [ LIKE regex_pattern ]</pre> */
  case object SHOW_FUNCTIONS extends SqlCommand("show functions", "(SHOW\\s+(USER|SYSTEM|ALL|)\\s+FUNCTIONS\\s+.+)")

  /** <pre>SHOW PARTITIONS table_identifier [ partition_spec ]</pre> */
  case object SHOW_PARTITIONS extends SqlCommand("show partitions", "(SHOW\\s+PARTITIONS\\s+.+)")

  /** <pre>SHOW TABLE EXTENDED [ { IN | FROM } database_name ] LIKE regex_pattern [ partition_spec ]</pre> */
  case object SHOW_TABLE_EXTENDED extends SqlCommand("show table extended", "(SHOW\\s+TABLE\\s+EXTENDED\\s+.+)")

  /** <pre>SHOW TABLES [ { FROM | IN } database_name ] [ LIKE regex_pattern ]</pre> */
  case object SHOW_TABLES extends SqlCommand("show tables", "(SHOW\\s+TABLES\\s+.+)")

  /** <pre>SHOW TBLPROPERTIES table_identifier [ ( unquoted_property_key | property_key_as_string_literal ) ]</pre> */
  case object SHOW_TBLPROPERTIES extends SqlCommand("show tblproperties", "(SHOW\\s+TBLPROPERTIES\\s+.+)")

  /** <pre>SHOW VIEWS [ { FROM | IN } database_name ] [ LIKE regex_pattern ]</pre> */
  case object SHOW_VIEWS extends SqlCommand("show views", "(SHOW\\s+VIEWS\\s+.+)")

  private[this] def cleanUp(sql: String): String =
    sql.trim.replaceAll("^(['\"])|(['\"])$", "")

}

/** Call of SQL command with operands and command type. */
case class SqlCommandCall(
    lineStart: Int,
    lineEnd: Int,
    command: SqlCommand,
    operands: Array[String],
    originSql: String) {}

case class SparkSqlValidationResult(
    success: JavaBool = true,
    failedType: SparkSqlValidationFailedType = null,
    lineStart: Int = 0,
    lineEnd: Int = 0,
    errorLine: Int = 0,
    errorColumn: Int = 0,
    sql: String = null,
    exception: String = null)

case class SqlSegment(start: Int, end: Int, sql: String)

object SqlSplitter {

  private lazy val singleLineCommentPrefixList = Set[String](PARAM_PREFIX)

  /**
   * Split whole text into multiple sql statements. Two Steps: Step 1, split the whole text into
   * multiple sql statements. Step 2, refine the results. Replace the preceding sql statements with
   * empty lines, so that we can get the correct line number in the parsing error message. e.g:
   * select a from table_1; select a from table_2; select a from table_3; The above text will be
   * splitted into: sql_1: select a from table_1 sql_2: \nselect a from table_2 sql_3: \n\nselect a
   * from table_3
   *
   * @param sql
   * @return
   */
  def splitSql(sql: String): List[SqlSegment] = {
    val queries = ListBuffer[String]()
    val lastIndex = if (StringUtils.isNotBlank(sql)) sql.length - 1 else 0
    var query = new mutable.StringBuilder

    var multiLineComment = false
    var singleLineComment = false
    var singleQuoteString = false
    var doubleQuoteString = false
    var lineNum: Int = 0
    val lineNumMap = new collection.mutable.HashMap[Int, (Int, Int)]()

    // Whether each line of the record is empty. If it is empty, it is false. If it is not empty, it is true
    val lineDescriptor = {
      val scanner = new Scanner(sql)
      val descriptor = new collection.mutable.HashMap[Int, Boolean]
      var lineNumber = 0
      var startComment = false
      var hasComment = false

      while (scanner.hasNextLine) {
        lineNumber += 1
        val line = scanner.nextLine().trim
        val nonEmpty =
          StringUtils.isNotBlank(line) && !line.startsWith(PARAM_PREFIX)
        if (line.startsWith("/*")) {
          startComment = true
          hasComment = true
        }

        descriptor += lineNumber -> (nonEmpty && !hasComment)

        if (startComment && line.endsWith("*/")) {
          startComment = false
          hasComment = false
        }
      }
      descriptor
    }

    @tailrec
    def findStartLine(num: Int): Int =
      if (num >= lineDescriptor.size || lineDescriptor(num)) num
      else findStartLine(num + 1)

    def markLineNumber(): Unit = {
      val line = lineNum + 1
      if (lineNumMap.isEmpty) {
        lineNumMap += (0 -> (findStartLine(1) -> line))
      } else {
        val index = lineNumMap.size
        val start = lineNumMap(lineNumMap.size - 1)._2 + 1
        lineNumMap += (index -> (findStartLine(start) -> line))
      }
    }

    for (idx <- 0 until sql.length) {

      if (sql.charAt(idx) == '\n') lineNum += 1

      breakable {
        val ch = sql.charAt(idx)

        // end of single line comment
        if (singleLineComment && (ch == '\n')) {
          singleLineComment = false
          query += ch
          if (idx == lastIndex && query.toString.trim.nonEmpty) {
            // add query when it is the end of sql.
            queries += query.toString
          }
          break()
        }

        // end of multiple line comment
        if (multiLineComment && (idx - 1) >= 0 && sql.charAt(idx - 1) == '/'
          && (idx - 2) >= 0 && sql.charAt(idx - 2) == '*') {
          multiLineComment = false
        }

        // single quote start or end mark
        if (ch == '\'' && !(singleLineComment || multiLineComment)) {
          if (singleQuoteString) {
            singleQuoteString = false
          } else if (!doubleQuoteString) {
            singleQuoteString = true
          }
        }

        // double quote start or end mark
        if (ch == '"' && !(singleLineComment || multiLineComment)) {
          if (doubleQuoteString && idx > 0) {
            doubleQuoteString = false
          } else if (!singleQuoteString) {
            doubleQuoteString = true
          }
        }

        // single line comment or multiple line comment start mark
        if (!singleQuoteString && !doubleQuoteString && !multiLineComment && !singleLineComment && idx < lastIndex) {
          if (isSingleLineComment(sql.charAt(idx), sql.charAt(idx + 1))) {
            singleLineComment = true
          } else if (sql.charAt(idx) == '/' && sql.length > (idx + 2)
            && sql.charAt(idx + 1) == '*' && sql.charAt(idx + 2) != '+') {
            multiLineComment = true
          }
        }

        if (ch == ';' && !singleQuoteString && !doubleQuoteString && !multiLineComment && !singleLineComment) {
          markLineNumber()
          // meet the end of semicolon
          if (query.toString.trim.nonEmpty) {
            queries += query.toString
            query = new mutable.StringBuilder
          }
        } else if (idx == lastIndex) {
          markLineNumber()

          // meet the last character
          if (!singleLineComment && !multiLineComment) {
            query += ch
          }

          if (query.toString.trim.nonEmpty) {
            queries += query.toString
            query = new mutable.StringBuilder
          }
        } else if (!singleLineComment && !multiLineComment) {
          // normal case, not in single line comment and not in multiple line comment
          query += ch
        } else if (ch == '\n') {
          query += ch
        }
      }
    }

    val refinedQueries = new collection.mutable.HashMap[Int, String]()
    for (i <- queries.indices) {
      val currStatement = queries(i)
      if (isSingleLineComment(currStatement) || isMultipleLineComment(currStatement)) {
        // transform comment line as blank lines
        if (refinedQueries.nonEmpty) {
          val lastRefinedQuery = refinedQueries.last
          refinedQueries(refinedQueries.size - 1) =
            lastRefinedQuery + extractLineBreaks(currStatement)
        }
      } else {
        var linesPlaceholder = ""
        if (i > 0) {
          linesPlaceholder = extractLineBreaks(refinedQueries(i - 1))
        }
        // add some blank lines before the statement to keep the original line number
        val refinedQuery = linesPlaceholder + currStatement
        refinedQueries += refinedQueries.size -> refinedQuery
      }
    }

    val set = new ListBuffer[SqlSegment]
    refinedQueries.foreach(x => {
      val line = lineNumMap(x._1)
      set += SqlSegment(line._1, line._2, x._2)
    })
    set.toList.sortWith((a, b) => a.start < b.start)
  }

  /**
   * extract line breaks
   *
   * @param text
   * @return
   */
  private[this] def extractLineBreaks(text: String): String = {
    val builder = new mutable.StringBuilder
    for (i <- 0 until text.length) {
      if (text.charAt(i) == '\n') {
        builder.append('\n')
      }
    }
    builder.toString
  }

  private[this] def isSingleLineComment(text: String) =
    text.trim.startsWith(PARAM_PREFIX)

  private[this] def isMultipleLineComment(text: String) =
    text.trim.startsWith("/*") && text.trim.endsWith("*/")

  /**
   * check single-line comment
   *
   * @param curChar
   * @param nextChar
   * @return
   */
  private[this] def isSingleLineComment(curChar: Char, nextChar: Char): Boolean = {
    var flag = false
    for (singleCommentPrefix <- singleLineCommentPrefixList) {
      if (singleCommentPrefix.length == 1) {
        if (curChar == singleCommentPrefix.charAt(0)) {
          flag = true
        }
      }
      if (singleCommentPrefix.length == 2) {
        if (curChar == singleCommentPrefix.charAt(0) && nextChar == singleCommentPrefix.charAt(1)) {
          flag = true
        }
      }
    }
    flag
  }

}
