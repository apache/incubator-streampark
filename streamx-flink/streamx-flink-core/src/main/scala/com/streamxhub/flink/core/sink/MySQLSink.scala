package com.streamxhub.flink.core.sink


import java.sql.{Connection, SQLException}
import java.util.Properties

import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.conf.Config
import com.streamxhub.flink.core.util.{Logger, MySQLUtils}
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}
import org.apache.flink.streaming.api.scala.DataStream

import scala.collection.Map
import scala.collection.JavaConversions._

object MySQLSink {

  /**
   * @param ctx      : StreamingContext
   * @param instance : MySQL的实例名称(用于区分多个不同的MySQL实例...)
   * @return
   */
  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            name: String = null,
            parallelism: Int = 0,
            uidHash: String = null)(implicit instance: String = ""): MySQLSink = new MySQLSink(ctx, overwriteParams, name, parallelism, uidHash)

}

class MySQLSink(@transient ctx: StreamingContext,
                overwriteParams: Map[String, String] = Map.empty[String, String],
                name: String = null,
                parallelism: Int = 0,
                uidHash: String = null)(implicit instance: String = "") extends Sink with Logger {

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
    val sinkFun = new MySQLSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, name, parallelism, uidHash)
  }

}

/**
 * MySQLSink基于两阶段提交实现,保证了数据的ExactlyOnce,可直接使用与生产环境。。。
 *
 * @param config
 * @param toSQLFn
 * @tparam T
 */
class MySQLSinkFunction[T](config: Properties, toSQLFn: T => String) extends TwoPhaseCommitSinkFunction[T, Connection, Void](new KryoSerializer[Connection](classOf[Connection], new ExecutionConfig), VoidSerializer.INSTANCE) with Logger {

  override def beginTransaction(): Connection = {
    logInfo("[StreamX] MySQLSink beginTransaction ....")
    val connection = MySQLUtils.getConnection(this.config)
    connection.setAutoCommit(false)
    connection
  }

  override def invoke(transaction: Connection, value: T, context: SinkFunction.Context[_]): Unit = {
    logInfo("[StreamX] MySQLSink invoke ....")
    val sql = toSQLFn(value)
    transaction.prepareStatement(sql).executeUpdate()
  }

  override def preCommit(transaction: Connection): Unit = {
    logInfo("[StreamX] MySQLSink preCommit ....")
  }

  override def commit(transaction: Connection): Unit = {
    if (transaction != null) {
      try {
        logInfo("[StreamX] MySQLSink commit ....")
        transaction.commit()
      } catch {
        case e: SQLException => logError(s"[StreamX] MySQLSink commit error:${e.getMessage}")
      } finally {
        close(transaction)
      }
    }
  }

  override def abort(transaction: Connection): Unit = {
    if (transaction != null) {
      try {
        logInfo(s"[StreamX] MySQLSink abort ...")
        transaction.rollback()
      } catch {
        case e: SQLException => logError(s"[StreamX] MySQLSink commit error:${e.getMessage}")
      } finally {
        close(transaction)
      }
    }
  }

  private def close(conn: Connection): Unit = MySQLUtils.close(this.config, conn, null, null)

}