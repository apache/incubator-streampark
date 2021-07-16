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

import java.util.Scanner
import java.util.regex._
import scala.util.Try

object SqlConvertUtils {

  private[this] val FIELD_PATTERN = Pattern.compile("\\s+([a-zA-Z]+\\((\\d+|\\d+,\\d+)\\)|[a-zA-Z]+)((\\s+|)|((.*)(COMMENT)\\s+('(.*)'|\"(.*)\")|(.*))),$", Pattern.CASE_INSENSITIVE)

  private[this] val PRIMARY_PATTERN = Pattern.compile("PRIMARY\\s+KEY\\s+\\((.*)\\)", Pattern.CASE_INSENSITIVE)

  /**
   * 转成Flink里的数据类型
   *
   * @param dataType
   * @param length
   * @return
   */
  def toFlinkDataType(dataType: String, length: String): String = {
    dataType.toUpperCase() match {
      case "TEXT" | "LONGTEXT" => "VARCHAR"
      case "DATETIME" => "TIMESTAMP"
      case x if x.startsWith("BIGINT") => "BIGINT"
      case x if x.startsWith("TINYINT") => "TINYINT"
      case x if x.startsWith("SMALLINT") => "SMALLINT"
      case x if x.startsWith("INT") => "INTEGER"
      case x if x.startsWith("VARCHAR") => "VARCHAR"
      case x if x.startsWith("DOUBLE") => "DOUBLE"
      case x => x.toUpperCase()
    }
  }

  /**
   * 其他数据库类型转换为clickHouse类型,
   *
   * @param dataType
   * @param length
   * @return
   */
  def toClickhouseDataType(dataType: String, length: String): String = {
    dataType.toUpperCase() match {
      case x if x.startsWith("TEXT")
        || x.startsWith("LONGTEXT")
        || x.startsWith("BLOB")
        || x.startsWith("TEXT")
        || x.startsWith("VARCHAR")
        || x.startsWith("VARBINARY") => "String"
      case "DATETIME" | "TIMESTAMP" => "DateTime"
      case x if x.startsWith("BIGINT") || x.startsWith("INT") =>
        Try(length.toInt).getOrElse(11) match {
          case x if x < 3 => "Int8"
          case x if x < 5 => "Int16"
          case x if x < 9 => "Int32"
          case _ => "Int64"
        }
      case x if x.startsWith("TINYINT") => "Int8"
      case x if x.startsWith("SMALLINT") => "Int16"
      case x if x.startsWith("FLOAT") => "Float32"
      case x if x.startsWith("DOUBLE") => "Float64"
      case x if x.startsWith("DATE") => "Date"
      case x if x.startsWith("DECIMAL") =>
        length.split(",").map(_.toInt) match {
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

  def convertSql(sql: String, func: (String, String) => String = null, keyFunc: String => String = null, postfix: String = null): String = {
    val scanner = new Scanner(sql)
    val sqlBuffer = new StringBuffer()
    var index: Int = 0
    while (scanner.hasNextLine) {
      val line = {
        val line = scanner.nextLine().trim
        line.toUpperCase().trim match {
          case a if a.startsWith("CREATE ") => line
          case a if a.startsWith("PRIMARY KEY") =>
            keyFunc match {
              case null => null
              case _ => keyFunc(line)
            }
          case a if !a.startsWith("UNIQUE KEY") && !a.startsWith("KEY") =>
            val matcher = FIELD_PATTERN.matcher(line)
            if (!matcher.find) null; else {
              val dataType = matcher.group(1)
              val length = matcher.group(2)
              if (dataType == null) null; else {
                val comment = matcher.group(7) match {
                  case x if x != null =>
                    (matcher.group(9), matcher.group(10)) match {
                      case (a, _) if a != null => a
                      case (_, b) => b
                    }
                  case _ => null
                }
                s"${line.trim.split("\\s+").head} ${func(dataType, length)} ${if (comment != null) s"COMMENT '$comment'" else ""}".trim
              }
            }
          case _ => null
        }
      }
      if (line != null) {
        sqlBuffer.append(line).append(if (index == 0) "\n" else ",\n")
        index += 1
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
      val matcher = PRIMARY_PATTERN.matcher(x)
      if (!matcher.find()) null; else {
        val primary = matcher.group()
        primary.concat(" NOT ENFORCED")
      }
    },
    postfix
  )

  def mysqlToClickhouse(sql: String, postfix: String): String = convertSql(sql, toClickhouseDataType, postfix = postfix)

  def main(args: Array[String]): Unit = {
    val sql =
      """
        |CREATE TABLE `t_flink_app` (
        |`ID` bigint NOT NULL AUTO_INCREMENT,
        |`JOB_TYPE` tinyint DEFAULT NULL,
        |`EXECUTION_MODE` tinyint DEFAULT NULL,
        |`PROJECT_ID` varchar(64) DEFAULT NULL,
        |`JOB_NAME` varchar(255) DEFAULT NULL,
        |`MODULE` varchar(255) DEFAULT NULL,
        |`JAR` varchar(255) DEFAULT NULL,
        |`MAIN_CLASS` varchar(255) DEFAULT NULL,
        |`ARGS` text DEFAULT NULL,
        |`OPTIONS` text DEFAULT NULL,
        |`USER_ID` bigint DEFAULT NULL,
        |`APP_ID` varchar(255) DEFAULT NULL,
        |`APP_TYPE` tinyint DEFAULT NULL,
        |`DURATION` bigint DEFAULT NULL,
        |`JOB_ID` varchar(64) DEFAULT NULL,
        |`STATE` varchar(50) DEFAULT NULL,
        |`RESTART_SIZE` int DEFAULT NULL,
        |`RESTART_COUNT` int DEFAULT NULL,
        |`CP_THRESHOLD` int DEFAULT NULL,
        |`CP_MAX_FAILURE_INTERVAL` int NULL,
        |`CP_FAILURE_RATE_INTERVAL` int NULL,
        |`CP_FAILURE_ACTION` tinyint NULL,
        |`DYNAMIC_OPTIONS` text DEFAULT NULL,
        |`DESCRIPTION` varchar(255) DEFAULT NULL,
        |`RESOLVE_ORDER` tinyint DEFAULT NULL,
        |`FLAME_GRAPH` tinyint DEFAULT '0',
        |`JM_MEMORY` int DEFAULT NULL,
        |`TM_MEMORY` int DEFAULT NULL,
        |`TOTAL_TASK` int DEFAULT NULL,
        |`TOTAL_TM` int DEFAULT NULL,
        |`TOTAL_SLOT` int DEFAULT NULL,
        |`AVAILABLE_SLOT` int DEFAULT NULL,
        |`OPTION_STATE` tinyint DEFAULT NULL,
        |`TRACKING` tinyint DEFAULT NULL,
        |`CREATE_TIME` datetime DEFAULT NULL,
        |`DEPLOY` tinyint DEFAULT '0',
        |`START_TIME` datetime DEFAULT NULL,
        |`END_TIME` datetime DEFAULT NULL,
        |`ALERT_EMAIL` varchar(255) DEFAULT NULL,
        |PRIMARY KEY (`ID`) USING BTREE,
        |KEY `INX_STATE` (`STATE`) USING BTREE,
        |KEY `INX_JOB_TYPE` (`JOB_TYPE`) USING BTREE,
        |KEY `INX_TRACK` (`TRACKING`) USING BTREE
        |) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
        |""".stripMargin

    val flinkSql = mysqlToFlinkSql(
      sql,
      """WITH (
        |'connector' = 'upsert-kafka',
        |'topic' = 'test_topic',
        |'properties.bootstrap.servers' = '172.0.0.1:9092,172.0.0.2:9092,172.0.0.3:9092',
        |'key.format' = 'json',
        |'value.format' = 'json'
        |);
        |""".stripMargin
    )
    println(flinkSql)

    println("-------------------------------------------------")

    val ckSql = mysqlToClickhouse(
      sql,
      """
        |ENGINE = MergeTree
        |ORDER BY id
        |SETTINGS index_granularity = 8192
        |""".stripMargin
    )

    println(ckSql)

  }

}
