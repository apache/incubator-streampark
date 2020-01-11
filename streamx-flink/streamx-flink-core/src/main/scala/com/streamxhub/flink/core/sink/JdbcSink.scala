package com.streamxhub.flink.core.sink


import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import java.sql._
import java.util.Properties

import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.conf.Config
import com.streamxhub.flink.core.util.{Logger, MySQLUtils}
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.scala.DataStream

import scala.collection.Map
import scala.collection.JavaConversions._

object JdbcSink {

  /**
   * @param ctx      : StreamingContext
   * @param instance : MySQL的实例名称(用于区分多个不同的MySQL实例...)
   * @return
   */
  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit instance: String = ""): JdbcSink = new JdbcSink(ctx, overwriteParams, parallelism, name, uid)

}

class JdbcSink(@transient ctx: StreamingContext,
               overwriteParams: Map[String, String] = Map.empty[String, String],
               parallelism: Int = 0,
               name: String = null,
               uid: String = null)(implicit instance: String = "") extends Sink with Logger {

  //每隔10s进行启动一个检查点
  ctx.enableCheckpointing(10000)
  //设置模式为：exactly_one，仅一次语义
  ctx.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
  //确保检查点之间有1s的时间间隔【checkpoint最小间隔】
  ctx.getCheckpointConfig.setMinPauseBetweenCheckpoints(1000)
  //检查点必须在10s之内完成，或者被丢弃【checkpoint超时时间】
  ctx.getCheckpointConfig.setCheckpointTimeout(10000)
  //同一时间只允许进行一次检查点
  ctx.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
  //被cancel会保留Checkpoint数据
  ctx.getCheckpointConfig.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)

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
    val sinkFun = new JdbcSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class JdbcSinkFunction[T](config: Properties, toSQLFn: T => String) extends RichSinkFunction[T] with Logger {

  private var connection: Connection = _
  private var preparedStatement: PreparedStatement = _

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    logInfo("[StreamX] JdbcSink Open....")
    connection = MySQLUtils.getConnection(config)
    connection.setAutoCommit(false)
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    require(connection != null)
    val sql = toSQLFn(value)
    preparedStatement = connection.prepareStatement(sql)
    try{
      preparedStatement.executeUpdate
      connection.commit()
    }catch {
      case e:Exception =>
        logError(s"[StreamX] JdbcSink invoke error:${sql}")
        throw e
    }
  }

  /**
   *
   * @throws
   */
  @throws[Exception]
  override def close(): Unit =  MySQLUtils.close(preparedStatement,connection)

}

