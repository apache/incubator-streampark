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

package org.apache.streampark.flink.connector.jdbc.request

import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.connector.jdbc.internal.JdbcASyncFunction
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.{AsyncDataStream, DataStream}

import java.util.Properties
import java.util.concurrent.TimeUnit
import scala.annotation.meta.param

object JdbcRequest {

  def apply[T: TypeInformation](@(transient@param) stream: DataStream[T], property: Properties = new Properties()): JdbcRequest[T] = new JdbcRequest[T](stream, property)

}

class JdbcRequest[T: TypeInformation](@(transient@param) private val stream: DataStream[T], property: Properties = new Properties()) {

  /**
   *
   * @param sqlFun
   * @param jdbc
   * @tparam R
   * @return
   */
  def requestOrdered[R: TypeInformation](@(transient@param) sqlFun: T => String, @(transient@param) resultFun: (T, Map[String, _]) => R, timeout: Long = 1000, capacity: Int = 10)(implicit jdbc: Properties): DataStream[R] = {
    Utils.copyProperties(property, jdbc)
    val async = new JdbcASyncFunction[T, R](sqlFun, resultFun, jdbc)
    AsyncDataStream.orderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

  def requestUnordered[R: TypeInformation](@(transient@param) sqlFun: T => String, @(transient@param) resultFun: (T, Map[String, _]) => R, timeout: Long = 1000, capacity: Int = 10)(implicit jdbc: Properties): DataStream[R] = {
    Utils.copyProperties(property, jdbc)
    val async = new JdbcASyncFunction[T, R](sqlFun, resultFun, jdbc)
    AsyncDataStream.unorderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

}






