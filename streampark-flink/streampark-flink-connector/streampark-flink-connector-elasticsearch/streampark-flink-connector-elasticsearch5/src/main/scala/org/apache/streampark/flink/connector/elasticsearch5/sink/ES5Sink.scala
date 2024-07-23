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

package org.apache.streampark.flink.connector.elasticsearch5.sink

import org.apache.streampark.common.util.{Logger, Utils}
import org.apache.streampark.flink.connector.elasticsearch5.conf.ESConfig
import org.apache.streampark.flink.connector.elasticsearch5.internal.ESSinkFunction
import org.apache.streampark.flink.connector.function.TransformFunction
import org.apache.streampark.flink.connector.sink.Sink
import org.apache.streampark.flink.core.scala.StreamingContext

import org.apache.flink.streaming.api.datastream.{DataStream => JavaDataStream, DataStreamSink}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch5.ElasticsearchSink
import org.elasticsearch.action.ActionRequest

import java.util.Properties

import scala.annotation.meta.param
import org.apache.streampark.common.util.Implicits._

object ES5Sink {

  val functionNullHintMsg = "ES pocess element func must not null"
  val sinkNullHintMsg = "Sink Stream must not null"

  def apply(
      @(transient @param)
      property: Properties = new Properties(),
      parallelism: Int = 0,
      name: String = null,
      uid: String = null)(implicit ctx: StreamingContext): ES5Sink =
    new ES5Sink(ctx, property, parallelism, name, uid)

}

class ES5Sink(
    @(transient @param) ctx: StreamingContext,
    property: Properties = new Properties(),
    parallelism: Int = 0,
    name: String = null,
    uid: String = null,
    alias: String = "")
  extends Sink
  with Logger {

  val prop: Properties = ctx.parameter.getProperties

  Utils.copyProperties(property, prop)

  def this(ctx: StreamingContext) {
    this(ctx, new Properties(), 0, null, null, "")
  }

  private val config: ESConfig = new ESConfig(prop)

  private def process[T](
      userConfig: JavaMap[String, String],
      stream: JavaDataStream[T],
      failureHandler: ActionRequestFailureHandler,
      f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    require(stream != null, () => sinkNullHintMsg)
    require(f != null, () => functionNullHintMsg)
    val esSink: ElasticsearchSink[T] =
      new ElasticsearchSink(userConfig, config.host, new ESSinkFunction(f), failureHandler)
    if (config.disableFlushOnCheckpoint) {
      esSink.disableFlushOnCheckpoint()
    }
    val sink = stream.addSink(esSink)
    afterSink(sink, parallelism, name, uid)
  }

  private def process[T](
      userConfig: JavaMap[String, String],
      stream: DataStream[T],
      failureHandler: ActionRequestFailureHandler,
      f: T => ActionRequest): DataStreamSink[T] = {
    require(stream != null, () => sinkNullHintMsg)
    require(f != null, () => functionNullHintMsg)
    val esSink: ElasticsearchSink[T] =
      new ElasticsearchSink(userConfig, config.host, new ESSinkFunction(f), failureHandler)
    if (config.disableFlushOnCheckpoint) {
      esSink.disableFlushOnCheckpoint()
    }
    val sink = stream.addSink(esSink)
    afterSink(sink, parallelism, name, uid)
  }

  /**
   * @param stream
   * @param suffix
   * @param restClientFactory
   * @param failureHandler
   * @param f
   * @tparam T
   * @return
   */
  def sink[T](
      userConfig: JavaMap[String, String],
      stream: DataStream[T],
      failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)(
      implicit f: T => ActionRequest): DataStreamSink[T] = {
    process(userConfig, stream, failureHandler, f)
  }

  def sink[T](
      userConfig: JavaMap[String, String],
      stream: JavaDataStream[T],
      failureHandler: ActionRequestFailureHandler,
      f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(userConfig, stream, failureHandler, f)
  }

  def sink[T](
      userConfig: JavaMap[String, String],
      stream: JavaDataStream[T],
      f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(userConfig, stream, new RetryRejectedExecutionFailureHandler, f)
  }
}
