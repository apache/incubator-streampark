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
import com.streamxhub.streamx.common.util.{ExceptionUtils, Logger, SqlSplitter}
import org.apache.calcite.config.Lex
import org.apache.calcite.sql.{SqlIdentifier, SqlJoin, SqlNode, SqlSelect}
import org.apache.calcite.sql.parser.SqlParser
import org.apache.flink.sql.parser.ddl.SqlTableColumn.SqlRegularColumn
import org.apache.flink.sql.parser.ddl.{SqlCreateTable, SqlCreateView, SqlTableColumn, SqlTableOption}
import org.apache.flink.sql.parser.dml.RichSqlInsert
import org.apache.flink.sql.parser.validate.FlinkSqlConformance
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.SqlDialect.{DEFAULT, HIVE}
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, TableException}
import org.apache.flink.table.planner.delegation.FlinkSqlParserFactories
import org.apache.flink.table.planner.parse.CalciteParser
import org.apache.flink.table.planner.utils.TableConfigUtils

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object FlinkSqlLineage extends Logger {


  private[this] lazy val parser = {
    val tableConfig = StreamTableEnvironment.create(
      StreamExecutionEnvironment.getExecutionEnvironment,
      EnvironmentSettings
        .newInstance
        .inStreamingMode
        .build
    ).getConfig

    val sqlParserConfig = TableConfigUtils.getCalciteConfig(tableConfig).getSqlParserConfig.getOrElse {
      val conformance = tableConfig.getSqlDialect match {
        case HIVE => FlinkSqlConformance.HIVE
        case DEFAULT => FlinkSqlConformance.DEFAULT
        case _ =>
          throw new TableException(
            SqlError(
              SqlErrorType.UNSUPPORTED_DIALECT,
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


  def  lineageSql(sql: String): Any = {
      val lines = SqlSplitter.splitSql(sql)
      var allTables: List[Map[String, Object]] = List()
      var outputTables: List[Map[String, Object]] = List()
      var inputTables: List[Map[String, Object]] = List()
      for (sqlLine <- lines) {
        val sqlNode = parser.parse(sqlLine)
        sqlNode match {
          case flinkTable: SqlCreateTable =>
            var properties: Map[String, String] = Map()
            val propertyList = flinkTable.getPropertyList().getList
            val it = propertyList.iterator()
            while (it.hasNext) {
              val tableOption = it.next().asInstanceOf[SqlTableOption]
              properties += (tableOption.getKeyString -> tableOption.getValueString)
            }
            var map: Map[String, Object] = Map()
            map += ("connector" -> properties("connector"))
            map += ("flinkTable" -> flinkTable.getTableName.toString)
            properties("connector") match {
              case "kafka" =>
                val topicArr = properties("topic").split(";")
                topicArr.foreach(topic => {
                  map += ("table" -> topic)
                })
              case connector if connector.startsWith("elasticsearch") =>
                map += ("connector" -> "elasticsearch")
                map += ("table" -> properties("index"))
              case "datahub" =>
                map += ("table" -> properties("topic"))
              case "datahubproxy" =>
                map += ("table" -> properties("dataId"))
              case "kafkaproxy" =>
                map += ("table" -> properties("topic"))
              case "jdbc" =>
                properties("url") match {
                  case url if url.startsWith("jdbc:clickhouse:") => map += ("connector" -> "clickhouse")
                  case url if url.startsWith("jdbc:mysql:") => map += ("connector" -> "mysql")
                  case url if url.startsWith("jdbc:postgresql:") => map += ("connector" -> "postgresql")
                  case url if url.startsWith("jdbc:oracle:") => map += ("connector" -> "oracle")
                  case _ => None
                }
                map += ("table" -> properties("table-name"))
              case _ =>
            }
            var columnList: List[Map[String, Object]] = List()
            flinkTable.getColumnList.getList.asScala.foreach(col => {
              col match {
                case column: SqlRegularColumn =>
                  var filedsMap: Map[String, Object] = Map()
                  filedsMap += ("name" -> column.getName.toString )
                  filedsMap += ("type" -> column.getType.getTypeName.toString)
                  columnList :+= filedsMap
                case _ =>
              }
            })
            map += ("fields" -> columnList)
            allTables :+= map;
          case table: SqlCreateView =>
            val tables = ListBuffer[String]()
            getTableNameFromSqlNode(table.getQuery, tables)
            allTables.foreach(map => {
              tables.foreach(table =>
                if (map("flinkTable").equals(table)) {
                  inputTables:+= map
                }
              )
            })
          case table: RichSqlInsert =>
            val tables = ListBuffer[String]()
            getTableNameFromSqlNode(table.getSource, tables)
            allTables.foreach(map => {
              if (map("flinkTable").equals(table.getTargetTable.toString)) {
                outputTables :+= map
              }
              tables.foreach(table =>
                if (map("flinkTable").equals(table)) {
                  inputTables :+= map
                }
              )
            }

        )
          case _ =>
        }
      }
      inputTables = inputTables.map(table => table - ("flinkTable"))
      outputTables = outputTables.map(table => table - ("flinkTable"))
      var result: Map[String, List[Map[String, Object]]] = Map()
      result += ("inputTables" -> inputTables)
      result += ("outputTables" -> outputTables)
      deepAsJava(result)
}
  def deepAsJava(x: Any): Any = x match {
    case l: List[_] => l.map(deepAsJava).asJava
    case m: Map[_, _] => m.map { case (k, v) => (k, deepAsJava(v)) }.asJava
    case x => x
  }
  def getTableNameFromSqlNode(sqlNode: SqlNode, tables: ListBuffer[String]) {
    sqlNode match {
      case sqlNode: SqlSelect =>
        sqlNode.getFrom match {
          case from: SqlIdentifier =>
            tables.append(from.toString)
          case from: SqlJoin =>
            getTableNameFromSqlNode(from.getLeft, tables)
            getTableNameFromSqlNode(from.getRight, tables)

        }
      case sqlNode: SqlIdentifier =>
        tables.append(sqlNode.toString)
    }
  }
}
