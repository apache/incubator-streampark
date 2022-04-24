/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.connector.jdbc.internal

import com.streamxhub.streamx.common.enums.ApiType
import com.streamxhub.streamx.common.enums.ApiType.ApiType
import com.streamxhub.streamx.common.util.{JdbcUtils, Logger}
import com.streamxhub.streamx.flink.connector.function.TransformFunction
import com.streamxhub.streamx.flink.connector.jdbc.bean.Transaction
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}

import java.sql.{Connection, SQLException, Statement}
import java.util.{Optional, Properties}

/**
 * (flink checkpoint + db transactionId) 模拟了提交读.充分利用flink的checkpoint机制,经过flink checkpoint确认过的数据才提交
 *
 * @param apiType
 * @param jdbc
 * @tparam T
 */
class Jdbc2PCSinkFunction[T](apiType: ApiType = ApiType.scala, jdbc: Properties)
  extends TwoPhaseCommitSinkFunction[T, Transaction, Void](new KryoSerializer[Transaction](classOf[Transaction], new ExecutionConfig), VoidSerializer.INSTANCE)
    with Logger {

  private[this] val buffer: collection.mutable.Map[String, Transaction] = collection.mutable.Map.empty[String, Transaction]

  private var scalaToSQLFn: T => String = _
  private var javaToSQLFunc: TransformFunction[T, String] = _

  def this(jdbc: Properties, toSQLFn: T => String) {
    this(ApiType.scala, jdbc)
    this.scalaToSQLFn = toSQLFn
  }

  def this(jdbc: Properties, toSQLFn: TransformFunction[T, String]) {
    this(ApiType.java, jdbc)
    require(toSQLFn != null, "[StreamX] ToSQLFunction can not be null")
    this.javaToSQLFunc = toSQLFn
  }

  override def initializeUserContext(): Optional[Void] = super.initializeUserContext()

  override def beginTransaction(): Transaction = {
    logInfo("Jdbc2PCSink beginTransaction.")
    Transaction()
  }

  override def invoke(transaction: Transaction, value: T, context: SinkFunction.Context): Unit = {
    val sql = apiType match {
      case ApiType.scala => scalaToSQLFn(value)
      case ApiType.java => javaToSQLFunc.transform(value)
    }
    if (!sql.toUpperCase.trim.startsWith("INSERT")) {
      transaction.insertMode = false
    }
    //调用invoke插入过数据....
    transaction.invoked = true
    transaction + sql
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
      logInfo(s"Jdbc2PCSink preCommit.TransactionId:${transaction.transactionId}")
      buffer += transaction.transactionId -> transaction
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
      logInfo(s"Jdbc2PCSink commit,TransactionId:${transaction.transactionId}")
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
          transaction.sql.foreach(statement.executeUpdate)
        }
        connection.commit()
        //成功,清除state...
        buffer -= transaction.transactionId
      } catch {
        case t: Throwable =>
          logError(s"Jdbc2PCSink commit Exception:${t.getMessage}")
          throw t
      } finally {
        JdbcUtils.close(statement, connection)
      }
    }
  }

  override def abort(transaction: Transaction): Unit = {
    logInfo(s"Jdbc2PCSink abort,TransactionId:${transaction.transactionId}")
    buffer -= transaction.transactionId
  }

}
