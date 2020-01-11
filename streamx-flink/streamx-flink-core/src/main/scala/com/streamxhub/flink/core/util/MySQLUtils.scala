package com.streamxhub.flink.core.util

import java.sql.{Connection, DriverManager, ResultSet, Statement}
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

object MySQLUtils {

  @transient
  implicit private lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  private val lockMap: mutable.Map[String, ReentrantLock] = new ConcurrentHashMap[String, ReentrantLock]

  private[this] val dataSourceHolder = new ConcurrentHashMap[String, HikariDataSource]

  /**
   * 将查询的一行数据的所有字段封装到Map里,返回List。。。
   *
   * @param sql
   * @param jdbcConfig
   * @return
   */
  def select(sql: String)(implicit jdbcConfig: Properties): List[Map[String, _]] = select(getConnection(jdbcConfig), sql)

  def select(conn: Connection, sql: String)(implicit jdbcConfig: Properties): List[Map[String, _]] = {
    if (Try(sql.isEmpty).getOrElse(false)) List.empty else {
      var stmt: Statement = null
      var result: ResultSet = null
      try {
        stmt = createStatement(conn)
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
        close(conn, stmt, result)
      }
    }
  }

  /**
   * 直接查询一个对象
   *
   * @param sql
   * @param jdbcConfig
   * @tparam T
   * @return
   */
  def select2[T](sql: String)(implicit jdbcConfig: Properties, manifest: Manifest[T]): List[T] = toObject[T](select(sql))

  def select2[T](connection: Connection, sql: String)(implicit jdbcConfig: Properties, manifest: Manifest[T]): List[T] = toObject[T](select(connection, sql))

  private[this] def toObject[T](list: List[Map[String, _]])(implicit manifest: Manifest[T]): List[T] = if (list.isEmpty) List.empty else list.map(x => JsonUtils.read[T](JsonUtils.write(x)))

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
  def update(sql: String)(implicit jdbcConfig: Properties): Int = {
    val conn = getConnection(jdbcConfig)
    var statement: Statement = null
    try {
      statement = conn.createStatement
      val index = statement.executeUpdate(sql)
      conn.commit()
      index
    } catch {
      case ex: Exception => ex.printStackTrace()
        -1
    } finally {
      close(conn, statement)
    }
  }

  def unique(sql: String)(implicit jdbcConfig: Properties): Map[String, _] = unique(getConnection(jdbcConfig), sql)

  /**
   * 查询返回一行记录...
   *
   * @param sql
   * @return
   */
  def unique(conn: Connection, sql: String)(implicit jdbcConfig: Properties): Map[String, _] = {
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
      close(conn, stmt, result)
    }
  }

  def unique2[T](sql: String)(implicit jdbcConfig: Properties, manifest: Manifest[T]): T = toObject[T](List(unique(sql))).head

  def unique2[T](connection: Connection, sql: String)(implicit jdbcConfig: Properties, manifest: Manifest[T]): T = toObject(List(unique(connection, sql))).head

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
  def execute(sql: String)(implicit jdbcConfig: Properties): Boolean = {
    val conn = getConnection(jdbcConfig)
    var stmt: Statement = null
    try {
      stmt = conn.createStatement
      val res = stmt.execute(sql)
      conn.commit()
      res
    } catch {
      case ex: Exception => ex.printStackTrace()
        false
    } finally {
      close(conn, stmt)
    }
  }

  /**
   *
   * @param prop
   * @return
   */
  def getConnection(prop: Properties): Connection = {
    val instance = prop(KEY_MYSQL_INSTANCE)
    val lock = lockMap.getOrElseUpdate(instance, new ReentrantLock())
    try {
      lock.lock()
      val ds: HikariDataSource = Try(Option(dataSourceHolder(instance))).getOrElse(None) match {
        case None =>
          //创建一个数据源对象
          val jdbcConfig = new HikariConfig()
          prop.filter(_._1 != KEY_MYSQL_INSTANCE).foreach(x => {
            val field = Try(Option(jdbcConfig.getClass.getDeclaredField(x._1))).getOrElse(None) match {
              case None =>
                val boolMethod = s"is${x._1.substring(0, 1).toUpperCase}${x._1.substring(1)}"
                Try(Option(jdbcConfig.getClass.getDeclaredField(boolMethod))).getOrElse(None) match {
                  case Some(x) => x
                  case None => throw new IllegalArgumentException(s"jdbcConfig error,property:${x._1} invalid,please see more properties jdbcConfig https://github.com/brettwooldridge/HikariCP")
                }
              case Some(x) => x
            }
            field.setAccessible(true)
            field.getType.getSimpleName match {
              case "String" => field.set(jdbcConfig, x._2)
              case "int" => field.set(jdbcConfig, x._2.toInt)
              case "long" => field.set(jdbcConfig, x._2.toLong)
              case "boolean" => field.set(jdbcConfig, x._2.toBoolean)
              case _ =>
            }
          })
          jdbcConfig.setAutoCommit(false)
          val ds = new HikariDataSource(jdbcConfig)
          dataSourceHolder += instance -> ds
          ds
        case Some(x) => x
      }
      //返回连接...
      val conn = ds.getConnection()
      conn.setAutoCommit(false)
      conn

      /**
       *Class.forName(prop(KEY_MYSQL_DRIVER))
       * val connection = Try(prop(KEY_MYSQL_USER)).getOrElse(null) match {
       * case null => DriverManager.getConnection(prop(KEY_MYSQL_URL))
       * case _ => DriverManager.getConnection(prop(KEY_MYSQL_URL), prop(KEY_MYSQL_USER), prop(KEY_MYSQL_PASSWORD))
       * }
       *connection.setAutoCommit(false)
       * connection
       **/
    } finally {
      lock.unlock()
    }
  }

  private[this] def createStatement(conn: Connection): Statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  def close(closeable: AutoCloseable*): Unit = Try(closeable.filter(x => x != null).foreach(_.close()))

}
