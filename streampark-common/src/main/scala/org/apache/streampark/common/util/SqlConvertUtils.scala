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

package org.apache.streampark.common.util

import org.apache.streampark.common.util.Implicits._

import java.util
import java.util.Scanner
import java.util.regex.Pattern

import scala.annotation.tailrec

object SqlConvertUtils extends Logger {

  private[this] val FIELD_REGEXP = Pattern.compile(
    "\\s*(.*?)\\s+(([a-z]+)\\((.*?)\\)|[a-z]+)(\\s*|((.*?)(comment)\\s+(['|\"](.*?)['|\"])|(.*?))),$",
    Pattern.CASE_INSENSITIVE)

  private[this] val PRIMARY_REGEXP =
    Pattern.compile("primary\\s+key\\s+\\((.*?)\\)", Pattern.CASE_INSENSITIVE)

  /**
   * convert to flink types
   *
   * @param dataType
   * @param length
   * @return
   */
  private[this] def toFlinkDataType(dataType: String, length: String): String = {
    dataType.toUpperCase() match {
      case "TEXT" | "LONGTEXT" => "VARCHAR"
      case "DATETIME" => "TIMESTAMP"
      case "INT" => "INTEGER"
      case x => x.toUpperCase()
    }
  }

  /**
   * convert to clickhouse types
   *
   * @param dataType
   * @param length
   * @return
   */
  private[this] def toClickhouseDataType(dataType: String, length: String): String = {
    dataType.toUpperCase() match {
      case "TEXT" | "LONGTEXT" | "BLOB" | "VARCHAR" | "VARBINARY" => "String"
      case "DATETIME" | "TIMESTAMP" => "DateTime"
      case "TINYINT" => "Int8"
      case "SMALLINT" => "Int16"
      case "FLOAT" => "Float32"
      case "DOUBLE" => "Float64"
      case "DATE" => "Date"
      case "BIGINT" | "INT" =>
        length match {
          case null => "Int32"
          case x =>
            x.trim.toInt match {
              case x if x < 3 => "Int8"
              case x if x < 5 => "Int16"
              case x if x < 9 => "Int32"
              case _ => "Int64"
            }
        }
      case "DECIMAL" =>
        length.split(",").map(_.trim.toInt) match {
          case Array(p, s) =>
            p + s match {
              case x if x <= 32 => s"Decimal32($s)"
              case x if x <= 64 => s"Decimal64($s)"
              case _ => s"Decimal128($s)"
            }
          case _ => throw new IllegalArgumentException
        }
      case x => x.toUpperCase()
    }
  }

  /**
   * An magic code for format sql. only support ddl, create table syntax, etc.
   *
   * @param sql
   * @return
   */
  private[this] def formatSql(sql: String): String = {

    val LENGTH_REGEXP = "(.*?)\\s*\\([^\\\\)|^\\n]+,$".r

    val COMMENT_REGEXP =
      Pattern.compile("(comment)\\s+(['\"])", Pattern.CASE_INSENSITIVE)

    @tailrec def commentJoin(
        map: util.Map[Integer, String],
        index: Integer,
        segment: String): (Integer, String) = {
      val matcher = COMMENT_REGEXP.matcher(segment)
      matcher.find() match {
        case b if !b => index -> segment
        case _ =>
          val s = matcher.group(2)
          val regexp = s"\\$s(,|)$$".r
          val cleaned = segment
            .replaceFirst("(?i)(comment)\\s+(['\"])", "")
            .replace(s"\\$s", "")
          regexp.findFirstIn(cleaned) match {
            case Some(_) => index -> segment
            case _ =>
              val nextLine = map(index + 1)
              commentJoin(map, index + 1, s"$segment$nextLine")
          }
      }
    }

    @tailrec def lengthJoin(
        map: util.Map[Integer, String],
        index: Integer,
        segment: String): (Integer, String) = {
      LENGTH_REGEXP.findFirstIn(segment) match {
        case None => commentJoin(map, index, segment)
        case Some(_) =>
          val nextLine = map(index + 1)
          lengthJoin(map, index + 1, s"${segment.trim}$nextLine")
      }
    }

    val body = sql
      .substring(sql.indexOf("("), sql.lastIndexOf(")") + 1)
      .replaceAll("\r|\n|\r\n", "")
      .replaceFirst("\\(", "(\n")
      .replaceFirst("\\)$", "\n)")
      .replaceAll(",", ",\n")

    val scanner = new Scanner(body)
    val map = new util.HashMap[Integer, String]()
    while (scanner.hasNextLine) {
      map.put(map.size(), scanner.nextLine().trim)
    }
    val sqlBuffer = new StringBuffer(sql.substring(0, sql.indexOf("(")))
    var skipNo: Int = -1
    map.foreach(a => {
      if (a._1 > skipNo) {
        val length = lengthJoin(map, a._1, a._2)
        if (length._1 > a._1) {
          sqlBuffer.append(length._2).append("\n")
          skipNo = length._1
        } else {
          val comment = commentJoin(map, a._1, a._2)
          if (comment._1 > a._1) {
            sqlBuffer.append(comment._2).append("\n")
            skipNo = comment._1
          } else {
            sqlBuffer.append(a._2).append("\n")
          }
        }
      }
    })
    scanner.close()
    sqlBuffer.toString.trim.concat(sql.substring(sql.lastIndexOf(")") + 1))

  }

  /**
   * @param sql
   *   : original sql statement to be converted
   * @param typeFunc
   *   : type conversion function
   * @param keyFunc
   *   : handle primary key function
   * @param postfix
   *   : suffix content
   * @return
   */
  private[this] def convertSql(
      sql: String,
      typeFunc: (String, String) => String = null,
      keyFunc: String => String = null,
      postfix: String = null): String = {

    val formattedSql = formatSql(sql)
    val scanner = new Scanner(formattedSql)
    val sqlBuffer = new StringBuffer()
    while (scanner.hasNextLine) {
      val line = {
        val line = scanner.nextLine().trim
        line.toUpperCase().trim match {
          case a if a.startsWith("CREATE ") => line
          case a if a.startsWith("PRIMARY KEY ") =>
            keyFunc match {
              case null => null
              case _ => keyFunc(line)
            }
          case a if !a.startsWith("UNIQUE KEY ") && !a.startsWith("KEY ") =>
            val matcher = FIELD_REGEXP.matcher(line)
            if (!matcher.find()) null;
            else {
              val fieldName = matcher.group(1)
              val dataType = (matcher.group(2), matcher.group(3)) match {
                case (_, b) if b != null => b
                case (a, _) => a
              }
              val length = matcher.group(4)
              if (dataType == null) null;
              else {
                val fieldType = typeFunc(dataType, length)
                matcher.group(8) match {
                  case x if x != null => s"$fieldName $fieldType COMMENT '${matcher.group(10)}'"
                  case _ => s"$fieldName $fieldType"
                }
              }
            }
          case _ => null
        }
      }
      if (line != null) {
        sqlBuffer
          .append(line)
          .append(if (line.toUpperCase.trim.startsWith("CREATE ")) "\n" else ",\n")
      }
    }
    scanner.close()
    sqlBuffer.toString.trim.replaceAll(",$", "\n)") match {
      case x if postfix != null => s"$x $postfix"
      case x => x
    }
  }

  def mysqlToFlinkSql(sql: String, postfix: String): String = convertSql(
    sql,
    toFlinkDataType,
    x => {
      val matcher = PRIMARY_REGEXP.matcher(x)
      matcher.find() match {
        case b if b => s"${matcher.group()} NOT ENFORCED"
        case _ => null
      }
    },
    postfix)

  def mysqlToClickhouse(sql: String, postfix: String): String =
    convertSql(sql, toClickhouseDataType, postfix = postfix)

}
