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

import com.streamxhub.common.util.Logger
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.runtime.state.FunctionInitializationContext
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}
import org.apache.flink.streaming.api.scala.DataStream

import scala.annotation.meta.param
import scala.collection.mutable.ListBuffer

/**
 * println()升级版.精准一次的打印
 */
object EchoSink {

  def apply[T](@(transient@param) stream: DataStream[T], name: String): DataStreamSink[T] = {
    stream.addSink(new EchoSinkFunction[T](name))
  }

}

class EchoSinkFunction[T](name: String) extends TwoPhaseCommitSinkFunction[T, Echo, Void](new KryoSerializer[Echo](classOf[Echo], new ExecutionConfig), VoidSerializer.INSTANCE)
  with Logger {

  private val SINK_2PC_STATE = "echo-sink-2pc-state"

  @transient private[this] var transactionState: ListState[Echo] = _

  override def beginTransaction(): Echo = Echo()

  override def invoke(echo: Echo, in: T, context: SinkFunction.Context[_]): Unit = {
    echo.index = getRuntimeContext.getIndexOfThisSubtask
    echo.name = name
    echo.list + in.toString
  }

  override def preCommit(txn: Echo): Unit = {
    if (txn.list.nonEmpty) {
      transactionState.add(txn)
    }
  }

  override def commit(txn: Echo): Unit = {
    val prefix = if (name == null || name.trim.isEmpty) txn.index.toString else name
    if (txn.list.nonEmpty) {
      txn.list.foreach(x => println(s"$prefix > $x"))
      transactionState.clear()
    }
  }

  override def abort(txn: Echo): Unit = transactionState.clear()

  override def initializeState(context: FunctionInitializationContext): Unit = {
    transactionState = context.getOperatorStateStore.getListState(new ListStateDescriptor(SINK_2PC_STATE, classOf[Echo]))
    super.initializeState(context)
  }

}

case class Echo(var index: Int = null, var list: ListBuffer[String] = ListBuffer.empty[String], var name: String = "") extends Serializable
