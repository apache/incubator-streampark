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
package com.streamxhub.flink.core.source

import java.util.Properties

import com.streamxhub.common.util.Logger
import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.wrapper.HBaseQuery
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{CheckpointListener, FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.RichSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.hadoop.hbase.client._

import scala.annotation.meta.param
import scala.collection.JavaConversions._

object HBaseSource {

  def apply(@(transient@param) ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]): HBaseSource = new HBaseSource(ctx, overrideParams)

}

/*
 * @param ctx
 * @param overrideParams
 */
class HBaseSource(@(transient@param) val ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]) {

  def getDataStream[R: TypeInformation](query: HBaseQuery => HBaseQuery, func: Result => R)(implicit config: Properties): DataStream[R] = {
    overrideParams.foreach(x => config.setProperty(x._1, x._2))
    val hBaseFunc = new HBaseSourceFunction[R](query, func)
    ctx.addSource(hBaseFunc)
  }

}


class HBaseSourceFunction[R: TypeInformation](queryFunc: HBaseQuery => HBaseQuery, func: Result => R)(implicit prop: Properties) extends RichSourceFunction[R] with CheckpointListener with CheckpointedFunction with Logger {

  @volatile private[this] var running = true

  @transient private[this] var table: Table = _

  @transient private var query: HBaseQuery = _

  @transient private var state: ListState[HBaseQuery] = _

  private val OFFSETS_STATE = "offset-states"

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
  }

  override def run(ctx: SourceContext[R]): Unit = {
    while (running) {
      ctx.getCheckpointLock.synchronized {
        //将上次的保存的信息传入...
        query = queryFunc(query)
        require(query != null && query.getTable != null, "[StreamX] HBaseSource query and query's param table muse be not null ")
        table = query.getTable(prop)
        table.getScanner(query).foreach(x => ctx.collect(func(x)))
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
    if (!running) {
      logger.error("[StreamX] HBaseSource snapshotState called on closed source")
    } else {
      state.clear()
      state.add(query)
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    state = context.getOperatorStateStore.getListState(new ListStateDescriptor(OFFSETS_STATE, TypeInformation.of(classOf[HBaseQuery])))
    state.get.foreach(x => query = x)
  }

  override def notifyCheckpointComplete(checkpointId: Long): Unit = {
    logger.info(s"[StreamX] HBaseSource checkpointComplete: $checkpointId")
  }
}


class HQuery(table:String) extends Scan with Serializable{

}