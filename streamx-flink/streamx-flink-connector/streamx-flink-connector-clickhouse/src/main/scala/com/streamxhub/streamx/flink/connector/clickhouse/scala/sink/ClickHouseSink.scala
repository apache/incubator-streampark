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

package com.streamxhub.streamx.flink.connector.clickhouse.scala.sink

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util._
import com.streamxhub.streamx.flink.connector.clickhouse.scala.conf.ClickHouseConfigConst.CLICKHOUSE_SINK_PREFIX
import com.streamxhub.streamx.flink.connector.clickhouse.scala.internal.{AsyncClickHouseSinkFunction, ClickHouseSinkFunction}
import com.streamxhub.streamx.flink.connector.function.PojoToStringFunction
import com.streamxhub.streamx.flink.connector.sink.Sink
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.api.datastream.{DataStream => JavaDataStream}

import java.util.Properties
import scala.annotation.meta.param

/**
 * @author benjobs
 */
object ClickHouseSink {

  /**
   *
   * @param property
   * @param parallelism
   * @param name
   * @param uid
   * @param ctx
   * @return
   */

  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): ClickHouseSink = new ClickHouseSink(ctx, property, parallelism, name, uid)

}

class ClickHouseSink(@(transient@param) ctx: StreamingContext,
                     property: Properties = new Properties(),
                     parallelism: Int = 0,
                     name: String = null,
                     uid: String = null)(implicit alias: String = "") extends Sink with Logger {

  val prop = ConfigUtils.getConf(ctx.parameter.toMap, CLICKHOUSE_SINK_PREFIX)(alias)

  Utils.copyProperties(property, prop)

  def this(ctx: StreamingContext, alias: String) {
    this(ctx, new Properties, 0, null, null)(alias)
  }

  def this(ctx: StreamingContext) {
    this(ctx, new Properties, 0, null, null)
  }

  /**
   * asynchronous Write
   *
   * @param scala stream
   * @param toSQLFn
   * @tparam T
   * @return
   */
  def sink[T](stream: DataStream[T])(implicit toSQLFn: T => String = null): DataStreamSink[T] = {
    checkParameters(stream)
    val sinkFun = new AsyncClickHouseSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }


  private def checkParameters[T](stream: Object): Unit = {
    val failoverTable: String = prop.getProperty(KEY_SINK_FAILOVER_TABLE)
    require(failoverTable != null && !failoverTable.isEmpty, () => s"ClickHouse async  insert failoverTable must not null")
    require(stream != null, () => s"sink Stream must not null")
  }

  /**
   * asynchronous Write
   *
   * @param java stream
   * @param toSQLFn
   * @tparam T
   * @return
   */
  def sink[T](stream: JavaDataStream[T], toSQLFn: PojoToStringFunction[T]): DataStreamSink[T] = {
    checkParameters(stream)
    val sinkFun = new AsyncClickHouseSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  /**
   * asynchronous Write with all Feild
   *
   * @param stream
   * @tparam T
   * @return
   */
  def sink[T](stream: JavaDataStream[T]): DataStreamSink[T] = {
    sink(stream, null)
  }

  /**
   * synchronous Write
   *
   * @param scala stream
   * @param toSQLFn
   * @tparam T
   * @return
   */
  def syncSink[T](stream: DataStream[T])(implicit toSQLFn: T => String = null): DataStreamSink[T] = {
    val sinkFun = new ClickHouseSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  /**
   * synchronous Write
   *
   * @param java stream
   * @param toSQLFn
   * @tparam T
   * @return
   */
  def syncSink[T](stream: JavaDataStream[T], toSQLFn: PojoToStringFunction[T]): DataStreamSink[T] = {
    val sinkFun = new ClickHouseSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  /**
   * synchronous Write with all Feild
   *
   * @param stream
   * @tparam T
   * @return
   */
  def syncSink[T](stream: JavaDataStream[T]): DataStreamSink[T] = {
    syncSink(stream, null)
  }
}
