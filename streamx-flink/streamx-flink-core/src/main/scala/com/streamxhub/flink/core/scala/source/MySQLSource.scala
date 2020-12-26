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

import com.streamxhub.common.util.{JdbcUtils, Logger, Utils}
import com.streamxhub.flink.core.java.function.SQLGetFunction
import com.streamxhub.flink.core.scala.StreamingContext
import com.streamxhub.flink.core.scala.enums.ApiType
import com.streamxhub.flink.core.scala.enums.ApiType.ApiType
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala.DataStream

import java.util.{Date, Properties}
import scala.annotation.meta.param
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.collection.Map


object MySQLSource {

  def apply(@(transient@param) ctx: StreamingContext, property: Properties = new Properties()): MySQLSource = new MySQLSource(ctx, property)

}

class MySQLSource(@(transient@param) val ctx: StreamingContext, property: Properties = new Properties()) {

  /**
   *
   * @param sqlFun
   * @param fun
   * @param jdbc
   * @tparam R
   * @return
   */
  def getDataStream[R: TypeInformation](sqlFun: R => String, fun: Iterable[Map[String, _]] => Iterable[R])(implicit jdbc: Properties = new Properties()): DataStream[R] = {
    Utils.copyProperties(property, jdbc)
    val mysqlFun = new MySQLSourceFunction[R](jdbc, sqlFun, fun)
    ctx.addSource(mysqlFun)
  }

}

/**
 *
 * @tparam R
 */
private[this] class MySQLSourceFunction[R: TypeInformation](apiType: ApiType = ApiType.scala, jdbc: Properties) extends RichSourceFunction[R] with Logger {

  @volatile private[this] var running = true
  private[this] var scalaSqlFunc: R => String = _
  private[this] var scalaResultFunc: Function[Iterable[Map[String, _]], Iterable[R]] = _
  private[this] var sqlFunc: SQLGetFunction[R] = _
  private[this] var lastOne: R = _

  //for Scala
  def this(jdbc: Properties, sqlFunc: R => String, resultFunc: Iterable[Map[String, _]] => Iterable[R]) = {
    this(ApiType.scala, jdbc)
    this.scalaSqlFunc = sqlFunc
    this.scalaResultFunc = resultFunc
  }

  //for JAVA
  def this(jdbc: Properties, javaSqlFunc: SQLGetFunction[R]) {
    this(ApiType.java, jdbc)
    this.sqlFunc = javaSqlFunc
  }

  @throws[Exception]
  override def run(@(transient@param) ctx: SourceFunction.SourceContext[R]): Unit = {
    while (this.running) {
      ctx.getCheckpointLock.synchronized {
        val sql = apiType match {
          case ApiType.scala => scalaSqlFunc(lastOne)
          case ApiType.java => sqlFunc.getQuery(lastOne)
        }
        val result: List[Map[String, _]] = apiType match {
          case ApiType.scala => JdbcUtils.select(sql)(jdbc)
          case ApiType.java => JdbcUtils.select(sql)(jdbc)
        }
        apiType match {
          case ApiType.scala => scalaResultFunc(result).foreach(x => {
            lastOne = x
            ctx.collectWithTimestamp(lastOne, new Date().getTime)
          })
          case ApiType.java => sqlFunc.doResult(result.map(_.asJava)).foreach(x => {
            lastOne = x
            ctx.collectWithTimestamp(lastOne, new Date().getTime)
          })
        }
      }
    }
  }

  override def cancel(): Unit = this.running = false

}

