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
package com.streamxhub.flink.core.request


import java.util.Properties
import java.util.concurrent.{CompletableFuture, ExecutorService, TimeUnit, Executors => JExec}
import java.util.function.{Consumer, Supplier}

import com.streamxhub.common.util.Logger
import com.streamxhub.flink.core.wrapper.HBaseQuery
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.concurrent.Executors
import org.apache.flink.streaming.api.scala.async.{ResultFuture, RichAsyncFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.hadoop.hbase.client.{Result, ResultScanner, Table}

import scala.collection.JavaConversions._
import scala.annotation.meta.param
import scala.concurrent.ExecutionContext


object HBaseRequest {

  def apply[T: TypeInformation](@(transient@param) stream: DataStream[T], overrideParams: Map[String, String] = Map.empty[String, String]): HBaseRequest[T] = new HBaseRequest[T](stream, overrideParams)

}


class HBaseRequest[T: TypeInformation](@(transient@param) private val stream: DataStream[T], overrideParams: Map[String, String] = Map.empty[String, String]) {

  /**
   *
   * @param queryFunc
   * @param resultFunc
   * @param timeout
   * @param capacity
   * @param prop
   * @tparam R
   * @return
   */
  def requestOrdered[R: TypeInformation](queryFunc: T => HBaseQuery, resultFunc: Result => R, timeout: Long = 1000, capacity: Int = 10)(implicit prop: Properties): DataStream[R] = {
    val async = new HBaseAsyncFunction[T, R](prop, queryFunc, resultFunc, capacity)
    AsyncDataStream.orderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

  /**
   *
   * @param queryFunc
   * @param resultFunc
   * @param timeout
   * @param capacity
   * @param prop
   * @tparam R
   * @return
   */
  def requestUnordered[R: TypeInformation](queryFunc: T => HBaseQuery, resultFunc: Result => R, timeout: Long = 1000, capacity: Int = 10)(implicit prop: Properties): DataStream[R] = {
    val async = new HBaseAsyncFunction[T, R](prop, queryFunc, resultFunc, capacity)
    AsyncDataStream.unorderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

}

class HBaseAsyncFunction[T: TypeInformation, R: TypeInformation](prop: Properties, queryFunc: T => HBaseQuery, resultFunc: Result => R, capacity: Int) extends RichAsyncFunction[T, R] with Logger {
  @transient private[this] var table: Table = _
  @transient private[this] var executorService: ExecutorService = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    executorService = JExec.newFixedThreadPool(capacity)
  }

  override def asyncInvoke(input: T, resultFuture: async.ResultFuture[R]): Unit = {
    CompletableFuture.supplyAsync(new Supplier[ResultScanner]() {
      override def get(): ResultScanner = {
        val query = queryFunc(input)
        require(query != null && query.getTable != null, "[StreamX] HBaseRequest query and query's attr table must be not null ")
        table = query.getTable(prop)
        table.getScanner(query)
      }
    }, executorService).thenAccept(new Consumer[ResultScanner] {
      override def accept(result: ResultScanner): Unit = resultFuture.complete(result.map(resultFunc))
    })
  }

  override def timeout(input: T, resultFuture: ResultFuture[R]): Unit = {
    logger.warn("[Streamx] HBaseASync request timeout. retrying... ")
    asyncInvoke(input, resultFuture)
  }

  override def close(): Unit = {
    super.close()
    table.close()
    if (!executorService.isShutdown) {
      executorService.shutdown()
    }
  }
}