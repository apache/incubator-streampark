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

package org.apache.streampark.flink.connector.elasticsearch7.sink

import org.apache.streampark.common.conf.ConfigConst._
import org.apache.streampark.common.util.{ConfigUtils, Logger, Utils}
import org.apache.streampark.flink.connector.elasticsearch7.bean.RestClientFactoryImpl
import org.apache.streampark.flink.connector.elasticsearch7.conf.ES7Config
import org.apache.streampark.flink.connector.elasticsearch7.internal.ESSinkFunction
import org.apache.streampark.flink.connector.function.TransformFunction
import org.apache.streampark.flink.connector.sink.Sink
import org.apache.streampark.flink.core.scala.StreamingContext

import org.apache.flink.streaming.api.datastream.{DataStream => JavaDataStream, DataStreamSink}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkBase.{CONFIG_KEY_BULK_FLUSH_BACKOFF_DELAY, CONFIG_KEY_BULK_FLUSH_BACKOFF_ENABLE, CONFIG_KEY_BULK_FLUSH_BACKOFF_RETRIES, CONFIG_KEY_BULK_FLUSH_BACKOFF_TYPE, CONFIG_KEY_BULK_FLUSH_INTERVAL_MS, CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS, CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB, FlushBackoffType}
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch7.{ElasticsearchSink, RestClientFactory}
import org.apache.http.HttpHost
import org.elasticsearch.action.ActionRequest

import java.util.Properties

import scala.annotation.meta.param
import scala.collection.JavaConversions._

object ES7Sink {
  def apply(
      @(transient @param)
      property: Properties = new Properties(),
      parallelism: Int = 0,
      name: String = null,
      uid: String = null)(implicit ctx: StreamingContext): ES7Sink =
    new ES7Sink(ctx, property, parallelism, name, uid)
}

class ES7Sink(
    @(transient @param) ctx: StreamingContext,
    property: Properties = new Properties(),
    parallelism: Int = 0,
    name: String = null,
    uid: String = null)
  extends Sink
  with Logger {

  def this(ctx: StreamingContext) {
    this(ctx, new Properties(), 0, null, null)
  }

  val prop: Properties = ctx.parameter.getProperties

  Utils.copyProperties(property, prop)

  private val config: ES7Config = new ES7Config(prop)

  private def process[T](
      stream: DataStream[T],
      restClientFactory: Option[RestClientFactory],
      failureHandler: ActionRequestFailureHandler,
      f: T => ActionRequest): DataStreamSink[T] = {
    require(stream != null, "sink Stream must not null")
    require(f != null, "es process element func must not null")
    val sinkFunc: ESSinkFunction[T] = new ESSinkFunction(f)
    val esSink: ElasticsearchSink[T] = buildESSink(restClientFactory, failureHandler, sinkFunc)
    if (config.disableFlushOnCheckpoint) {
      esSink.disableFlushOnCheckpoint()
    }
    val sink = stream.addSink(esSink)
    afterSink(sink, parallelism, name, uid)
  }

  private def process[T](
      stream: JavaDataStream[T],
      restClientFactory: Option[RestClientFactory],
      failureHandler: ActionRequestFailureHandler,
      f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    require(stream != null, "sink Stream must not null")
    require(f != null, "es process element func must not null")
    val sinkFunc: ESSinkFunction[T] = new ESSinkFunction(f)
    val esSink: ElasticsearchSink[T] = buildESSink(restClientFactory, failureHandler, sinkFunc)
    if (config.disableFlushOnCheckpoint) {
      esSink.disableFlushOnCheckpoint()
    }
    val sink = stream.addSink(esSink)
    afterSink(sink, parallelism, name, uid)
  }

  private def buildESSink[T](
      restClientFactory: Option[RestClientFactory],
      failureHandler: ActionRequestFailureHandler,
      sinkFunc: ESSinkFunction[T]): ElasticsearchSink[T] = {
    val sinkBuilder = new ElasticsearchSink.Builder[T](config.host, sinkFunc)
    sinkBuilder.setFailureHandler(failureHandler)
    restClientFactory match {
      case Some(factory) =>
        sinkBuilder.setRestClientFactory(factory)
      case None =>
        sinkBuilder.setRestClientFactory(new RestClientFactoryImpl(config))
    }

    def doConfig(param: (String, String)): Unit = param match {
      // parameter of sink.es.bulk.flush.max.actions
      case (CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS, v) => sinkBuilder.setBulkFlushMaxActions(v.toInt)
      // parameter of sink.es.bulk.flush.max.size.mb
      case (CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB, v) => sinkBuilder.setBulkFlushMaxSizeMb(v.toInt)
      // parameter of sink.es.bulk.flush.interval.ms
      case (CONFIG_KEY_BULK_FLUSH_INTERVAL_MS, v) => sinkBuilder.setBulkFlushInterval(v.toInt)
      // parameter of sink.es.bulk.flush.backoff.enable
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_ENABLE, v) => sinkBuilder.setBulkFlushBackoff(v.toBoolean)
      // parameter of sink.es.bulk.flush.backoff.type value of [ CONSTANT or EXPONENTIAL ]
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_TYPE, v) =>
        sinkBuilder.setBulkFlushBackoffType(FlushBackoffType.valueOf(v))
      // parameter of sink.es.bulk.flush.backoff.retries
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_RETRIES, v) =>
        sinkBuilder.setBulkFlushBackoffRetries(v.toInt)
      // parameter of sink.es.bulk.flush.backoff.delay
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_DELAY, v) =>
        sinkBuilder.setBulkFlushBackoffDelay(v.toLong)
      // other...
      case _ =>
    }
    // set value from properties
    config.sinkOption.getInternalConfig().foreach(doConfig)
    sinkBuilder.build()
  }

  def sink[T](
      stream: DataStream[T],
      restClientFactory: Option[RestClientFactory] = None,
      failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)(
      implicit f: T => ActionRequest): DataStreamSink[T] = {
    process(stream, restClientFactory, failureHandler, f)
  }

  def sink[T](
      stream: JavaDataStream[T],
      restClientFactory: RestClientFactory,
      failureHandler: ActionRequestFailureHandler,
      f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(stream, Some(restClientFactory), failureHandler, f)
  }

  def sink[T](
      stream: JavaDataStream[T],
      restClientFactory: RestClientFactory,
      f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(stream, Some(restClientFactory), new RetryRejectedExecutionFailureHandler, f)
  }

  def sink[T](
      stream: JavaDataStream[T],
      f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(stream, None, new RetryRejectedExecutionFailureHandler, f)
  }
}
