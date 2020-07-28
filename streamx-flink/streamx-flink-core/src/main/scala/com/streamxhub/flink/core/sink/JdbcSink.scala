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


import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction, TwoPhaseCommitSinkFunction}
import java.sql._
import java.util.concurrent.atomic.AtomicLong
import java.util.{Optional, Properties}

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.{ConfigUtils, JdbcUtils, Logger}
import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.enums.ApiType
import com.streamxhub.flink.core.enums.ApiType.ApiType
import com.streamxhub.flink.core.function.ToSQLFunction
import com.streamxhub.flink.core.sink.Dialect.Dialect
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.runtime.state.FunctionInitializationContext
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream

import scala.annotation.meta.param
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

object JdbcSink {

  /**
   * @param ctx   : StreamingContext
   * @param alias :    实例别名(用于区分多个不同的数据库实例...)
   * @return
   */
  def apply(@(transient@param) ctx: StreamingContext,
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit alias: String = ""): JdbcSink = new JdbcSink(ctx, parallelism, name, uid)

}

class JdbcSink(@(transient@param) ctx: StreamingContext,
               parallelism: Int = 0,
               name: String = null,
               uid: String = null)(implicit alias: String = "") extends Sink with Logger {

  /**
   *
   * @param stream  : DataStream
   * @param dialect : 数据库方言
   * @param toSQLFn : 转换成SQL的函数,有用户提供.
   * @tparam T : DataStream里的流的数据类型
   * @return
   */
  def sink[T](stream: DataStream[T], dialect: Dialect = Dialect.MYSQL)(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val prop = ConfigUtils.getJdbcConf(ctx.parameter.toMap, dialect.toString, alias)
    val sinkFun = new JdbcSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  def towPCSink[T](stream: DataStream[T], dialect: Dialect = Dialect.MYSQL)(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val prop = ConfigUtils.getJdbcConf(ctx.parameter.toMap, dialect.toString, alias)
    val sinkFun = new Jdbc2PCSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class JdbcSinkFunction[T](apiType: ApiType = ApiType.Scala, jdbc: Properties) extends RichSinkFunction[T] with Logger {
  private var connection: Connection = _
  private var statement: Statement = _
  private var scalaToSQLFn: T => String = _
  private var javaToSQLFunc: ToSQLFunction[T] = _
  private val offset: AtomicLong = new AtomicLong(0L)
  private var timestamp: Long = 0L

  private val batchSize = jdbc.remove(KEY_JDBC_INSERT_BATCH) match {
    case null => DEFAULT_JDBC_INSERT_BATCH
    case batch => batch.toString.toInt
  }

  def this(jdbc: Properties, toSQLFn: T => String) {
    this(ApiType.Scala, jdbc)
    this.scalaToSQLFn = toSQLFn
  }

  def this(jdbc: Properties, toSQLFn: ToSQLFunction[T]) {
    this(ApiType.JAVA, jdbc)
    require(toSQLFn != null, "[StreamX] ToSQLFunction can not be null")
    this.javaToSQLFunc = toSQLFn
  }

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    require(jdbc != null, "[StreamX] JdbcSink jdbc can not be null")
    logInfo("[StreamX] JdbcSink Open....")
    connection = JdbcUtils.getConnection(jdbc)
    connection.setAutoCommit(false)
    if (batchSize > 1) {
      statement = connection.createStatement()
    }
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    require(connection != null)
    val sql = apiType match {
      case ApiType.Scala => scalaToSQLFn(value)
      case ApiType.JAVA => javaToSQLFunc.toSQL(value)
    }
    batchSize match {
      case 1 =>
        try {
          statement = connection.prepareStatement(sql)
          statement.asInstanceOf[PreparedStatement].executeUpdate
          connection.commit()
        } catch {
          case e: Exception =>
            logError(s"[StreamX] JdbcSink invoke error:${sql}")
            throw e
          case _: Throwable =>
        }
      case batch =>
        try {
          statement.addBatch(sql)
          (offset.incrementAndGet() % batch, System.currentTimeMillis()) match {
            case (0, _) => execBatch()
            case (_, current) if current - timestamp > 1000 => execBatch()
            case _ =>
          }
        } catch {
          case e: Exception =>
            logError(s"[StreamX] JdbcSink batch invoke error:${sql}")
            throw e
          case _: Throwable =>
        }
    }
  }

  override def close(): Unit = {
    execBatch()
    JdbcUtils.close(statement, connection)
  }

  private[this] def execBatch(): Unit = {
    if (offset.get() > 0) {
      offset.set(0L)
      val start = System.currentTimeMillis()
      val count = statement.executeBatch().sum
      statement.clearBatch()
      connection.commit()
      logInfo(s"[StreamX] JdbcSink batch $count use ${System.currentTimeMillis() - start} MS")
      timestamp = System.currentTimeMillis()
    }
  }

}


class JdbcOutputFormat[T: TypeInformation](implicit prop: Properties, toSQlFun: T => String) extends RichOutputFormat[T] with Logger {

  val sinkFunction = new JdbcSinkFunction[T](prop, toSQlFun)

  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}

object Dialect extends Enumeration {
  type Dialect = Value
  val MYSQL, ORACLE, MSSQL, H2 = Value
}


//-------------Jdbc2PCSinkFunction,端到端精准一次---------------------------------------------------------------------------------------
/**
 *
 * @param apiType
 * @param jdbc
 * @tparam T
 */
class Jdbc2PCSinkFunction[T](apiType: ApiType = ApiType.Scala, jdbc: Properties)
  extends TwoPhaseCommitSinkFunction[T, Transaction, Void](new KryoSerializer[Transaction](classOf[Transaction], new ExecutionConfig), VoidSerializer.INSTANCE)
    with Logger {

  @transient private[this] var transactionState: ListState[Transaction] = _
  private var scalaToSQLFn: T => String = _
  private var javaToSQLFunc: ToSQLFunction[T] = _

  private val SINK_2PC_STATE = "jdbc-sink-2pc-state"

  def this(jdbc: Properties, toSQLFn: T => String) {
    this(ApiType.Scala, jdbc)
    this.scalaToSQLFn = toSQLFn
  }

  def this(jdbc: Properties, toSQLFn: ToSQLFunction[T]) {
    this(ApiType.JAVA, jdbc)
    require(toSQLFn != null, "[StreamX] ToSQLFunction can not be null")
    this.javaToSQLFunc = toSQLFn
  }

  override def initializeUserContext(): Optional[Void] = super.initializeUserContext()

  override def beginTransaction(): Transaction = {
    logInfo("[StreamX] Jdbc2PCSink beginTransaction.")
    Transaction()
  }

  override def invoke(transaction: Transaction, value: T, context: SinkFunction.Context[_]): Unit = {
    val sql = apiType match {
      case ApiType.Scala => scalaToSQLFn(value)
      case ApiType.JAVA => javaToSQLFunc.toSQL(value)
    }
    if (!sql.toUpperCase.trim.startsWith("INSERT")) {
      transaction.insertMode = false
    }
    //调用invoke插入过数据....
    transaction.invoked = true
    transaction.add(sql)
  }

  /**
   * call on snapshotState
   * 将要操作的sql语句保存到状态里.如果这一步失败,会回滚
   *
   * @param transaction
   */
  override def preCommit(transaction: Transaction): Unit = {
    //防止未调用invoke方法直接调用preCommit
    if (transaction.invoked) {
      logInfo(s"[StreamX] Jdbc2PCSink preCommit.TransactionId:${transaction.transactionId}")
      transactionState.add(transaction)
    }
  }


  /**
   * 在数据checkpoint完成或者恢复完成的时候会调用该方法,这里直接利用db的事务特性
   * 当前操作处于第二阶段:
   * 如果当前一批数据保存成功则整个过程成功
   * 如果失败,会抛出异常,导致本次完成的checkpoint也会回滚
   * 进而下次启动的时候还是从上次消费的位置开始.做到端到端精准一次.
   *
   * @param transaction
   */
  override def commit(transaction: Transaction): Unit = {
    //防止未调用invoke方法直接调用preCommit和commit...
    if (transaction.invoked && transaction.sql.nonEmpty) {
      logInfo(s"[StreamX] Jdbc2PCSink commit,TransactionId:${transaction.transactionId}")
      var connection: Connection = null
      var statement: Statement = null
      try {
        //获取jdbc连接....
        connection = JdbcUtils.getConnection(jdbc)
        connection.setAutoCommit(false)
        statement = connection.createStatement()
        //全部是插入则走批量插入
        if (transaction.insertMode) {
          transaction.sql.foreach(statement.addBatch)
          statement.executeBatch
          statement.clearBatch()
        } else {
          //单条记录插入...
          transaction.sql.foreach(sql => {
            statement.executeUpdate(sql)
          })
        }
        connection.commit()
        //成功,清除state...
        transactionState.clear()
      } catch {
        case e: SQLException =>
          logError(s"[StreamX] Jdbc2PCSink commit SQLException:${e.getMessage}")
          throw e
        case t: Throwable =>
          logError(s"[StreamX] Jdbc2PCSink commit Exception:${t.getMessage}")
          throw t
      } finally {
        JdbcUtils.close(statement, connection)
      }
    }
  }

  override def abort(transaction: Transaction): Unit = {
    logInfo(s"[StreamX] Jdbc2PCSink abort,TransactionId:${transaction.transactionId}")
    transactionState.clear()
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    logInfo("[StreamX] Jdbc2PCSink initializeState ....")
    transactionState = context.getOperatorStateStore.getListState(new ListStateDescriptor(SINK_2PC_STATE, classOf[Transaction]))
    super.initializeState(context)
  }

}


class Jdbc2PCOutputFormat[T: TypeInformation](implicit prop: Properties, toSQlFun: T => String) extends RichOutputFormat[T] with Logger {

  private val sinkFunction = new Jdbc2PCSinkFunction[T](prop, toSQlFun)

  private var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}

case class Transaction(transactionId: String = System.currentTimeMillis().toString, sql: ListBuffer[String] = ListBuffer.empty, var insertMode: Boolean = true, var invoked: Boolean = false) extends Serializable {
  def add(text: String): Unit = sql.add(text)
}




