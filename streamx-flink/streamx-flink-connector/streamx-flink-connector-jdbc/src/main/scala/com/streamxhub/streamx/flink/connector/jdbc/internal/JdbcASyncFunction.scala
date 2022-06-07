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

package com.streamxhub.streamx.flink.connector.jdbc.internal

import com.streamxhub.streamx.common.util.{JdbcUtils, Logger}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.async.{ResultFuture, RichAsyncFunction}

import java.util.Properties
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}
import java.util.function.{Consumer, Supplier}

/**
 * 基于线程池实现
 *
 * @param sqlFun
 * @param resultFun
 * @param jdbc
 * @tparam T
 * @tparam R
 */

class JdbcASyncFunction[T: TypeInformation, R: TypeInformation](sqlFun: T => String, resultFun: (T, Map[String, _]) => R, jdbc: Properties, capacity: Int = 10) extends RichAsyncFunction[T, R] with Logger {

  @transient private[this] var executorService: ExecutorService = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    executorService = Executors.newFixedThreadPool(capacity)
  }

  override def close(): Unit = {
    super.close()
    if (!executorService.isShutdown) {
      executorService.shutdown()
    }
  }

  @throws[Exception]
  def asyncInvoke(input: T, resultFuture: ResultFuture[R]): Unit = {
    CompletableFuture.supplyAsync(new Supplier[Iterable[Map[String, _]]] {
      override def get(): Iterable[Map[String, _]] = JdbcUtils.select(sqlFun(input))(jdbc)
    }, executorService).thenAccept(new Consumer[Iterable[Map[String, _]]] {
      override def accept(result: Iterable[Map[String, _]]): Unit = {
        val list = result.toList
        if (list.isEmpty) {
          resultFuture.complete(List(resultFun(input, Map.empty[String, Any])))
        } else {
          resultFuture.complete(list.map(x => resultFun(input, x)))
        }
      }
    })

  }

  override def timeout(input: T, resultFuture: ResultFuture[R]): Unit = {
    logWarn("JdbcASync request timeout. retrying... ")
    asyncInvoke(input, resultFuture)
  }
}
