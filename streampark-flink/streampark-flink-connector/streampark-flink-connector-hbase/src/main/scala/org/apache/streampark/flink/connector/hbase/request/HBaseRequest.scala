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

package org.apache.streampark.flink.connector.hbase.request

import org.apache.streampark.common.util.{Logger, Utils}
import org.apache.streampark.flink.connector.hbase.bean.HBaseQuery

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.async.{ResultFuture, RichAsyncFunction}
import org.apache.hadoop.hbase.client.{Result, ResultScanner, Table}

import java.util.Properties
import java.util.concurrent.{CompletableFuture, Executors, ExecutorService, TimeUnit}
import java.util.function.{Consumer, Supplier}

import scala.annotation.meta.param
import org.apache.streampark.common.util.Implicits._

object HBaseRequest {

  def apply[T: TypeInformation](
      @(transient @param) stream: DataStream[T],
      property: Properties = new Properties()): HBaseRequest[T] =
    new HBaseRequest[T](stream, property)

}

class HBaseRequest[T: TypeInformation](
    @(transient @param) private val stream: DataStream[T],
    property: Properties = new Properties()) {

  def requestOrdered[R: TypeInformation](
      queryFunc: T => HBaseQuery,
      resultFunc: (T, Result) => R,
      timeout: Long = 1000,
      capacity: Int = 10)(implicit prop: Properties): DataStream[R] = {
    Utils.copyProperties(property, prop)
    val async = new HBaseAsyncFunction[T, R](prop, queryFunc, resultFunc, capacity)
    AsyncDataStream.orderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

  def requestUnordered[R: TypeInformation](
      queryFunc: T => HBaseQuery,
      resultFunc: (T, Result) => R,
      timeout: Long = 1000,
      capacity: Int = 10)(implicit prop: Properties): DataStream[R] = {
    Utils.copyProperties(property, prop)
    val async = new HBaseAsyncFunction[T, R](prop, queryFunc, resultFunc, capacity)
    AsyncDataStream.unorderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

}

class HBaseAsyncFunction[T: TypeInformation, R: TypeInformation](
    prop: Properties,
    queryFunc: T => HBaseQuery,
    resultFunc: (T, Result) => R,
    capacity: Int)
  extends RichAsyncFunction[T, R]
  with Logger {
  @transient private[this] var table: Table = _
  @transient private[this] var executorService: ExecutorService = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    executorService = Executors.newFixedThreadPool(capacity)
  }

  override def asyncInvoke(input: T, resultFuture: async.ResultFuture[R]): Unit = {
    CompletableFuture
      .supplyAsync(
        new Supplier[ResultScanner]() {
          override def get(): ResultScanner = {
            val query = queryFunc(input)
            require(
              query != null && query.getTable != null,
              "[StreamPark] HBaseRequest query and query's attr table must not be null ")
            table = query.getTable(prop)
            table.getScanner(query)
          }
        },
        executorService
      )
      .thenAccept(new Consumer[ResultScanner] {
        override def accept(result: ResultScanner): Unit = {
          val list = result.toList
          if (list.isEmpty) {
            resultFuture.complete(List(resultFunc(input, Result.EMPTY_RESULT)))
          } else {
            resultFuture.complete(list.map(r => resultFunc(input, r)))
          }
        }
      })
  }

  override def timeout(input: T, resultFuture: ResultFuture[R]): Unit = {
    logWarn("HBaseASync request timeout. retrying... ")
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
