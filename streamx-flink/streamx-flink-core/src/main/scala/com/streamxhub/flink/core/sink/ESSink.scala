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

import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler
import com.streamxhub.flink.core.StreamingContext
import org.elasticsearch.action.index.IndexRequest

import scala.collection.Map

object ESSink {

  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null): ESSink = new ESSink(ctx, overwriteParams, parallelism, name, uid)

}

class ESSink(@transient context: StreamingContext,
             overwriteParams: Map[String, String] = Map.empty[String, String],
             parallelism: Int = 0,
             name: String = null,
             uid: String = null) {

  /**
   * for ElasticSearch5....
   *
   * @param stream
   * @param suffix
   * @param failureHandler
   * @param f
   * @tparam T
   * @return
   */
  def sink5[T](stream: DataStream[T],
               suffix: String = "",
               failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)
              (implicit f: T => IndexRequest): DataStreamSink[T] = {

    //TODO....
    null
    //new sink5(context, overwriteParams, parallelism, uidHash).sink[T](stream, suffix, failureHandler)(f)
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
  def sink6[T](stream: DataStream[T],
               suffix: String = "",
               restClientFactory: Any = null,
               failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)
              (implicit f: T => IndexRequest): DataStreamSink[T] = {

    new ES6Sink(context, overwriteParams, parallelism, name, uid).sink[T](stream, suffix, failureHandler)(f)
  }

}
