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

import com.streamxhub.common.util.{JdbcUtils, Logger}
import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.enums.ApiType
import com.streamxhub.flink.core.enums.ApiType.ApiType
import com.streamxhub.flink.core.function.{ResultSetFunction, SQLFunction}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala.DataStream

import scala.annotation.meta.param
import scala.collection.JavaConverters._
import scala.collection.Map


object MySQLSource {

  def apply(@(transient@param) ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]): MySQLSource = new MySQLSource(ctx, overrideParams)

}

class MySQLSource(@(transient@param) val ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]) {

  /**
   *
   * @param sqlFun
   * @param fun
   * @param jdbc
   * @tparam R
   * @return
   */
  def getDataStream[R: TypeInformation](sqlFun: => String, fun: Map[String, _] => R)(implicit jdbc: Properties): DataStream[R] = {
    val mysqlFun = new MySQLSourceFunction[R](jdbc, sqlFun, fun)
    ctx.addSource(mysqlFun)
  }

}

/**
 *
 * @tparam R
 */
private[this] class MySQLSourceFunction[R: TypeInformation](apiType: ApiType = ApiType.Scala, jdbc: Properties) extends SourceFunction[R] with Logger {

  private[this] var isRunning = true
  private[this] var scalaSqlFunc: String = _
  private[this] var scalaResultFunc: Function[Map[String, _], R] = _
  private[this] var javaSqlFunc: SQLFunction = null
  private[this] var javaResultFunc: ResultSetFunction[R] = null

  //for Scala
  def this(jdbc: Properties, sqlFunc: => String, resultFunc: Map[String, _] => R) = {
    this(ApiType.Scala, jdbc)
    this.scalaSqlFunc = sqlFunc
    this.scalaResultFunc = resultFunc
  }

  //for JAVA
  def this(jdbc: Properties, javaSqlFunc: SQLFunction, javaResultFunc: ResultSetFunction[R]) {
    this(ApiType.JAVA, jdbc)
    this.javaSqlFunc = javaSqlFunc
    this.javaResultFunc = javaResultFunc
  }

  @throws[Exception]
  override def run(@(transient@param) ctx: SourceFunction.SourceContext[R]): Unit = {
    while (this.isRunning) {
      apiType match {
        case ApiType.Scala => JdbcUtils.select(scalaSqlFunc)(jdbc).map(scalaResultFunc).foreach(ctx.collect)
        case ApiType.JAVA => JdbcUtils.select(javaSqlFunc.getSQL)(jdbc).map(x => javaResultFunc.result(x.asJava))
      }
    }
  }

  override def cancel(): Unit = this.isRunning = false
}