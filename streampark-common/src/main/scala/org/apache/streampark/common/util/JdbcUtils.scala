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

import org.apache.streampark.common.conf.ConfigKeys._
import org.apache.streampark.common.util.Implicits._

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import java.sql.{Connection, ResultSet, Statement}
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
 * Based on the hikari connection pool implementation. Support multiple data sources, note that all
 * modifications and additions are automatically committed transactions.
 */
object JdbcUtils {

  private val lockMap: mutable.Map[String, ReentrantLock] =
    new ConcurrentHashMap[String, ReentrantLock]

  private[this] val dataSourceHolder =
    new ConcurrentHashMap[String, HikariDataSource]

  /** Wrap all the fields of a query row of data into a List[Map] */
  def select(sql: String, func: ResultSet => Unit = null)(implicit jdbcConfig: Properties): List[Map[String, _]] = {
    if (Try(sql.isEmpty).getOrElse(false)) List.empty
    else {
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
        case ex: Exception =>
          ex.printStackTrace()
          List.empty
      } finally {
        close(result, stmt, conn)
      }
    }
  }

  def count(sql: String)(implicit jdbcConfig: Properties): Long = unique(
    sql).head._2.toString.toLong

  def count(conn: Connection, sql: String): Long =
    unique(conn, sql).head._2.toString.toLong

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
          sql
            .map(x => {
              prepStat.addBatch(x)
              index += 1
              if (index > 0 && index % batchSize == 0) {
                val count = prepStat.executeBatch().sum
                conn.commit()
                prepStat.clearBatch()
                count
              } else 0
            })
            .sum + prepStat.executeBatch().sum
        } catch {
          case ex: Exception =>
            ex.printStackTrace()
            0
        } finally {
          conn.commit()
          close(conn)
        }
    }
  }

  def update(sql: String)(implicit jdbcConfig: Properties): Int =
    update(getConnection(jdbcConfig), sql)

  /**
   * Used to execute INSERT, UPDATE, or DELETE statements and SQL DDL statements, such as CREATE
   * TABLE and DROP TABLE. The effect of an INSERT, UPDATE, or DELETE statement is to modify one or
   * more columns in zero or more rows of a table. The return value of executeUpdate is an integer
   * (int) indicating the number of rows affected (i.e., the update count). For statements such as
   * CREATE TABLE or DROP TABLE that do not operate on rows, the return value of executeUpdate is
   * always zero
   */
  def update(conn: Connection, sql: String): Int = {
    var statement: Statement = null
    try {
      statement = conn.createStatement
      statement.executeUpdate(sql)
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        -1
    } finally {
      close(statement, conn)
    }
  }

  def unique(sql: String)(implicit jdbcConfig: Properties): Map[String, _] =
    unique(getConnection(jdbcConfig), sql)

  /** The query returns one row of records */
  def unique(conn: Connection, sql: String): Map[String, _] = {
    var stmt: Statement = null
    var result: ResultSet = null
    try {
      stmt = createStatement(conn)
      result = stmt.executeQuery(sql)
      val count = result.getMetaData.getColumnCount
      if (!result.next()) Map.empty
      else {
        var map = Map[String, Any]()
        for (x <- 1 to count) {
          val key = result.getMetaData.getColumnLabel(x)
          val value = result.getObject(x).asInstanceOf[Any]
          map += key -> value
        }
        map
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Map.empty
    } finally {
      close(result, stmt, conn)
    }
  }

  /**
   * Note about execute: It can be used to execute any SQL statement, returns a boolean value
   * indicating whether the execution of the SQL statement returned a ResultSet. Returns true if the
   * first result after execution is a ResultSet, otherwise it returns false. However, it is
   * cumbersome to execute SQL statements. Usually we don't need to use the execute method to
   * execute SQL statements,but it is more appropriate to use executeQuery or executeUpdate. But if
   * you don't know the type of the SQL statement,you can only use the execute method to execute the
   * SQL statement.
   */
  def execute(sql: String)(implicit jdbcConfig: Properties): Boolean =
    execute(getConnection(jdbcConfig), sql)

  def execute(conn: Connection, sql: String): Boolean = {
    var stmt: Statement = null
    try {
      stmt = conn.createStatement
      stmt.execute(sql)
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        false
    } finally {
      close(stmt, conn)
    }
  }

  /** Get connection with HikariDataSource */
  def getConnection(prop: Properties): Connection = {
    val alias = prop(KEY_ALIAS)
    val lock = lockMap.getOrElseUpdate(alias, new ReentrantLock())
    try {
      lock.lock()
      val ds: HikariDataSource =
        Try(Option(dataSourceHolder(alias))).getOrElse(None) match {
          case None =>
            val jdbcConfig = new HikariConfig()
            prop
              .filter(x => x._1 != KEY_ALIAS && x._1 != KEY_SEMANTIC)
              .foreach(x => {
                Try(Option(jdbcConfig.getClass.getDeclaredField(x._1)))
                  .getOrElse(None) match {
                  case Some(field) =>
                    field.setAccessible(true)
                    field.getType.getSimpleName match {
                      case "String" =>
                        field.set(jdbcConfig, x._2.asInstanceOf[Object])
                      case "int" =>
                        field.set(jdbcConfig, x._2.toInt.asInstanceOf[Object])
                      case "long" =>
                        field
                          .set(jdbcConfig, x._2.toLong.asInstanceOf[Object])
                      case "boolean" =>
                        field.set(jdbcConfig, x._2.toBoolean.asInstanceOf[Object])
                      case _ =>
                    }
                  case None =>
                    val setMethod =
                      s"set${x._1.substring(0, 1).toUpperCase}${x._1.substring(1)}"
                    val method = Try(
                      jdbcConfig.getClass.getMethods
                        .filter(_.getName == setMethod)
                        .filter(_.getParameterCount == 1)
                        .head).getOrElse(null)
                    method match {
                      case m if m != null =>
                        m.setAccessible(true)
                        m.getParameterTypes.head.getSimpleName match {
                          case "String" =>
                            m.invoke(jdbcConfig, Seq(x._2.asInstanceOf[Object]): _*)
                          case "int" =>
                            m.invoke(jdbcConfig, Seq(x._2.toInt.asInstanceOf[Object]): _*)
                          case "long" =>
                            m.invoke(jdbcConfig, Seq(x._2.toLong.asInstanceOf[Object]): _*)
                          case "boolean" =>
                            m.invoke(jdbcConfig, Seq(x._2.toBoolean.asInstanceOf[Object]): _*)
                          case _ =>
                        }
                      case null =>
                        throw new IllegalArgumentException(
                          s"jdbcConfig error,property:${x._1} invalid,please see more properties jdbcConfig https://github.com/brettwooldridge/HikariCP")
                    }
                }
              })
            val ds = new HikariDataSource(jdbcConfig)
            dataSourceHolder += alias -> ds
            ds
          case Some(x) => x
        }
      ds.getConnection()
    } finally {
      lock.unlock()
    }
  }

  private[this] def createStatement(conn: Connection): Statement =
    conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  def close(closeable: AutoCloseable*): Unit = Try(
    closeable.filter(x => x != null).foreach(_.close()))

}
