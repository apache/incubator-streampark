package com.streamxhub.flink.core.sink

import java.sql._
import java.util

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import ru.yandex.clickhouse.ClickHouseDataSource
import ru.yandex.clickhouse.settings.ClickHouseProperties

class ClickHouseJDBCSink[Map[String, Any]]() extends RichSinkFunction[util.Map[String, Any]] {
  // 定义提交窗口时间/频次值
  var batchSize: Int = Integer.valueOf(500)
  var batchInterval: Int = Integer.valueOf(6000)
  var batchCount: Int = 0
  var lastBatchTime: Long = System.currentTimeMillis

  // clickhouse参数值
  var schemaName: String = "test"
  var tableName: String = "test"
  var user: String = "default"
  var password: String = ""
  var url: String = "jdbc:clickhouse://hadoop222:8123"

  // 全局连接变量
  var dataSource: ClickHouseDataSource = null
  var connection: Connection = null
  var pst: PreparedStatement = null
  var columnNameAndType: util.List[String] = null

  // 继承open方法
  override def open(parameters: Configuration): Unit = {
    // 设置clickhouse连接配置
    val properties = new ClickHouseProperties()
    properties.setUser(user)
    properties.setPassword(password)
    properties.setDatabase(schemaName)
    properties.setSocketTimeout(50000)
    dataSource = new ClickHouseDataSource(url, properties)

    connection = dataSource.getConnection()

    // 动态获取表字段:类型
    columnNameAndType = getColumnNameAndType(tableName, schemaName, connection)
    val insertSql = generatePreparedSqlByColumnNameAndType(columnNameAndType, schemaName, tableName)
    pst = connection.prepareStatement(insertSql)
    //    System.out.println("insertsql:"+insertSql)
    //    System.out.println("columnNameAndType:"+columnNameAndType)
  }

  // 继承invoke
  override def invoke(map: util.Map[String, Any], context: SinkFunction.Context[_]): Unit = {

    try { //this inserts your data

      // 峰值提交值信息
      pst = generatePreparedCloumns(pst, columnNameAndType, map)
      batchCount = batchCount + 1

      pst.addBatch()

      // 当大于batchSize条数或者时间窗口为lastBatchTime时提交
      if (batchCount >= batchSize || lastBatchTime < System.currentTimeMillis - batchInterval) {
        batchCount = 0
        lastBatchTime = System.currentTimeMillis()
        pst.executeBatch()
      }
    } catch {
      case e: SQLException =>
        println(e.printStackTrace())
    }

  }

  // 继承close方法
  override def close(): Unit = {

    if (pst != null) {
      pst.close()
    }
    if (connection != null) {
      connection.close()
    }
  }

  /**
    * 动态封装sql语句
    *
    * @param columnNames
    * @param schemaName 库名
    * @param tableName  表名
    * @return sql语句
    */
  private def generatePreparedSqlByColumnNameAndType(columnNames: util.List[String], schemaName: String, tableName: String): String = {
    var insertColumns: String = ""
    var insertValues: String = ""
    if (columnNames != null && columnNames.size > 0) {
      insertColumns += columnNames.get(0).split(":")(0)
      insertValues += "?"
    }

    for (i <- 1 until columnNames.size()) {
      insertColumns += ", " + columnNames.get(i).split(":")(0)
      insertValues += ", " + "?"
    }

    val insertSql: String = "INSERT INTO " + schemaName + "." + tableName + " (" + insertColumns + ") values(" + insertValues + ")"
    insertSql
  }

  /**
    * 根据库/表明获取字段名称及类型
    *
    * @param tableName  表名
    * @param schemaName 库名
    * @param conn       连接
    * @return List<String> String=字段名:自动类型
    * @throws SQLException
    */
  @throws[SQLException]
  private def getColumnNameAndType(tableName: String, schemaName: String, conn: Connection): util.List[String] = {
    val dd: DatabaseMetaData = conn.getMetaData
    val columnNameAndType: util.List[String] = new util.ArrayList[String]
    val colRet: ResultSet = dd.getColumns(null, "%", tableName, "%")
    while ( {
      colRet.next
    }) {
      val columnName: String = colRet.getString("COLUMN_NAME")
      val columnType: String = colRet.getString("TYPE_NAME")
      columnNameAndType.add(columnName + ":" + columnType)
    }

    columnNameAndType
  }

  /**
    * 动态拼装sql值
    *
    * @param ps
    * @param columnNamesAndType 字段类型
    * @return
    */
  private def generatePreparedCloumns(ps: PreparedStatement, columnNamesAndType: util.List[String], map: util.Map[String, Any]) = {
    try {
      for (i <- 0 to columnNamesAndType.size() - 1) {

        // 设置sql值时的下标
        var y: Int = 0
        y = i + 1

        val columnName = columnNamesAndType.get(i).split(":")(0)
        val value = map.get(columnName)
        var clickhouseType = columnNamesAndType.get(i).split(":")(1)
        //                value=String.valueOf(value);
        var convert_value = String.valueOf(value)
        if (convert_value == "null" || convert_value == "") convert_value = "null"

        if (isNullable(clickhouseType)) clickhouseType = unwrapNullable(clickhouseType)

        if (clickhouseType.startsWith("Int") || clickhouseType.startsWith("UInt")) {
          if (convert_value == "null") convert_value = "0"
          if (clickhouseType.endsWith("64")) ps.setLong(y, convert_value.toLong)
          else ps.setInt(y, Integer.valueOf(convert_value))
        }
        else if ("String" == clickhouseType) {
          if (convert_value == "null") convert_value = "null"
          ps.setString(y, String.valueOf(convert_value))
        }
        else if (clickhouseType.startsWith("Float32")) {
          if (convert_value == "null") {
            convert_value = "0"
          }
          ps.setFloat(y, convert_value.toFloat)
        }
        else if (clickhouseType.startsWith("Float64")) {
          if (convert_value == "null") {
            convert_value = "0"
          }
          ps.setDouble(y, convert_value.toDouble)
        }
        else if ("Date" == clickhouseType) {
          if (convert_value == "null") convert_value = "0"
          ps.setString(y, String.valueOf(convert_value))
        }
        else if ("DateTime" == clickhouseType) {
          if (convert_value == "null") convert_value = "0"
          ps.setString(y, String.valueOf(convert_value))
        }
        else if ("FixedString" == clickhouseType) { // BLOB 暂不处理
          ps.setString(y, "ERROR")
        }
        else if (isArray(clickhouseType)) { //ARRAY 暂不处理
          ps.setString(y, "ERROR")
        }
        else ps.setString(y, "ERROR")
      }
    } catch {
      case e: Exception =>
        println("初始化clickhouse sinks生成prepared columns异常", e)
    }
    ps
  }

  private def unwrapNullable(clickhouseType: String): String = {
    clickhouseType.substring("Nullable(".length, clickhouseType.length - 1)
  }

  /**
    * 判断是否为Nullable
    *
    * @param clickhouseType
    * @return
    */
  private def isNullable(clickhouseType: String): Boolean = {
    clickhouseType.startsWith("Nullable(") && clickhouseType.endsWith(")")
  }

  /**
    * 判断是否为Array
    *
    * @param clickhouseType
    * @return
    */
  private def isArray(clickhouseType: String): Boolean = {
    clickhouseType.startsWith("Array(") && clickhouseType.endsWith(")")
  }
}
