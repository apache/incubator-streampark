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

import java.io.Serializable

import com.streamxhub.common.util.Logger
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.functions.util.PrintSinkOutputWriter
import org.apache.flink.api.common.typeutils.base.VoidSerializer
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{SinkFunction, TwoPhaseCommitSinkFunction}
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext
import org.apache.flink.streaming.api.scala.DataStream

import scala.annotation.meta.param
import scala.collection.mutable.ListBuffer

/**
 * println()升级版.精准一次的打印
 */
object EchoSink {

  def apply[T](@(transient@param) stream: DataStream[T], sinkIdentifier: String): DataStreamSink[T] = {
    stream.addSink(new EchoSinkFunction[T](sinkIdentifier)).name("Echo to Std. Out")
  }

}

/**
 * @param sinkIdentifier
 * @tparam T
 */
class EchoSinkFunction[T](sinkIdentifier: String) extends TwoPhaseCommitSinkFunction[T, Echo[T], Void](new KryoSerializer[Echo[T]](classOf[Echo[T]], new ExecutionConfig), VoidSerializer.INSTANCE)
  with Logger {

  private[this] val buffer: collection.mutable.Map[String, Echo[T]] = collection.mutable.Map.empty

  private[this] val writer: PrintSinkOutputWriter[T] = new PrintSinkOutputWriter[T](sinkIdentifier, false)

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    val ctx = getRuntimeContext.asInstanceOf[StreamingRuntimeContext]
    writer.open(ctx.getIndexOfThisSubtask, ctx.getNumberOfParallelSubtasks)
  }

  override def beginTransaction(): Echo[T] = new Echo[T]()

  override def invoke(transaction: Echo[T], value: T, context: SinkFunction.Context[_]): Unit = {
    transaction.invoked = true
    transaction.add(value)
  }

  override def preCommit(transaction: Echo[T]): Unit = {
    if (transaction.invoked) {
      buffer += (transaction.transactionId -> transaction)
    }
  }

  override def commit(transaction: Echo[T]): Unit = {
    if (transaction.invoked) {
      /**
       * 此步骤理论上讲有发生异常的可能,如循环到一半机器挂了.会导致已经输出的打印无法被撤回,下面的清理也无法完成,出现重复打印的情况....
       */
      transaction.buffer.foreach(writer.write)
      //提交完成清空...
      buffer -= transaction.transactionId
    }
  }

  override def abort(transaction: Echo[T]): Unit = {
    buffer -= transaction.transactionId
  }
}

case class Echo[T](transactionId: String = System.currentTimeMillis().toString, buffer: ListBuffer[T] = ListBuffer.empty[T], var invoked: Boolean = false) extends Serializable {
  def add(value: T): Unit = buffer += value
}