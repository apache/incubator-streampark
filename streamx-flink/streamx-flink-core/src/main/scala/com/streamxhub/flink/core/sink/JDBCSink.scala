package com.streamxhub.flink.core.sink


import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import java.sql._
import java.util.Properties

import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.conf.Config
import com.streamxhub.flink.core.util.{Logger, MySQLUtils}
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream

import scala.collection.Map
import scala.collection.JavaConversions._

object JDBCSink {

  /**
   * @param ctx      : StreamingContext
   * @param instance : MySQL的实例名称(用于区分多个不同的MySQL实例...)
   * @return
   */
  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit instance: String = ""): JDBCSink = new JDBCSink(ctx, overwriteParams, parallelism, name, uid)

}

class JDBCSink(@transient ctx: StreamingContext,
               overwriteParams: Map[String, String] = Map.empty[String, String],
               parallelism: Int = 0,
               name: String = null,
               uid: String = null)(implicit instance: String = "") extends Sink with Logger {

  /**
   *
   * @param stream  : DataStream
   * @param toSQLFn : 转换成SQL的函数,有用户提供.
   * @tparam T : DataStream里的流的数据类型
   * @return
   */
  def sink[T](stream: DataStream[T])(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val prop = Config.getMySQLSink(ctx.parameter)(instance)
    prop.putAll(overwriteParams)
    val sinkFun = new JDBCSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class JDBCSinkFunction[T](config: Properties, toSQLFn: T => String) extends RichSinkFunction[T] with Logger {

  private var connection: Connection = null
  private var preparedStatement: PreparedStatement = null

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    logInfo("[StreamX] JDBCSink Open....")
    connection = MySQLUtils.getConnection(config)
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    logInfo("[StreamX] JDBCSink invoke....")
    require(connection != null)
    val sql = toSQLFn(value)
    preparedStatement = connection.prepareStatement(sql)
    try{
      preparedStatement.executeUpdate
      connection.commit()
    }catch {
      case e:Exception =>
        System.err.println(s"[StreamX] JDBCSink invoke error:${sql}")
    }
  }

  /**
   *
   * @throws
   */
  @throws[Exception]
  override def close(): Unit =  MySQLUtils.close(preparedStatement,connection)

}

