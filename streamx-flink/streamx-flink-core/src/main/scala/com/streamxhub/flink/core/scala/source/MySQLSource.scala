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
package com.streamxhub.flink.core.scala.source

import com.streamxhub.common.util.{JdbcUtils, Logger}
import com.streamxhub.flink.core.java.function.{GetSQLFunction, ResultSetFunction}
import com.streamxhub.flink.core.scala.StreamingContext
import com.streamxhub.flink.core.scala.enums.ApiType
import com.streamxhub.flink.core.scala.enums.ApiType.ApiType
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala.DataStream

import java.util.Properties
import scala.annotation.meta.param
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
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
  def getDataStream[R: TypeInformation](sqlFun: () => String, fun: Iterable[Map[String, _]] => Iterable[R])(implicit jdbc: Properties): DataStream[R] = {
    overrideParams.foreach(x => jdbc.put(x._1, x._2))
    val mysqlFun = new MySQLSourceFunction[R](jdbc, sqlFun, fun)
    ctx.addSource(mysqlFun)
  }

}

/**
 *
 * @tparam R
 */
private[this] class MySQLSourceFunction[R: TypeInformation](apiType: ApiType = ApiType.SCALA, jdbc: Properties) extends RichSourceFunction[R] with Logger {

  @volatile private[this] var running = true
  private[this] var scalaSqlFunc: () => String = _
  private[this] var scalaResultFunc: Function[Iterable[Map[String, _]], Iterable[R]] = _
  private[this] var javaSqlFunc: GetSQLFunction = _
  private[this] var javaResultFunc: ResultSetFunction[R] = _

  //for Scala
  def this(jdbc: Properties, sqlFunc: () => String, resultFunc: Iterable[Map[String, _]] => Iterable[R]) = {
    this(ApiType.SCALA, jdbc)
    this.scalaSqlFunc = sqlFunc
    this.scalaResultFunc = resultFunc
  }

  //for JAVA
  def this(jdbc: Properties, javaSqlFunc: GetSQLFunction, javaResultFunc: ResultSetFunction[R]) {
    this(ApiType.JAVA, jdbc)
    this.javaSqlFunc = javaSqlFunc
    this.javaResultFunc = javaResultFunc
  }

  @throws[Exception]
  override def run(@(transient@param) ctx: SourceFunction.SourceContext[R]): Unit = {
    while (this.running) {
      ctx.getCheckpointLock.synchronized {
        val sql = apiType match {
          case ApiType.SCALA => scalaSqlFunc()
          case ApiType.JAVA => javaSqlFunc.getSQL
        }
        val result: List[Map[String, _]] = apiType match {
          case ApiType.SCALA => JdbcUtils.select(sql)(jdbc)
          case ApiType.JAVA => JdbcUtils.select(sql)(jdbc)
        }
        apiType match {
          case ApiType.SCALA => scalaResultFunc(result).foreach(ctx.collect)
          case ApiType.JAVA => javaResultFunc.result(result.map(_.asJava)).foreach(ctx.collect)
        }
      }
    }
  }

  override def cancel(): Unit = this.running = false

}

