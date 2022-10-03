/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.connector.jdbc.internal

import org.apache.streampark.common.enums.ApiType
import org.apache.streampark.common.enums.ApiType.ApiType
import org.apache.streampark.common.util.{JdbcUtils, Logger}
import org.apache.streampark.flink.connector.function.TransformFunction
import org.apache.streampark.flink.connector.jdbc.bean.Transaction
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}

import java.sql.{Connection, SQLException, Statement}
import java.util.{Optional, Properties}

/**
 * (flink checkpoint + db transactionId) Simulates commit read. Use of flink's checkpoint mechanism, and only submit data that has been confirmed
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
    require(toSQLFn != null, "[StreamPark] ToSQLFunction can not be null")
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
    transaction.invoked = true
    transaction + sql
  }

  /**
   * Save the sql statement to be operated in the state. If this step fails, it will be rolled back
   *
   * @param transaction
   */
  override def preCommit(transaction: Transaction): Unit = {
    // Prevent called preCommit directly without called the invoke method
    if (transaction.invoked) {
      logInfo(s"Jdbc2PCSink preCommit.TransactionId:${transaction.transactionId}")
      buffer += transaction.transactionId -> transaction
    }
  }


  /**
   * When the data checkpoint is completed or the recovery is finished, this method will be called,
   * here use the transaction feature of db. The current operation is at the second stage:
   * If the current batch of data is saved successfully, the whole process is successful;
   * If it fails, will be thrown an exception, resulting in the checkpoint will also be rolled back,
   * at the same time, the next time to started, it will start from the last consumption location,
   * ensuring that end-to-end exactly once.
   */
  override def commit(transaction: Transaction): Unit = {
    if (transaction.invoked && transaction.sql.nonEmpty) {
      logInfo(s"Jdbc2PCSink commit,TransactionId:${transaction.transactionId}")
      var connection: Connection = null
      var statement: Statement = null
      try {
        connection = JdbcUtils.getConnection(jdbc)
        connection.setAutoCommit(false)
        statement = connection.createStatement()
        if (transaction.insertMode) {
          transaction.sql.foreach(statement.addBatch)
          statement.executeBatch
          statement.clearBatch()
        } else {
          transaction.sql.foreach(statement.executeUpdate)
        }
        connection.commit()
        // successful, clean state
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
