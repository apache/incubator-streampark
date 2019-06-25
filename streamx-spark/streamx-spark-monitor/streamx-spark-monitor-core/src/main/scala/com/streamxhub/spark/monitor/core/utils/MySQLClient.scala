package com.streamxhub.spark.monitor.core.utils

import java.sql.Connection

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import com.streamxhub.spark.monitor.core.utils.CommonUtils._
import com.zaxxer.hikari.HikariDataSource

import scala.reflect.ClassTag

object MySQLClient extends Serializable {

  val connPool = new ThreadLocal[Connection]

  var ds: HikariDataSource = _

  val dataSourceConfig: Config = ConfigFactory.load().getConfig("mysql")

  def getConnection: Connection = {
    connPool.get match {
      case null =>
        ds match {
          case null =>
            ds = new HikariDataSource
            ds.setJdbcUrl(dataSourceConfig.getString("url"))
            ds.setUsername(dataSourceConfig.getString("user"))
            ds.setPassword(dataSourceConfig.getString("password"))
            ds.setDriverClassName(dataSourceConfig.getString("driver"))
            ds.setReadOnly(dataSourceConfig.getBoolean("readOnly"))
            // 等待连接池分配连接的最大时长（毫秒），超过这个时长还没可用的连接则发生SQLException， 缺省:30秒 -->
            ds.setConnectionTimeout(dataSourceConfig.getInt("connectionTimeout"))
            // 一个连接idle状态的最大时长（毫秒），超时则被释放（retired），缺省:10分钟 -->
            ds.setIdleTimeout(dataSourceConfig.getInt("idleTimeout"))
            // 一个连接的生命时长（毫秒），超时而且没被使用则被释放（retired），缺省:30分钟，建议设置比数据库超时时长少30秒，参考MySQL wait_timeout参数（show variables like '%timeout%';） -->
            ds.setMaxLifetime(dataSourceConfig.getLong("maxLifetime"))
            // 连接池中允许的最大连接数。缺省值：10；推荐的公式：((core_count * 2) + effective_spindle_count) -->
            ds.setMinimumIdle(dataSourceConfig.getInt("maximumPoolSize"))
          case _ =>
        }
        connPool.set(ds.getConnection)
        connPool.get()
      case conn => conn
    }
  }

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
  def executeUpdate(sql: String): Int = {
    val conn = getConnection
    try {
      val stmt = conn.createStatement
      stmt.executeUpdate(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        -1
    } finally {
      conn.close()
      connPool.remove()
    }
  }

  def executeBatch(sql: Iterable[String]): Int = {
    Try(sql.size).getOrElse(0) match {
      case 0 => 0
      case 1 => executeUpdate(sql.head)
      case _ =>
        val conn = getConnection
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
          connPool.remove()
        }
    }
  }

  def select[T: ClassTag](sql: String): List[T] = {
    val empty = List[T]()
    if (isEmpty(sql)) empty else {
      val conn = getConnection
      try {
        val stmt = conn.createStatement
        val result = stmt.executeQuery(sql)
        val count = result.getMetaData.getColumnCount
        val array = ArrayBuffer[T]()
        val myClassOf = implicitly[ClassTag[T]].runtimeClass
        val fields = myClassOf.getDeclaredFields
        val separator = "_|-"
        while (result.next()) {
          val obj = myClassOf.newInstance()
          for (x <- 1 to count) {
            val k = result.getMetaData.getColumnLabel(x)
            fields.filter(f => f.getName.replaceAll(separator, "").toUpperCase() == k.replaceAll(separator, "").toUpperCase()).foreach(f => {
              f.setAccessible(true)
              val v = result.getObject(x)
              f.set(obj, v)
            })
          }
          array += obj.asInstanceOf[T]
        }
        if (array.isEmpty) empty else array.toList
      } catch {
        case ex: Exception => ex.printStackTrace()
          empty
      } finally {
        conn.close()
        connPool.remove()
      }
    }
  }


  def select(sql: String): List[Map[String, Any]] = {
    if (isEmpty(sql)) List.empty else {
      val conn = getConnection
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
        connPool.remove()
      }
    }
  }

  /**
    * 查询返回一行记录...
    *
    * @param sql
    * @return
    */
  def selectOne[T: ClassTag](sql: String): T = {
    val conn = getConnection
    try {
      val stmt = conn.createStatement
      val result = stmt.executeQuery(sql)
      val count = result.getMetaData.getColumnCount
      if (!result.next()) null.asInstanceOf[T] else {
        val myClassOf = implicitly[ClassTag[T]].runtimeClass
        val obj = myClassOf.newInstance()
        val fields = myClassOf.getDeclaredFields
        val separator = "_|-"
        for (x <- 1 to count) {
          val k = result.getMetaData.getColumnLabel(x)
          val v = result.getObject(x)
          fields.filter(f => f.getName.replaceAll(separator, "").toUpperCase() == k.replaceAll(separator, "").toUpperCase() && v != null).foreach(f => {
            f.setAccessible(true)
            f.set(obj, v)
          })
        }
        obj.asInstanceOf[T]
      }
    } catch {
      case ex: Exception => ex.printStackTrace()
        null.asInstanceOf[T]
    } finally {
      conn.close()
      connPool.remove()
    }
  }

  /**
    * 查询返回一行记录...
    *
    * @param sql
    * @return
    */
  def selectOne(sql: String): Map[String, Any] = {
    val conn = getConnection
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
      connPool.remove()
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
  def execute(sql: String): Boolean = {
    val conn = getConnection
    try {
      val stmt = conn.createStatement
      stmt.execute(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        false
    } finally {
      conn.close()
      connPool.remove()
    }
  }
}
