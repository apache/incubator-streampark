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

package org.apache.streampark.flink.connector.hbase.source

import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.connector.hbase.bean.HBaseQuery
import org.apache.streampark.flink.connector.hbase.internal.HBaseSourceFunction
import org.apache.streampark.flink.core.scala.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.hadoop.hbase.client._

import java.util.Properties
import scala.annotation.meta.param

object HBaseSource {

  def apply(@(transient@param) property: Properties = new Properties())(implicit ctx: StreamingContext): HBaseSource = new HBaseSource(ctx, property)

}

/**
 * Support end-to-end exactly once and replay
 *
 * @param ctx
 * @param property
 */
class HBaseSource(@(transient@param) val ctx: StreamingContext, property: Properties = new Properties()) {

  def getDataStream[R: TypeInformation](query: R => HBaseQuery,
                                        func: Result => R,
                                        running: Unit => Boolean)(implicit prop: Properties = new Properties()): DataStream[R] = {
    Utils.copyProperties(property, prop)
    val hBaseFunc = new HBaseSourceFunction[R](prop, query, func, running)
    ctx.addSource(hBaseFunc)
  }

}



