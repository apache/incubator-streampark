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
package com.streamxhub.flink.core.source

import java.util.Properties

import com.streamxhub.common.util.{Logger, MySQLUtils}
import com.streamxhub.flink.core.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala.DataStream

import scala.collection.Map


class MySQLSource(@transient val ctx: StreamingContext, specialKafkaParams: Map[String, String] = Map.empty[String, String]) {

  def getDataStream[R: TypeInformation](querySQL: String, fun: Map[String, _] => R)(implicit config: Properties): DataStream[R] = {
    val mysqlFun = new MySQLSourceFunction[R](querySQL, fun)
    ctx.addSource(mysqlFun)
  }

}

private[this] class MySQLSourceFunction[R: TypeInformation](querySQL: String, fun: Map[String, _] => R)(implicit config: Properties) extends SourceFunction[R] with Logger {

  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  @throws[Exception]
  override def run(ctx: SourceFunction.SourceContext[R]): Unit = {
    while (isRunning) {
      val list = MySQLUtils.select(querySQL)
      list.foreach(x => ctx.collect(fun(x)))
    }
  }
}