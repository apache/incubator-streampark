/*
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
package com.streamxhub.streamx.flink.core.scala.source

import com.streamxhub.streamx.common.enums.ApiType
import com.streamxhub.streamx.common.enums.ApiType.ApiType
import com.streamxhub.streamx.common.util.{FlinkUtils, JdbcUtils, Logger, Utils}
import com.streamxhub.streamx.flink.core.java.function.{RunningFunction, SQLQueryFunction, SQLResultFunction}
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.state.ListState
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.runtime.state.{CheckpointListener, FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.RichSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.scala.DataStream

import java.lang
import java.util.Properties
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.Map
import scala.util.{Success, Try}


object JdbcSource {

  def apply(@(transient@param) property: Properties = new Properties())(implicit ctx: StreamingContext): JdbcSource = new JdbcSource(ctx, property)

}

class JdbcSource(@(transient@param) val ctx: StreamingContext, property: Properties = new Properties()) {

  /**
   *
   * @param sqlFun
   * @param fun
   * @param jdbc
   * @tparam R
   * @return
   */
  def getDataStream[R: TypeInformation](sqlFun: R => String,
                                        fun: Iterable[Map[String, _]] => Iterable[R],
                                        running: Unit => Boolean)(implicit jdbc: Properties = new Properties()): DataStream[R] = {
    Utils.copyProperties(property, jdbc)
    val mysqlFun = new JdbcSourceFunction[R](jdbc, sqlFun, fun, running)
    ctx.addSource(mysqlFun)
  }

}

/**
 *
 * @tparam R
 */
private[this] class JdbcSourceFunction[R: TypeInformation](apiType: ApiType = ApiType.scala, jdbc: Properties) extends RichSourceFunction[R]
  with CheckpointedFunction
  with CheckpointListener
  with Logger {

  @volatile private[this] var running = true
  private[this] var scalaRunningFunc: Unit => Boolean = (_) => true
  private[this] var javaRunningFunc: RunningFunction = _

  private[this] var scalaSqlFunc: R => String = _
  private[this] var scalaResultFunc: Function[Iterable[Map[String, _]], Iterable[R]] = _
  private[this] var javaSqlFunc: SQLQueryFunction[R] = _
  private[this] var javaResultFunc: SQLResultFunction[R] = _
  @transient private var state: ListState[R] = _
  private val OFFSETS_STATE_NAME: String = "jdbc-source-query-states"
  private[this] var last: R = _

  //for Scala
  def this(jdbc: Properties,
           sqlFunc: R => String,
           resultFunc: Iterable[Map[String, _]] => Iterable[R],
           runningFunc: Unit => Boolean) = {

    this(ApiType.scala, jdbc)
    this.scalaSqlFunc = sqlFunc
    this.scalaResultFunc = resultFunc
    this.scalaRunningFunc = if (runningFunc == null) _ => true else runningFunc
  }

  //for JAVA
  def this(jdbc: Properties,
           javaSqlFunc: SQLQueryFunction[R],
           javaResultFunc: SQLResultFunction[R],
           runningFunc: RunningFunction) {

    this(ApiType.java, jdbc)
    this.javaSqlFunc = javaSqlFunc
    this.javaResultFunc = javaResultFunc
    this.javaRunningFunc = if (runningFunc != null) runningFunc else new RunningFunction {
      override def running(): lang.Boolean = true
    }
  }

  @throws[Exception]
  override def run(ctx: SourceContext[R]): Unit = {
    while (this.running) {
      apiType match {
        case ApiType.scala =>
          if (scalaRunningFunc()) {
            ctx.getCheckpointLock.synchronized {
              val sql = scalaSqlFunc(last)
              val result: List[Map[String, _]] = JdbcUtils.select(sql)(jdbc)
              scalaResultFunc(result).foreach(x => {
                last = x
                ctx.collectWithTimestamp(last, System.currentTimeMillis())
              })
            }
          }
        case ApiType.java =>
          if (javaRunningFunc.running()) {
            ctx.getCheckpointLock.synchronized {
              val sql = javaSqlFunc.query(last)
              val result: List[Map[String, _]] = JdbcUtils.select(sql)(jdbc)
              javaResultFunc.result(result.map(_.asJava)).foreach(x => {
                last = x
                ctx.collectWithTimestamp(last, System.currentTimeMillis())
              })
            }
          }
      }
    }
  }

  override def cancel(): Unit = this.running = false

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    if (running) {
      state.clear()
      if (last != null) {
        state.add(last)
      }
    } else {
      logError("JdbcSource snapshotState called on closed source")
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    //从checkpoint中恢复...
    logInfo("JdbcSource snapshotState initialize")
    state = FlinkUtils.getUnionListState[R](context, OFFSETS_STATE_NAME)
    Try(state.get.head) match {
      case Success(q) => last = q
      case _ =>
    }
  }

  override def notifyCheckpointComplete(checkpointId: Long): Unit = {
    logInfo(s"JdbcSource checkpointComplete: $checkpointId")
  }
}

