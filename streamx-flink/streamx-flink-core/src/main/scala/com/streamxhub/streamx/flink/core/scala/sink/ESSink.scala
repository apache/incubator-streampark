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

package com.streamxhub.streamx.flink.core.scala.sink

import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch6.RestClientFactory
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.update.UpdateRequest

import java.util.Properties
import scala.annotation.meta.param

object ESSink {

  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): ESSink = new ESSink(ctx, property, parallelism, name, uid)

}

class ESSink(@(transient@param) context: StreamingContext,
             property: Properties = new Properties(),
             parallelism: Int = 0,
             name: String = null,
             uid: String = null) {

  /**
   * for ElasticSearch6....
   *
   * @param stream
   * @param suffix
   * @param restClientFactory
   * @param failureHandler
   * @param f
   * @tparam T
   * @return
   */
  def sink6[T](stream: DataStream[T],
               suffix: String = "",
               restClientFactory: RestClientFactory = null,
               failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)
              (implicit f: T => IndexRequest): DataStreamSink[T] = {

    new ES6Sink(context, property, parallelism, name, uid).sink[T](stream, suffix, restClientFactory, failureHandler, f)
  }


  /**
   * for ElasticSearch6....
   *
   * @param stream
   * @param suffix
   * @param restClientFactory
   * @param failureHandler
   * @param f
   * @tparam T
   * @return
   */
  def update6[T](stream: DataStream[T],
               suffix: String = "",
               restClientFactory: RestClientFactory = null,
               failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)
              ( f: T => UpdateRequest): DataStreamSink[T] = {

    new ES6Sink(context, property, parallelism, name, uid).sink[T](stream, suffix, restClientFactory, failureHandler, f)
  }


  /**
   * for ElasticSearch6....
   *
   * @param stream
   * @param suffix
   * @param restClientFactory
   * @param failureHandler
   * @param f
   * @tparam T
   * @return
   */
  def delete6[T](stream: DataStream[T],
               suffix: String = "",
               restClientFactory: RestClientFactory = null,
               failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)
              ( f: T => DeleteRequest): DataStreamSink[T] = {

    new ES6Sink(context, property, parallelism, name, uid).sink[T](stream, suffix, restClientFactory, failureHandler, f)
  }

}
