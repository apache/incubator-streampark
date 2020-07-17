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
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, TimeUnit}
import java.util.function.{Consumer, Supplier}

import com.streamxhub.common.util.{HBaseClient, Logger}
import com.streamxhub.flink.core.wrapper.HBaseQuery
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.shaded.guava18.com.google.common.cache.Cache
import org.apache.flink.streaming.api.scala.async.{ResultFuture, RichAsyncFunction}
import org.apache.flink.streaming.api.scala.{AsyncDataStream, DataStream, async}
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.{Connection, Result, ResultScanner, Table}

import scala.collection.JavaConversions._
import scala.annotation.meta.param


object HBaseRequest {

  def apply[T: TypeInformation](@(transient@param) stream: DataStream[T], overrideParams: Map[String, String] = Map.empty[String, String]): HBaseRequest[T] = new HBaseRequest[T](stream, overrideParams)

}


class HBaseRequest[T: TypeInformation](@(transient@param) private val stream: DataStream[T], overrideParams: Map[String, String] = Map.empty[String, String]) {

  /**
   *
   * @param tableName
   * @param queryFunc
   * @param resultFunc
   * @param timeout
   * @param capacity
   * @param prop
   * @tparam R
   * @return
   */
  def requestOrdered[R: TypeInformation](tableName: String, queryFunc: T => HBaseQuery, resultFunc: Result => R, timeout: Long = 1000, capacity: Int = 10)(implicit prop: Properties): DataStream[R] = {
    val async = new HBaseAsyncFunction[T, R](tableName, prop, queryFunc, resultFunc, capacity)
    AsyncDataStream.orderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

  /**
   *
   * @param tableName
   * @param queryFunc
   * @param resultFunc
   * @param timeout
   * @param capacity
   * @param prop
   * @tparam R
   * @return
   */
  def requestUnordered[R: TypeInformation](tableName: String, queryFunc: T => HBaseQuery, resultFunc: Result => R, timeout: Long = 1000, capacity: Int = 10)(implicit prop: Properties): DataStream[R] = {
    val async = new HBaseAsyncFunction[T, R](tableName, prop, queryFunc, resultFunc, capacity)
    AsyncDataStream.unorderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

}

class HBaseAsyncFunction[T: TypeInformation, R: TypeInformation](tableName: String, prop: Properties, queryFunc: T => HBaseQuery, resultFunc: Result => R, capacity: Int) extends RichAsyncFunction[T, R] with Logger {
  @transient var table: Table = _
  @transient var executorService: ExecutorService = _

  override def open(parameters: Configuration): Unit = {
    table = HBaseClient(prop).table(tableName)
    executorService = Executors.newFixedThreadPool(capacity)
  }

  override def asyncInvoke(input: T, resultFuture: async.ResultFuture[R]): Unit = {
    CompletableFuture.supplyAsync(new Supplier[ResultScanner]() {
      override def get(): ResultScanner = table.getScanner(queryFunc(input))
    }).thenAccept(new Consumer[ResultScanner] {
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