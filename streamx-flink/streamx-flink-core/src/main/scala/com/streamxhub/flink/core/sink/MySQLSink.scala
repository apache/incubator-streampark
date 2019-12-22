package com.streamxhub.flink.core.sink


import java.sql.{Connection, SQLException}

import com.streamxhub.flink.core.util.{MySQLConfig, MySQLUtils}
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}

class MySQLSink(implicit config: MySQLConfig) extends TwoPhaseCommitSinkFunction[String, Connection, Void](new KryoSerializer[Connection](classOf[Connection], new ExecutionConfig), VoidSerializer.INSTANCE) {

  override def beginTransaction(): Connection = {
    val connection = MySQLUtils.getConnection(this.config)
    connection.setAutoCommit(false)
    connection
  }

  override def invoke(transaction: Connection, sql: String, context: SinkFunction.Context[_]): Unit = transaction.prepareStatement(sql).execute

  override def preCommit(transaction: Connection): Unit = {}

  override def commit(transaction: Connection): Unit = {
    if (transaction != null) {
      try {
        transaction.commit()
      } catch {
        case e: SQLException => e.printStackTrace()
      } finally {
        close(transaction)
      }
    }
  }

  override def abort(transaction: Connection): Unit = {
    if (transaction != null) {
      try {
        transaction.rollback()
      } catch {
        case e: SQLException => e.printStackTrace()
      } finally {
        close(transaction)
      }
    }
  }

  private def close(conn: Connection): Unit =  MySQLUtils.close(this.config, conn, null, null)

}
