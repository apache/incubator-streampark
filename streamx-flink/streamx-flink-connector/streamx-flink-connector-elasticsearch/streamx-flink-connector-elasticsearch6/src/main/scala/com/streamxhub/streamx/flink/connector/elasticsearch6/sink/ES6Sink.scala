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

package com.streamxhub.streamx.flink.connector.elasticsearch6.sink

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{ConfigUtils, Logger, Utils}
import com.streamxhub.streamx.flink.connector.elasticsearch6.bean.RestClientFactoryImpl
import com.streamxhub.streamx.flink.connector.function.TransformFunction
import com.streamxhub.streamx.flink.connector.elasticsearch6.internal.ESSinkFunction
import com.streamxhub.streamx.flink.connector.sink.Sink
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.streaming.api.datastream.{DataStreamSink, DataStream => JavaDataStream}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkBase._
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch6.{ElasticsearchSink, RestClientFactory}
import org.apache.http.HttpHost
import org.elasticsearch.action.ActionRequest

import java.util.Properties
import java.util.function.BiConsumer
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.mutable


object ES6Sink {

  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): ES6Sink = new ES6Sink(ctx, property, parallelism, name, uid)

}


class ES6Sink(@(transient@param) ctx: StreamingContext,
              property: Properties = new Properties(),
              parallelism: Int = 0,
              name: String = null,
              uid: String = null,
              alias: String = "") extends Sink with Logger {

  val prop = ConfigUtils.getConf(ctx.parameter.toMap, ES_PREFIX)(alias)

  Utils.copyProperties(property, prop)

  def this(ctx: StreamingContext) {
    this(ctx, new Properties(), 0, null, null, "")
  }

  private def initProp[T](suffix: String): (mutable.Map[String, String], Array[HttpHost]) = {
    //当前实例(默认,或者指定后缀实例)的配置文件...
    val shortConfig = prop
      .filter(_._1.endsWith(suffix))
      .map(x => x._1.drop(ES_PREFIX.length + suffix.length) -> x._2.trim)

    val httpHosts = shortConfig.getOrElse(KEY_HOST, SIGN_EMPTY).split(SIGN_COMMA).map(x => {
      x.split(SIGN_COLON) match {
        case Array(host, port) => new HttpHost(host, port.toInt)
      }
    })
    require(httpHosts.nonEmpty, "elasticsearch config error,please check, e.g: sink.es.host=$host1:$port1,$host2:$port2")
    (shortConfig, httpHosts)
  }

  private def process[T](stream: DataStream[T],
                         suffix: String,
                         restClientFactory: RestClientFactory,
                         failureHandler: ActionRequestFailureHandler,
                         f: T => ActionRequest): DataStreamSink[T] = {
    require(stream != null, () => s"sink Stream must not null")
    require(f != null, () => s"es pocess element fun  must not null")

    val (shortConfig: mutable.Map[String, String], httpHosts: Array[HttpHost]) = initProp(suffix)

    val sinkFunc: ESSinkFunction[T] = new ESSinkFunction(f)

    val esSink: _root_.org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink[T] = buildESSink(restClientFactory, failureHandler, shortConfig, httpHosts, sinkFunc)
    if (shortConfig.getOrElse(KEY_ES_DISABLE_FLUSH_ONCHECKPOINT, "false").toBoolean) {
      esSink.disableFlushOnCheckpoint()
    }
    val sink = stream.addSink(esSink)
    afterSink(sink, parallelism, name, uid)
  }

  private def process[T](stream: JavaDataStream[T],
                         suffix: String,
                         restClientFactory: RestClientFactory,
                         failureHandler: ActionRequestFailureHandler,
                         f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    require(stream != null, () => s"sink Stream must not null")
    require(f != null, () => s"es pocess element fun  must not null")

    val (shortConfig: mutable.Map[String, String], httpHosts: Array[HttpHost]) = initProp(suffix)

    val sinkFunc: ESSinkFunction[T] = new ESSinkFunction(f)

    val esSink: _root_.org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink[T] = buildESSink(restClientFactory, failureHandler, shortConfig, httpHosts, sinkFunc)
    if (shortConfig.getOrElse(KEY_ES_DISABLE_FLUSH_ONCHECKPOINT, "false").toBoolean) {
      esSink.disableFlushOnCheckpoint()
    }
    val sink = stream.addSink(esSink)
    afterSink(sink, parallelism, name, uid)
  }

  private def buildESSink[T](restClientFactory: RestClientFactory, failureHandler: ActionRequestFailureHandler, shortConfig: mutable.Map[String, String], httpHosts: Array[HttpHost], sinkFunc: ESSinkFunction[T]): ElasticsearchSink[T] = {
    val sinkBuilder = new ElasticsearchSink.Builder[T](httpHosts.toList, sinkFunc)
    // failureHandler
    sinkBuilder.setFailureHandler(failureHandler)
    //restClientFactory
    if (restClientFactory == null) {
      val restClientFactory = new RestClientFactoryImpl(prop)
      sinkBuilder.setRestClientFactory(restClientFactory)
    } else {
      sinkBuilder.setRestClientFactory(restClientFactory)
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
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_TYPE, v) => sinkBuilder.setBulkFlushBackoffType(FlushBackoffType.valueOf(v))
      // parameter of sink.es.bulk.flush.backoff.retries
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_RETRIES, v) => sinkBuilder.setBulkFlushBackoffRetries(v.toInt)
      // parameter of sink.es.bulk.flush.backoff.delay
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_DELAY, v) => sinkBuilder.setBulkFlushBackoffDelay(v.toLong)
      // other...
      case _ =>
    }
    //set value from properties
    shortConfig.filter(_._1.startsWith(KEY_ES_BULK_PREFIX)).foreach(doConfig)
    //set value from method parameter...
    property.forEach(new BiConsumer[Object, Object] {
      override def accept(k: Object, v: Object): Unit = doConfig(k.toString, v.toString)
    })

    val esSink: ElasticsearchSink[T] = sinkBuilder.build()
    esSink
  }


  /**
   *
   * @param stream
   * @param suffix
   * @param restClientFactory
   * @param failureHandler
   * @param f
   * @tparam T
   * @return
   */
  def sink[T](stream: DataStream[T],
              suffix: String = "",
              restClientFactory: RestClientFactory,
              failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)
             (implicit f: T => ActionRequest): DataStreamSink[T] = {
    process(stream, suffix, restClientFactory, failureHandler, f)
  }

  def sink[T](stream: JavaDataStream[T],
              suffix: String,
              restClientFactory: RestClientFactory,
              failureHandler: ActionRequestFailureHandler,
              f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(stream, suffix, restClientFactory, failureHandler, f)
  }

  def sink[T](stream: JavaDataStream[T],
              suffix: String,
              f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(stream, suffix, null, new RetryRejectedExecutionFailureHandler, f)
  }

  def sink[T](stream: JavaDataStream[T],
              f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(stream, "", null, new RetryRejectedExecutionFailureHandler, f)
  }


}


