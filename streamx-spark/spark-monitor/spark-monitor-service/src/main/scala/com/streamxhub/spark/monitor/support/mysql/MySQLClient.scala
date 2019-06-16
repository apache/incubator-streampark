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

package com.streamxhub.spark.monitor.support.mysql

import java.sql.Connection

import com.alibaba.druid.pool.DruidDataSource
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import com.streamxhub.spark.monitor.util.CommonUtils._

object MySQLClient extends Serializable {

  val dataSourceMap = new collection.mutable.HashMap[String, DruidDataSource]

  val dataSourceConfig: Config = ConfigFactory.load().getConfig("jdbc")

  /**
    *
    * 用于执行 INSERT、UPDATE 或 DELETE 语句以及 SQL DDL（数据定义语言）语句，例如 CREATE TABLE 和 DROP TABLE。
    * INSERT、UPDATE 或 DELETE 语句的效果是修改表中零行或多行中的一列或多列。
    * executeUpdate 的返回值是一个整数（int），指示受影响的行数（即更新计数）。
    * 对于 CREATE TABLE 或 DROP TABLE 等不操作行的语句，executeUpdate 的返回值总为零
    *
    * @param sql
    * @param db
    * @return
    */
  def executeUpdate(sql: String, db: String = ""): Int = {
    val conn = getConnection(db)
    try {
      val stmt = conn.createStatement
      stmt.executeUpdate(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        -1
    } finally {
      conn.close()
    }
  }

  def executeBatch(sql: Iterable[String], db: String = ""): Int = {
    Try(sql.size).getOrElse(0) match {
      case 0 => 0
      case 1 => executeUpdate(sql.head, db)
      case _ =>
        val conn = getConnection(db)
        conn.setAutoCommit(false)
        val ps = conn.prepareStatement("select 1")
        var index: Int = 0
        var count = 0
        try {
          sql.foreach(x => {
            ps.addBatch(x)
            index += 1
            if (index > 0 && index % 1000 == 0) {
              count += ps.executeBatch().sum
              conn.commit()
              ps.clearBatch()
            }
          })
          count += ps.executeBatch().sum
          conn.commit()
          conn.setAutoCommit(true)
          count
        } catch {
          case ex: Exception => ex.printStackTrace()
            0
        } finally {
          conn.close()
        }
    }
  }

  def select(sql: String, db: String = ""): List[Map[String, Any]] = {
    if (isEmpty(sql)) List.empty else {
      val conn = getConnection(db)
      try {
        val stmt = conn.createStatement
        val result = stmt.executeQuery(sql)
        val count = result.getMetaData.getColumnCount
        val array = ArrayBuffer[Map[String, Any]]()
        while (result.next()) {
          var map = Map[String, Any]()
          for (x <- 1 to count) {
            val key = result.getMetaData.getColumnLabel(x)
            val value = result.getObject(x).asInstanceOf[Any]
            map += key -> value
          }
          array += map
        }
        if (array.isEmpty) List.empty else array.toList
      } catch {
        case ex: Exception => ex.printStackTrace()
          List.empty
      } finally {
        conn.close()
      }
    }
  }

  def getConnection(db: String = ""): Connection = {
    val ds = dataSourceMap.get(db) match {
      case None =>
        val ds = new DruidDataSource
        /**
          * 实例配置
          * 如果未指定实例,则默认取db下的default配置的实例,
          * 如果指定了实例,则取指定实例的MySQL
          */
        val _db = dataSourceConfig.getConfig("db")
        val _name = if (db == "") _db.getString("default") else db
        val dbConfig = _db.getConfig(_name)

        ds.setUrl(dbConfig.getString("url"))
        ds.setUsername(dbConfig.getString("user"))
        ds.setPassword(dbConfig.getString("password"))

        /**
          * 通用配置
          */
        val config = dataSourceConfig.getConfig("config")
        ds.setDriverClassName(config.getString("driver"))
        ds.setMaxActive(config.getInt("maxActive"))
        ds.setMinIdle(config.getInt("minIdle"))
        ds.setInitialSize(config.getInt("initialSize"))
        ds.setMaxWait(config.getLong("maxWait"))
        ds.setTimeBetweenEvictionRunsMillis(config.getLong("timeBetweenEvictionRunsMillis"))
        ds.setMinEvictableIdleTimeMillis(config.getLong("minEvictableIdleTimeMillis"))
        ds.setValidationQuery(config.getString("validationQuery"))
        ds.setTestWhileIdle(config.getBoolean("testWhileIdle"))
        ds.setTestOnBorrow(config.getBoolean("testOnBorrow"))
        ds.setTestOnReturn(config.getBoolean("testOnReturn"))
        ds.setPoolPreparedStatements(config.getBoolean("poolPreparedStatements"))
        ds.setMaxPoolPreparedStatementPerConnectionSize(config.getInt("maxPoolPreparedStatementPerConnectionSize"))
        ds.init()
        dataSourceMap.put(db, ds)
        ds
      case x => x.get
    }
    ds.getConnection
  }

  /**
    * 查询返回一行记录...
    *
    * @param sql
    * @param db
    * @return
    */
  def selectOne(sql: String, db: String = ""): Map[String, Any] = {
    val conn = getConnection(db)
    try {
      val stmt = conn.createStatement
      val result = stmt.executeQuery(sql)
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
      conn.close()
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
    * @param db
    * @return
    */
  def execute(sql: String, db: String = ""): Boolean = {
    val conn = getConnection(db)
    try {
      val stmt = conn.createStatement
      stmt.execute(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        false
    } finally {
      conn.close()
    }
  }
}
