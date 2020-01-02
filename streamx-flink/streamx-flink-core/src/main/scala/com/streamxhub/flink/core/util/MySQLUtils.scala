package com.streamxhub.flink.core.util

import java.sql.{Connection, ResultSet, Statement}
import java.util
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

import com.streamxhub.flink.core.conf.ConfigConst._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.json4s.DefaultFormats

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy


object MySQLUtils {

  @transient
  implicit private lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  private val lockMap: mutable.Map[String, ReentrantLock] = new ConcurrentHashMap[String, ReentrantLock]

  private[this] val connectionPool = new ThreadLocal[ConcurrentHashMap[String, util.Queue[Connection]]]()

  private[this] val dataSourceHolder = new ConcurrentHashMap[String, HikariDataSource]

  /**
   * 将查询的一行数据的所有字段封装到Map里,返回List。。。
   *
   * @param sql
   * @param config
   * @return
   */
  def select(sql: String)(implicit config: Properties): List[Map[String, _]] = select(getConnection(config), sql)

  def select(conn: Connection, sql: String)(implicit config: Properties): List[Map[String, _]] = {
    if (Try(sql.isEmpty).getOrElse(false)) List.empty else {
      var stmt: Statement = null
      var result: ResultSet = null
      try {
        stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
        result = stmt.executeQuery(sql)
        val count = result.getMetaData.getColumnCount
        val array = ArrayBuffer[Map[String, Any]]()
        while (result.next()) {
          var map = Map[String, Any]()
          for (x <- 1 to count) {
            val key = result.getMetaData.getColumnLabel(x)
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
        close( conn, stmt, result)
      }
    }
  }

  /**
   * 直接查询一个对象
   *
   * @param sql
   * @param config
   * @tparam T
   * @return
   */
  def select2[T](sql: String)(implicit config: Properties,manifest: Manifest[T]): List[T] = toObject[T](select(sql))

  def select2[T](connection: Connection, sql: String)(implicit config: Properties, manifest: Manifest[T]): List[T] = toObject[T](select(connection, sql))

  private[this] def toObject[T](list: List[Map[String, _]])(implicit manifest: Manifest[T]): List[T] = if (list.isEmpty) List.empty else list.map(x => JsonUtils.read[T](JsonUtils.write(x)))

  def executeBatch(sql: Iterable[String])(implicit config: Properties): Int = {
    var conn: Connection = null
    Try(sql.size).getOrElse(0) match {
      case 0 => 0
      case 1 => update(sql.head)
      case _ =>
        conn = getConnection(config)
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
          count
        } catch {
          case ex: Exception => ex.printStackTrace()
            0
        } finally {
          close( conn, null, null)
        }
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
  def update(sql: String)(implicit config: Properties): Int = {
    val conn = getConnection(config)
    var statement: Statement = null
    try {
      statement = conn.createStatement
      statement.executeUpdate(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        -1
    } finally {
      close( conn, statement, null)
    }
  }

  def unique(sql: String)(implicit config: Properties): Map[String, _] = unique(getConnection(config), sql)

  /**
   * 查询返回一行记录...
   *
   * @param sql
   * @return
   */
  def unique(conn: Connection, sql: String)(implicit config: Properties): Map[String, _] = {
    var stmt: Statement = null
    var result: ResultSet = null
    try {
      stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
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
      close( conn, stmt, result)
    }
  }

  def unique2[T](sql: String)(implicit config: Properties,manifest: Manifest[T]): T = toObject[T](List(unique(sql))).head

  def unique2[T](connection: Connection, sql: String)(implicit config: Properties, manifest: Manifest[T]): T = toObject(List(unique(connection, sql))).head

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
  def execute(sql: String)(implicit config: Properties): Boolean = {
    val conn = getConnection(config)
    var stmt: Statement = null
    try {
      stmt = conn.createStatement
      stmt.execute(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        false
    } finally {
      close( conn, null, null)
    }
  }

  def getConnection(prop: Properties): Connection = {
    val instance = prop(KEY_MYSQL_INSTANCE)
    val lock = lockMap.getOrElseUpdate(instance, new ReentrantLock())
    try {
      lock.lock()
      //连接实例名称
      //尝试从连接池里获取一个连接...
      val conn = Try(connectionPool.get()(instance).poll()).getOrElse(null)
      if (conn != null) conn else {
        //尝试获取该实力的数据源对象
        val ds: HikariDataSource = Try(Option(dataSourceHolder(instance))).getOrElse(None) match {
          case None =>
            //创建一个数据源对象
            val config = new HikariConfig()
            prop.filter(_._1 != KEY_MYSQL_INSTANCE).foreach(x => {
              val field = Try(Option(config.getClass.getDeclaredField(x._1))).getOrElse(None) match {
                case None =>
                  val boolMethod = s"is${x._1.substring(0, 1).toUpperCase}${x._1.substring(1)}"
                  Try(Option(config.getClass.getDeclaredField(boolMethod))).getOrElse(None) match {
                    case Some(x) => x
                    case None => throw new IllegalArgumentException(s"config error,property:${x._1} invalid,please see more properties config https://github.com/brettwooldridge/HikariCP")
                  }
                case Some(x) => x
              }
              field.setAccessible(true)
              field.getType.getSimpleName match {
                case "String" => field.set(config, x._2)
                case "int" => field.set(config, x._2.toInt)
                case "long" => field.set(config, x._2.toLong)
                case "boolean" => field.set(config, x._2.toBoolean)
                case _ =>
              }
            })
            val ds = new HikariDataSource(config)
            dataSourceHolder += instance -> ds
            ds
          case Some(x) => x
        }
        //返回连接...
        val conn = ds.getConnection

        Proxy.newProxyInstance(conn.getClass.getClassLoader, conn.getClass.getInterfaces, new InvocationHandler {
          override def invoke(proxy: Any, method: Method, args: Array[AnyRef]): AnyRef = {
            method.getName match {
              case "close" =>
                val proxyConn = proxy.asInstanceOf[Connection]
                Option(connectionPool.get()) match {
                  case Some(x) =>
                    x.getOrElseUpdate(instance,new util.LinkedList[Connection]())
                    x(instance).add(proxyConn)
                  case _ =>
                    val list = new util.LinkedList[Connection]()
                    list.add(proxyConn)
                    val map = new ConcurrentHashMap[String, util.Queue[Connection]]()
                    map += instance -> list
                    connectionPool.set(map)
                }
                null
              case _ => method.invoke(conn, args: _*)
            }
          }
        }).asInstanceOf[Connection]

      }
    } finally {
      lock.unlock()
    }
  }

  def close(connection: Connection, statement: Statement, resultSet: ResultSet) = {
    if (resultSet != null) {
      resultSet.close()
    }
    if (statement != null) {
      statement.close()
    }
    if (connection != null) {
      connection.close()
    }
  }


}
