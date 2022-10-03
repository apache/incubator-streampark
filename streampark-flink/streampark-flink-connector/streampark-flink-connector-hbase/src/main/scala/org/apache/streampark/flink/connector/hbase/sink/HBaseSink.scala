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

package org.apache.streampark.flink.connector.hbase.sink

import org.apache.streampark.common.conf.ConfigConst._
import org.apache.streampark.common.util.{ConfigUtils, Logger, Utils}
import org.apache.streampark.flink.connector.function.TransformFunction
import org.apache.streampark.flink.connector.hbase.internal.HBaseSinkFunction
import org.apache.streampark.flink.connector.sink.Sink
import org.apache.streampark.flink.core.scala.StreamingContext
import org.apache.flink.streaming.api.datastream.{DataStreamSink, DataStream => JavaDataStream}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.hadoop.hbase.client._

import java.lang.{Iterable => JIter}
import java.util.Properties
import scala.annotation.meta.param

object HBaseSink {

  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): HBaseSink = new HBaseSink(ctx, property, parallelism, name, uid)

}

class HBaseSink(@(transient@param) ctx: StreamingContext,
                property: Properties = new Properties(),
                parallelism: Int = 0,
                name: String = null,
                uid: String = null)(implicit alias: String = "") extends Sink with Logger {


  def this(ctx: StreamingContext) {
    this(ctx, new Properties, 0, null, null)
  }

  def sink[T](stream: DataStream[T], tableName: String)(implicit fun: T => JIter[Mutation]): DataStreamSink[T] = {
    val prop: Properties = checkProp(stream, tableName, fun)
    val sinkFun = new HBaseSinkFunction[T](tableName, prop, fun)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  def sink[T](stream: JavaDataStream[T], tableName: String, fun: TransformFunction[T, JIter[Mutation]]): DataStreamSink[T] = {
    val prop: Properties = checkProp(stream, tableName, fun)
    val sinkFun = new HBaseSinkFunction[T](tableName, prop, fun)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

  private def checkProp[T](stream: Object, tableName: String, fun: Object): Properties = {
    implicit val prop: Properties = ConfigUtils.getConf(ctx.parameter.toMap, HBASE_PREFIX, HBASE_PREFIX)(alias)
    Utils.copyProperties(property, prop)
    require(stream != null, () => s"sink Stream must not null")
    require(tableName != null, () => s"sink tableName must not null")
    require(fun != null, () => s" fun must not null")
    prop
  }
}




