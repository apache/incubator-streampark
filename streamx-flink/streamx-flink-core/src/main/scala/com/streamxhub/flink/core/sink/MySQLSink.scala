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


import java.sql.{Connection, DriverManager, SQLException}
import java.util.Properties

import com.streamxhub.common.util.{ConfigUtils, Logger}
import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.common.conf.ConfigConst._
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig

import scala.collection.Map
import scala.collection.JavaConversions._
import scala.util.Try

/**
 *
 * TODO  注意：
 * TODO  注意：
 * TODO  注意：
 * TODO 1) 该MySQLSink实现可能针对不用的MySQL版本会出现问题,导致获取类型失败(	at com.esotericsoftware.kryo.Generics.getConcreteClass(Generics.java:44)... )
 * TODO 2) 该MySQLSink对MySQL的连接时长有要求,要连接等待时长(wait_timeout)设置的长一些,不然一个连接打开一直在invoke插入数据阶段由于还未提交,还一直在长时间插入可能会超时(Communications link failure during rollback(). Transaction resolution unknown.)
 *
 */
object MySQLSink {

  /**
   * @param ctx      : StreamingContext
   * @param instance : MySQL的实例名称(用于区分多个不同的MySQL实例...)
   * @return
   */
  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit instance: String = ""): MySQLSink = new MySQLSink(ctx, overwriteParams, parallelism, name, uid)

}

class MySQLSink(@transient ctx: StreamingContext,
                overwriteParams: Map[String, String] = Map.empty[String, String],
                parallelism: Int = 0,
                name: String = null,
                uid: String = null)(implicit instance: String = "") extends Sink with Logger {


  ctx.enableCheckpointing(5000)
  ctx.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
  ctx.getCheckpointConfig.setMinPauseBetweenCheckpoints(1000)
  ctx.getCheckpointConfig.setCheckpointTimeout(5000)
  ctx.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
  ctx.getCheckpointConfig.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)

  /**
   *
   * @param stream  : DataStream
   * @param toSQLFn : 转换成SQL的函数,有用户提供.
   * @tparam T : DataStream里的流的数据类型
   * @return
   */
  def sink[T](stream: DataStream[T])(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val prop = ConfigUtils.getMySQLConf(ctx.paramMap)(instance)
    overwriteParams.foreach(x=>prop.put(x._1,x._2))
    val sinkFun = new MySQLSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

/**
 * MySQLSink基于两阶段提交实现,保证了数据的ExactlyOnce,可直接使用与生产环境。。。
 *
 * @param config
 * @param toSQLFn
 * @tparam T
 */
class MySQLSinkFunction[T](config: Properties, toSQLFn: T => String)
  extends TwoPhaseCommitSinkFunction[T, Connection, Void](new KryoSerializer[Connection](classOf[Connection], new ExecutionConfig), VoidSerializer.INSTANCE)
    with Logger {

  override def beginTransaction(): Connection = {
    logInfo("[StreamX] MySQLSink beginTransaction ....")
    Class.forName(config(KEY_JDBC_DRIVER))
    val connection = Try(config(KEY_JDBC_USER)).getOrElse(null) match {
      case null => DriverManager.getConnection(config(KEY_JDBC_URL))
      case _ => DriverManager.getConnection(config(KEY_JDBC_URL), config(KEY_JDBC_USER), config(KEY_JDBC_PASSWORD))
    }
    connection.setAutoCommit(false)
    connection
  }

  override def invoke(transaction: Connection, value: T, context: SinkFunction.Context[_]): Unit = {
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
        //前一个连接提交事务,则重新获取一个新连接
        this.beginTransaction()
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

  private def close(conn: Connection): Unit = if (conn != null) conn.close()


}


class MySQLOutputFormat[T: TypeInformation](implicit prop: Properties,toSQlFun: T => String) extends RichOutputFormat[T] with Logger {

  val sinkFunction = new MySQLSinkFunction[T](prop,toSQlFun)

  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}



