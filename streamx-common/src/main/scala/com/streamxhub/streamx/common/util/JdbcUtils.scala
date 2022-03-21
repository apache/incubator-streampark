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

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import java.sql.{Connection, ResultSet, Statement}
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
 * @author benjobs
 *         基于hikari连接池实现.呃,当然支持多数据源,需注意所有的修改和添加操作都是自动提交事物的...
 */
object JdbcUtils {


  private val lockMap: mutable.Map[String, ReentrantLock] = new ConcurrentHashMap[String, ReentrantLock]

  private[this] val dataSourceHolder = new ConcurrentHashMap[String, HikariDataSource]

  /**
   * 将查询的一行数据的所有字段封装到Map里,返回List。。。
   *
   * @param sql
   * @param jdbcConfig
   * @return
   */
  def select(sql: String, func: ResultSet => Unit = null)(implicit jdbcConfig: Properties): List[Map[String, _]] = {
    if (Try(sql.isEmpty).getOrElse(false)) List.empty else {
      val conn = getConnection(jdbcConfig)
      var stmt: Statement = null
      var result: ResultSet = null
      try {
        stmt = createStatement(conn)
        result = stmt.executeQuery(sql)
        if (func != null) {
          func(result)
        }
        val count = result.getMetaData.getColumnCount
        val array = ArrayBuffer[Map[String, Any]]()
        while (result.next()) {
          var map = Map[String, Any]()
          for (x <- 1 to count) {
            val key = result.getMetaData.getColumnLabel(x)
            result.getMetaData.getColumnType(x)
            val value = result.getObject(x)
            map += key -> value
          }
          array += map
        }
        if (array.isEmpty) List.empty else array.toList
      } catch {
        case ex: Exception => ex.printStackTrace()
          List.empty
      } finally {
        close(result, stmt, conn)
      }
    }
  }

  def count(sql: String)(implicit jdbcConfig: Properties): Long = unique(sql).head._2.toString.toLong

  def count(conn: Connection, sql: String): Long = unique(conn, sql).head._2.toString.toLong

  def batch(sql: Iterable[String])(implicit jdbcConfig: Properties): Int = {
    var conn: Connection = null
    Try(sql.size).getOrElse(0) match {
      case 0 => 0
      case 1 => update(sql.head)
      case _ =>
        conn = getConnection(jdbcConfig)
        val prepStat = conn.createStatement()
        try {
          var index: Int = 0
          val batchSize = 1000
          sql.map(x => {
            prepStat.addBatch(x)
            index += 1
            if (index > 0 && index % batchSize == 0) {
              val count = prepStat.executeBatch().sum
              conn.commit()
              prepStat.clearBatch()
              count
            } else 0
          }).sum + prepStat.executeBatch().sum
        } catch {
          case ex: Exception => ex.printStackTrace()
            0
        } finally {
          conn.commit()
          close(conn)
        }
    }
  }

  def update(sql: String)(implicit jdbcConfig: Properties): Int = update(getConnection(jdbcConfig), sql)

  /**
   *
   * 用于执行 INSERT、UPDATE 或 DELETE 语句以及 SQL DDL（数据定义语言）语句，例如 CREATE TABLE 和 DROP TABLE。
   * INSERT、UPDATE 或 DELETE 语句的效果是修改表中零行或多行中的一列或多列。
   * executeUpdate 的返回值是一个整数（int），指示受影响的行数（即更新计数）。
   * 对于 CREATE TABLE 或 DROP TABLE 等不操作行的语句，executeUpdate 的返回值总为零
   *
   * @param sql
   * @return
   */
  def update(conn: Connection, sql: String): Int = {
    var statement: Statement = null
    try {
      statement = conn.createStatement
      statement.executeUpdate(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        -1
    } finally {
      close(statement, conn)
    }
  }

  def unique(sql: String)(implicit jdbcConfig: Properties): Map[String, _] = unique(getConnection(jdbcConfig), sql)

  /**
   * 查询返回一行记录...
   *
   * @param sql
   * @return
   */
  def unique(conn: Connection, sql: String): Map[String, _] = {
    var stmt: Statement = null
    var result: ResultSet = null
    try {
      stmt = createStatement(conn)
      result = stmt.executeQuery(sql)
      val count = result.getMetaData.getColumnCount
      if (!result.next()) Map.empty else {
        var map = Map[String, Any]()
        for (x <- 1 to count) {
          val key = result.getMetaData.getColumnLabel(x)
          val value = result.getObject(x).asInstanceOf[Any]
          map += key -> value
        }
        map
      }
    } catch {
      case ex: Exception => ex.printStackTrace()
        Map.empty
    } finally {
      close(result, stmt, conn)
    }
  }

  /**
   *
   * 方法execute：
   * 可用于执行任何SQL语句，返回一个boolean值，表明执行该SQL语句是否返回了ResultSet。
   * 如果执行后第一个结果是ResultSet，则返回true，否则返回false。
   * 但它执行SQL语句时比较麻烦，通常我们没有必要使用execute方法来执行SQL语句，而是使用executeQuery或executeUpdate更适合。
   * 但如果在不清楚SQL语句的类型时则只能使用execute方法来执行该SQL语句了
   *
   * @param sql
   * @return
   */

  def execute(sql: String)(implicit jdbcConfig: Properties): Boolean = execute(getConnection(jdbcConfig), sql)

  def execute(conn: Connection, sql: String): Boolean = {
    var stmt: Statement = null
    try {
      stmt = conn.createStatement
      stmt.execute(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        false
    } finally {
      close(stmt, conn)
    }
  }

  /**
   * 以及Hikari连接池
   *
   * @param prop
   * @return
   */
  def getConnection(prop: Properties): Connection = {
    val alias = prop(KEY_ALIAS)
    val lock = lockMap.getOrElseUpdate(alias, new ReentrantLock())
    try {
      lock.lock()
      val ds: HikariDataSource = Try(Option(dataSourceHolder(alias))).getOrElse(None) match {
        case None =>
          // 创建一个数据源对象
          val jdbcConfig = new HikariConfig()
          prop.filter(x => x._1 != KEY_ALIAS && x._1 != KEY_SEMANTIC).foreach(x => {
            Try(Option(jdbcConfig.getClass.getDeclaredField(x._1))).getOrElse(None) match {
              case Some(field) =>
                field.setAccessible(true)
                field.getType.getSimpleName match {
                  case "String" => field.set(jdbcConfig, x._2.asInstanceOf[Object])
                  case "int" => field.set(jdbcConfig, x._2.toInt.asInstanceOf[Object])
                  case "long" => field.set(jdbcConfig, x._2.toLong.asInstanceOf[Object])
                  case "boolean" => field.set(jdbcConfig, x._2.toBoolean.asInstanceOf[Object])
                  case _ =>
                }
              case None =>
                val setMethod = s"set${x._1.substring(0, 1).toUpperCase}${x._1.substring(1)}"
                val method = Try(jdbcConfig.getClass.getMethods.filter(_.getName == setMethod).filter(_.getParameterCount == 1).head).getOrElse(null)
                method match {
                  case m if m != null =>
                    m.setAccessible(true)
                    m.getParameterTypes.head.getSimpleName match {
                      case "String" => m.invoke(jdbcConfig, Seq(x._2.asInstanceOf[Object]): _*)
                      case "int" => m.invoke(jdbcConfig, Seq(x._2.toInt.asInstanceOf[Object]): _*)
                      case "long" => m.invoke(jdbcConfig, Seq(x._2.toLong.asInstanceOf[Object]): _*)
                      case "boolean" => m.invoke(jdbcConfig, Seq(x._2.toBoolean.asInstanceOf[Object]): _*)
                      case _ =>
                    }
                  case null =>
                    throw new IllegalArgumentException(s"jdbcConfig error,property:${x._1} invalid,please see more properties jdbcConfig https://github.com/brettwooldridge/HikariCP")
                }
            }
          })
          val ds = new HikariDataSource(jdbcConfig)
          dataSourceHolder += alias -> ds
          ds
        case Some(x) => x
      }
      // 返回连接...
      ds.getConnection()
    } finally {
      lock.unlock()
    }
  }

  private[this] def createStatement(conn: Connection): Statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  def close(closeable: AutoCloseable*): Unit = Try(closeable.filter(x => x != null).foreach(_.close()))

}
