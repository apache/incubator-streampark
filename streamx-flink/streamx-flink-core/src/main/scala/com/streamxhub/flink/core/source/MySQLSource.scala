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

import com.streamxhub.common.util.{Logger, JdbcUtils}
import com.streamxhub.flink.core.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala.DataStream

import scala.collection.Map


class MySQLSource(@transient val ctx: StreamingContext, specialKafkaParams: Map[String, String] = Map.empty[String, String]) {

  /**
   *
   * @param sqlFun
   * @param fun
   * @param interval sql查询的间隔时间.
   * @param config
   * @tparam R
   * @return
   */
  def getDataStream[R: TypeInformation](sqlFun: => String, fun: List[Map[String, _]] => List[R], interval: Long = 0)(implicit config: Properties): DataStream[R] = {
    val mysqlFun = new MySQLSourceFunction[R](sqlFun, fun, interval)
    ctx.addSource(mysqlFun)
  }

}

/**
 *
 * @param sqlFun
 * @param resultFun
 * @param interval 两次sql执行查询的间隔.防止死循环密集的查询sql
 * @param typeInformation$R$0
 * @param config
 * @tparam R
 */
private[this] class MySQLSourceFunction[R: TypeInformation](sqlFun: => String, resultFun: List[Map[String, _]] => List[R], interval: Long)(implicit config: Properties) extends SourceFunction[R] with Logger {

  private[this] var isRunning = true

  var queryTime = 0L

  override def cancel(): Unit = this.isRunning = false

  @throws[Exception]
  override def run(ctx: SourceFunction.SourceContext[R]): Unit = {
    interval match {
      case x if x <= 0 =>
        while (isRunning) {
          val list = JdbcUtils.select(sqlFun)
          resultFun(list).foreach(ctx.collect)
        }
      case _ =>
        while (isRunning && System.currentTimeMillis() - queryTime >= interval) {
          queryTime = System.currentTimeMillis()
          val list = JdbcUtils.select(sqlFun)
          resultFun(list).foreach(ctx.collect)
        }
    }
  }

}