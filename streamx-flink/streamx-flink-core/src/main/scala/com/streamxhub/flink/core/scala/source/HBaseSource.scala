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
package com.streamxhub.flink.core.scala.source

import com.streamxhub.common.util.{Logger, Utils}
import com.streamxhub.flink.core.java.function.{HBaseQueryFunction, HBaseResultFunction}
import com.streamxhub.flink.core.java.wrapper.HBaseQuery
import com.streamxhub.flink.core.scala.StreamingContext
import com.streamxhub.flink.core.scala.enums.ApiType
import com.streamxhub.flink.core.scala.enums.ApiType.ApiType
import com.streamxhub.flink.core.scala.util.FlinkUtils
import org.apache.flink.api.common.state.ListState
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{CheckpointListener, FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.RichSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.hadoop.hbase.client._

import java.util.Properties
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.util.{Success, Try}

object HBaseSource {

  def apply(@(transient@param) ctx: StreamingContext, property: Properties = new Properties()): HBaseSource = new HBaseSource(ctx, property)

}

/**
 * 支持端到端的精准一次,可回放...
 *
 * @param ctx
 * @param property
 */
class HBaseSource(@(transient@param) val ctx: StreamingContext, property: Properties = new Properties()) {

  def getDataStream[R: TypeInformation](query: R => HBaseQuery, func: Result => R)(implicit prop: Properties = new Properties()): DataStream[R] = {
    Utils.copyProperties(property, prop)
    val hBaseFunc = new HBaseSourceFunction[R](prop, query, func)
    ctx.addSource(hBaseFunc)
  }

}


class HBaseSourceFunction[R: TypeInformation](apiType: ApiType = ApiType.scala, prop: Properties) extends RichSourceFunction[R] with CheckpointedFunction with CheckpointListener with Logger {

  @volatile private[this] var running = true

  @transient private[this] var table: Table = _

  @volatile var query: HBaseQuery = _

  private[this] var scalaQueryFunc: R => HBaseQuery = _
  private[this] var scalaResultFunc: Result => R = _

  private[this] var javaQueryFunc: HBaseQueryFunction[R] = _
  private[this] var javaResultFunc: HBaseResultFunction[R] = _

  @transient private var state: ListState[R] = _
  private val OFFSETS_STATE_NAME: String = "hbase-source-query-states"
  private[this] var last: R = _

  //for Scala
  def this(prop: Properties, queryFunc: R => HBaseQuery, resultFunc: Result => R) = {
    this(ApiType.scala, prop)
    this.scalaQueryFunc = queryFunc
    this.scalaResultFunc = resultFunc
  }

  //for JAVA
  def this(prop: Properties, queryFunc: HBaseQueryFunction[R], resultFunc: HBaseResultFunction[R]) {
    this(ApiType.java, prop)
    this.javaQueryFunc = queryFunc
    this.javaResultFunc = resultFunc
  }

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
  }

  override def run(ctx: SourceContext[R]): Unit = {
    while (running) {
      ctx.getCheckpointLock.synchronized {
        //将上次(或者从checkpoint中恢复)的query查询对象返回用户,用户根据这个构建下次要查询的条件.
        query = apiType match {
          case ApiType.scala => scalaQueryFunc(last)
          case ApiType.java => javaQueryFunc.query(last)
        }
        require(query != null && query.getTable != null, "[StreamX] HBaseSource query and query's param table muse be not null ")
        table = query.getTable(prop)
        table.getScanner(query).foreach(x => {
          apiType match {
            case ApiType.scala =>
              last = scalaResultFunc(x)
              ctx.collectWithTimestamp(last, System.currentTimeMillis())
            case ApiType.java =>
              last = javaResultFunc.result(x)
              ctx.collectWithTimestamp(last, System.currentTimeMillis())
          }
        })
      }
    }
  }

  override def cancel(): Unit = this.running = false

  override def close(): Unit = {
    super.close()
    if (table != null) {
      table.close()
    }
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    if (running) {
      state.clear()
      if (last != null) {
        state.add(last)
      }
    } else {
      logError("HBaseSource snapshotState called on closed source")
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    //从checkpoint中恢复...
    logInfo("HBaseSource snapshotState initialize")
    state = FlinkUtils.getUnionListState[R](context, OFFSETS_STATE_NAME)
    Try(state.get.head) match {
      case Success(q) => last = q
      case _ =>
    }
  }

  override def notifyCheckpointComplete(checkpointId: Long): Unit = {
    logInfo(s"HBaseSource checkpointComplete: $checkpointId")
  }

}
