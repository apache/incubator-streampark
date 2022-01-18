/*
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
package com.streamxhub.streamx.test.flink.scala.datastream

import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.sink.ClickHouseSink
import org.apache.flink.api.scala._

object ClickHouseSinkApp extends FlinkStreaming {

  override def handle(): Unit = {
    val createTable =
      """
        |create TABLE test.orders(
        |userId UInt16,
        |orderId UInt16,
        |siteId UInt8,
        |cityId UInt8,
        |orderStatus UInt8,
        |price Float64,
        |quantity UInt8,
        |timestamp UInt16
        |)ENGINE = TinyLog;
        |""".stripMargin

    println(createTable)

    val source = context.addSource(new TestSource)

    var index = 0
    val httpDs = source.map(x => {
      index += 1
      s"""http://www.qq.com?id=$index"""
    })
    source.print()
    //    HttpSink(context).getSink(httpDs).setParallelism(1)

    // 异步写入
    ClickHouseSink().sink[TestEntity](source)(x => {
      s"(${x.userId},${x.siteId})"
    }).setParallelism(1)

    // jdbc同步写入写入
    //    ClickHouseSink().syncSink[TestEntity](source)(x => {
    //      s"(${x.userId},${x.siteId})"
    //    }).setParallelism(1)
  }

}
