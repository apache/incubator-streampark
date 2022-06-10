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

package com.streamxhub.streamx.flink.connector.jdbc.source

import com.streamxhub.streamx.common.util.Utils
import com.streamxhub.streamx.flink.connector.jdbc.internal.JdbcSourceFunction
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.DataStream

import java.util.Properties
import scala.annotation.meta.param
import scala.collection.Map


object JdbcSource {

  def apply(@(transient@param) property: Properties = new Properties())(implicit ctx: StreamingContext): JdbcSource = new JdbcSource(ctx, property)

}

class JdbcSource(@(transient@param) val ctx: StreamingContext, property: Properties = new Properties()) {

  /**
   *
   * @param sqlFun
   * @param fun
   * @param jdbc
   * @tparam R
   * @return
   */
  def getDataStream[R: TypeInformation](sqlFun: R => String,
                                        fun: Iterable[Map[String, _]] => Iterable[R],
                                        running: Unit => Boolean)(implicit jdbc: Properties = new Properties()): DataStream[R] = {
    Utils.copyProperties(property, jdbc)
    val mysqlFun = new JdbcSourceFunction[R](jdbc, sqlFun, fun, running)
    ctx.addSource(mysqlFun)
  }

}



