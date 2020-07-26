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
package com.streamxhub.flink.core.sink


import java.sql.{SQLException, Statement}
import java.util.{Optional, Properties}

import com.streamxhub.common.util.{JdbcUtils, Logger}
import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.common.util.ConfigUtils._
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.state._
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.FunctionInitializationContext
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}
import org.apache.flink.streaming.api.scala.DataStream

import scala.annotation.meta.param
import scala.collection.Map
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
 *
 * 真正实现MySQL端到端精准一次sink.
 *
 */
object MySQLSink {

  /**
   * @param ctx   : StreamingContext
   * @param alias :   MySQL的实例别名(用于区分多个不同的MySQL实例...)
   * @return
   */
  def apply(@(transient@param) ctx: StreamingContext,
            overrideParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit alias: String = ""): MySQLSink = new MySQLSink(ctx, overrideParams, parallelism, name, uid)

}

class MySQLSink(@(transient@param) ctx: StreamingContext,
                overrideParams: Map[String, String] = Map.empty[String, String],
                parallelism: Int = 0,
                name: String = null,
                uid: String = null)(implicit alias: String = "") extends Sink with Logger {

  /**
   *
   * @param stream  : DataStream
   * @param toSQLFn : 转换成SQL的函数,有用户提供.
   * @tparam T : DataStream里的流的数据类型
   * @return
   */
  def sink[T](stream: DataStream[T])(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val prop = getMySQLConf(ctx.parameter.toMap)(alias)
    overrideParams.foreach(x => prop.put(x._1, x._2))
    val sinkFun = new MySQLSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

/**
 *
 * @param config
 * @param toSQLFn
 * @tparam T
 */
class MySQLSinkFunction[T](config: Properties, toSQLFn: T => String)
  extends TwoPhaseCommitSinkFunction[T, Transaction, Void](new KryoSerializer[Transaction](classOf[Transaction], new ExecutionConfig), VoidSerializer.INSTANCE)
    with Logger {

  @transient private[this] var transactionState: ListState[Transaction] = _

  private val SINK_2PC_STATE = "mysql-sink-2pc-state"

  override def initializeUserContext(): Optional[Void] = super.initializeUserContext()

  override def beginTransaction(): Transaction = {
    logInfo("[StreamX] MySQLSink beginTransaction.")
    Transaction()
  }

  override def invoke(transaction: Transaction, value: T, context: SinkFunction.Context[_]): Unit = {
    val sql = toSQLFn(value)
    if (!sql.toUpperCase.trim.startsWith("INSERT")) {
      transaction.copy(insertMode = false)
    }
    transaction.add(sql)
  }

  /**
   * call on snapshotState
   *
   * @param transaction
   */
  override def preCommit(transaction: Transaction): Unit = {
    logInfo(s"[StreamX] MySQLSink preCommit.TransactionId:${transaction.transactionId}")
    transactionState.add(transaction)
  }

  /**
   * 在数据checkpoint完成或者恢复完成的时候会调用该方法
   *
   * @param transaction
   */
  override def commit(transaction: Transaction): Unit = {
    println(s"[StreamX] MySQLSink commit,TransactionId:${transaction.transactionId}")
    val state = Try(transactionState.get().filter(_.transactionId == transaction.transactionId).head).getOrElse(null)
    if (state != null && state.sqlList.nonEmpty) {
      //获取jdbc连接....
      val connection = JdbcUtils.getConnection(config)
      var statement: Statement = null
      try {
        connection.setAutoCommit(false)
        //全部是插入则走批量插入
        if (state.insertMode) {
          statement = connection.createStatement()
          state.sqlList.foreach(statement.addBatch)
          statement.executeBatch
          statement.clearBatch()
        } else {
          //单条记录插入...
          state.sqlList.foreach(sql => {
            statement = connection.createStatement()
            statement.executeUpdate(sql)
          })
        }
        connection.commit()
      } catch {
        case e: SQLException => logError(s"[StreamX] MySQLSink commit error:${e.getMessage}")
      } finally {
        JdbcUtils.close(statement, connection)
        transactionState.clear()
      }
    }
  }

  override def abort(transaction: Transaction): Unit = {
    transactionState.clear()
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    logInfo("[StreamX] MySQLSink initializeState ....")
    transactionState = context.getOperatorStateStore.getListState(new ListStateDescriptor(SINK_2PC_STATE, classOf[Transaction]))
    super.initializeState(context)
  }

}


class MySQLOutputFormat[T: TypeInformation](implicit prop: Properties, toSQlFun: T => String) extends RichOutputFormat[T] with Logger {

  private val sinkFunction = new MySQLSinkFunction[T](prop, toSQlFun)

  private var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}

case class Transaction(transactionId: String = System.currentTimeMillis().toString, sqlList: ListBuffer[String] = ListBuffer.empty, insertMode: Boolean = true) extends Serializable {
  def add(sql: String): Unit = sqlList.add(sql)
}


