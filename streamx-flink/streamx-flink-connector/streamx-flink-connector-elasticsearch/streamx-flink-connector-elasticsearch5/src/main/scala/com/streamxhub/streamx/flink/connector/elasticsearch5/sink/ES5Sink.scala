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

package com.streamxhub.streamx.flink.connector.elasticsearch5.sink

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{ConfigUtils, Logger, Utils}
import com.streamxhub.streamx.flink.connector.elasticsearch5.internal.ESSinkFunction
import com.streamxhub.streamx.flink.connector.function.TransformFunction
import com.streamxhub.streamx.flink.connector.sink.Sink
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.streaming.api.datastream.{DataStreamSink, DataStream => JavaDataStream}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch5.ElasticsearchSink
import org.elasticsearch.action.ActionRequest

import java.net.InetSocketAddress
import java.util.Properties
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.mutable


object ES5Sink {

  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): ES5Sink = new ES5Sink(ctx, property, parallelism, name, uid)

}


class ES5Sink(@(transient@param) ctx: StreamingContext,
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

  private def process[T](stream: JavaDataStream[T],
                         suffix: String,
                         failureHandler: ActionRequestFailureHandler,
                         f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    require(stream != null, () => s"sink Stream must not null")
    require(f != null, () => s"es pocess element fun  must not null")
    val (shortConfig: _root_.scala.collection.mutable.Map[_root_.scala.Predef.String, _root_.java.lang.String],
    addresses: _root_.scala.Array[_root_.java.net.InetSocketAddress]) = initProp(suffix)

    val esSink: ElasticsearchSink[T] = new ElasticsearchSink(shortConfig, addresses.toList, new ESSinkFunction(f), failureHandler)
    if (shortConfig.getOrElse(KEY_ES_DISABLE_FLUSH_ONCHECKPOINT, "false").toBoolean) {
      esSink.disableFlushOnCheckpoint()
    }
    val sink = stream.addSink(esSink)
    afterSink(sink, parallelism, name, uid)
  }

  private def process[T](stream: DataStream[T],
                         suffix: String,
                         failureHandler: ActionRequestFailureHandler,
                         f: T => ActionRequest): DataStreamSink[T] = {
    require(stream != null, () => s"sink Stream must not null")
    require(f != null, () => s"es pocess element fun  must not null")
    val (shortConfig: _root_.scala.collection.mutable.Map[_root_.scala.Predef.String, _root_.java.lang.String],
    addresses: _root_.scala.Array[_root_.java.net.InetSocketAddress]) = initProp(suffix)

    val esSink: ElasticsearchSink[T] = new ElasticsearchSink(shortConfig, addresses.toList, new ESSinkFunction(f), failureHandler)
    if (shortConfig.getOrElse(KEY_ES_DISABLE_FLUSH_ONCHECKPOINT, "false").toBoolean) {
      esSink.disableFlushOnCheckpoint()
    }
    val sink = stream.addSink(esSink)
    afterSink(sink, parallelism, name, uid)
  }

  private def initProp[T](suffix: String): (mutable.Map[String, String], Array[InetSocketAddress]) = {
    //当前实例(默认,或者指定后缀实例)的配置文件...
    val shortConfig = prop
      .filter(_._1.endsWith(suffix))
      .map(x => x._1.drop(ES_PREFIX.length + suffix.length) -> x._2.trim)

    // parameter of sink.es.host
    val addresses = shortConfig.getOrElse(KEY_HOST, SIGN_EMPTY).split(SIGN_COMMA).map(x => {
      x.split(SIGN_COLON) match {
        case Array(host, port) => new InetSocketAddress(host, port.toInt)
      }
    })
    require(addresses.nonEmpty, "elasticsearch config error,please check, e.g: sink.es.host=$host1:$port1,$host2:$port2")
    (shortConfig, addresses)
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
              failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)
             (implicit f: T => ActionRequest): DataStreamSink[T] = {
    process(stream, suffix, failureHandler, f)
  }

  def sink[T](stream: JavaDataStream[T],
              suffix: String,
              failureHandler: ActionRequestFailureHandler,
              f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(stream, suffix, failureHandler, f)
  }

  def sink[T](stream: JavaDataStream[T],
              suffix: String,
              f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(stream, suffix, new RetryRejectedExecutionFailureHandler, f)
  }

  def sink[T](stream: JavaDataStream[T],
              f: TransformFunction[T, ActionRequest]): DataStreamSink[T] = {
    process(stream, "", new RetryRejectedExecutionFailureHandler, f)
  }


}


