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
package com.streamxhub.streamx.common.util

import java.util.Scanner
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.control.Breaks._

/**
 * Split text into multiple sql statements.
 *
 * refer to https://github.com/apache/zeppelin/blob/master/zeppelin-interpreter/
 * src/main/java/org/apache/zeppelin/interpreter/util/SqlSplitter.java
 *
 */
object SqlSplitter {

  private lazy val singleLineCommentPrefixList = Set[String]("--")

  /**
   * Split whole text into multiple sql statements.
   * Two Steps:
   * Step 1, split the whole text into multiple sql statements.
   * Step 2, refine the results. Replace the preceding sql statements with empty lines, so that
   * we can get the correct line number in the parsing error message.
   * e.g:
   * select a from table_1;
   * select a from table_2;
   * select a from table_3;
   * The above text will be splitted into:
   * sql_1: select a from table_1
   * sql_2: \nselect a from table_2
   * sql_3: \n\nselect a from table_3
   *
   * @param sql
   * @return
   */
  def splitSql(sql: String): List[(Int, Int, String)] = {
    val queries = ListBuffer[String]()
    val lastIndex = if (sql != null && sql.nonEmpty) sql.length - 1 else 0
    var query = new StringBuilder

    var multiLineComment = false
    var singleLineComment = false
    var singleQuoteString = false
    var doubleQuoteString = false
    var lineNum: Int = 0
    val lineNumMap = new collection.mutable.HashMap[Int, (Int, Int)]()

    val scanner = new Scanner(sql)
    val lineNotEmpty = new collection.mutable.HashMap[Int, Boolean]
    var count = 0
    while (scanner.hasNextLine) {
      count += 1
      val line = scanner.nextLine()
      val nonEmpty = line.trim.nonEmpty || !line.trim.startsWith("--")
      lineNotEmpty += count -> nonEmpty
    }

    def findStartLine(num: Int): Int = if (lineNotEmpty(num)) num else findStartLine(num + 1)

    def markLineNumber(): Unit = {
      if (lineNumMap.isEmpty) {
        lineNumMap += (0 -> (1 -> lineNum))
      } else {
        val index = lineNumMap.size
        val start = lineNumMap(lineNumMap.size - 1)._2 + 1
        lineNumMap += (index -> (findStartLine(start) -> lineNum))
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
            query = new StringBuilder
          }
        } else if (idx == lastIndex) {
          markLineNumber()

          // meet the last character
          if (!singleLineComment && !multiLineComment) {
            query += ch
          }

          if (query.toString.trim.nonEmpty) {
            queries += query.toString
            query = new StringBuilder
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
          refinedQueries(refinedQueries.size - 1) = lastRefinedQuery + extractLineBreaks(currStatement)
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
    val list = new ArrayBuffer[(Int, Int, String)]()
    refinedQueries.foreach(x => {
      val line = lineNumMap(x._1)
      list += Tuple3(line._1, line._2, x._2)
    })
    list.toList
  }


  /**
   * extract line breaks
   *
   * @param text
   * @return
   */
  private[this] def extractLineBreaks(text: String) = {
    val builder = new StringBuilder
    for (i <- 0 until text.length) {
      if (text.charAt(i) == '\n') {
        builder.append('\n')
      }
    }
    builder.toString
  }

  private[this] def isSingleLineComment(text: String) = text.trim.startsWith("--")

  private[this] def isMultipleLineComment(text: String) = text.trim.startsWith("/*") && text.trim.endsWith("*/")

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
