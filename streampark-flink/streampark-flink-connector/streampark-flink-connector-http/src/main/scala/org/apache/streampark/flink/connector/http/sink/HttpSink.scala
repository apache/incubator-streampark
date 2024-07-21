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

package org.apache.streampark.flink.connector.http.sink

import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.connector.http.function.HttpSinkFunction
import org.apache.streampark.flink.connector.sink.Sink
import org.apache.streampark.flink.core.scala.StreamingContext

import org.apache.flink.streaming.api.datastream.{DataStream => JavaDataStream, DataStreamSink}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.http.client.methods._

import java.util.Properties

import scala.annotation.meta.param
import org.apache.streampark.common.util.Implicits._

object HttpSink {

  def apply(
      @(transient @param)
      header: Map[String, String] = Map.empty[String, String],
      property: Properties = new Properties(),
      parallelism: Int = 0,
      name: String = null,
      uid: String = null)(implicit ctx: StreamingContext): HttpSink =
    new HttpSink(ctx, property, header, parallelism, name, uid)

}

class HttpSink(
    @(transient @param) ctx: StreamingContext,
    property: Properties = new Properties(),
    header: Map[String, String] = Map.empty[String, String],
    parallelism: Int = 0,
    name: String = null,
    uid: String = null)
  extends Sink {

  // for java
  def this(ctx: StreamingContext) {
    this(ctx, new Properties(), Map.empty[String, String], 0, null, null)
  }

  val prop: Properties = ctx.parameter.getProperties
  Utils.copyProperties(property, prop)

  def get(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpGet.METHOD_NAME)

  def get(stream: JavaDataStream[String]): DataStreamSink[String] =
    sink(new DataStream[String](stream), HttpGet.METHOD_NAME)

  def post(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPost.METHOD_NAME)

  def post(stream: JavaDataStream[String]): DataStreamSink[String] =
    sink(new DataStream[String](stream), HttpPost.METHOD_NAME)

  def patch(stream: DataStream[String]): DataStreamSink[String] =
    sink(stream, HttpPatch.METHOD_NAME)

  def patch(stream: JavaDataStream[String]): DataStreamSink[String] =
    sink(new DataStream[String](stream), HttpPatch.METHOD_NAME)

  def put(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPut.METHOD_NAME)

  def put(stream: JavaDataStream[String]): DataStreamSink[String] =
    sink(new DataStream[String](stream), HttpPut.METHOD_NAME)

  def delete(stream: DataStream[String]): DataStreamSink[String] =
    sink(stream, HttpDelete.METHOD_NAME)

  def delete(stream: JavaDataStream[String]): DataStreamSink[String] =
    sink(new DataStream[String](stream), HttpDelete.METHOD_NAME)

  def options(stream: DataStream[String]): DataStreamSink[String] =
    sink(stream, HttpOptions.METHOD_NAME)

  def options(stream: JavaDataStream[String]): DataStreamSink[String] =
    sink(new DataStream[String](stream), HttpOptions.METHOD_NAME)

  def trace(stream: DataStream[String]): DataStreamSink[String] =
    sink(stream, HttpTrace.METHOD_NAME)

  def trace(stream: JavaDataStream[String]): DataStreamSink[String] =
    sink(new DataStream[String](stream), HttpTrace.METHOD_NAME)

  private[this] def sink(stream: DataStream[String], method: String): DataStreamSink[String] = {
    val sinkFun = new HttpSinkFunction(prop, header, method)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }
}
