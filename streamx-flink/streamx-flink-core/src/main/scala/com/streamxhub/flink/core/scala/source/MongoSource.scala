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

import com.mongodb.MongoClient
import com.mongodb.client.{FindIterable, MongoCursor, MongoDatabase}
import com.streamxhub.common.util.{Logger, MongoConfig, Utils}
import com.streamxhub.flink.core.scala.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala.DataStream
import org.bson.Document
import java.util.Properties

import com.streamxhub.flink.core.java.function.MongoFunction
import com.streamxhub.flink.core.scala.enums.ApiType
import com.streamxhub.flink.core.scala.enums.ApiType.ApiType

import scala.annotation.meta.param
import scala.collection.JavaConversions._


object MongoSource {

  def apply(@(transient@param) ctx: StreamingContext, property: Properties = new Properties()): MongoSource = new MongoSource(ctx, property)

}

class MongoSource(@(transient@param) val ctx: StreamingContext, property: Properties = new Properties()) {


  /**
   *
   * @param queryFun
   * @param resultFun
   * @param prop
   * @tparam R
   * @return
   */

  def getDataStream[R: TypeInformation](queryFun: MongoDatabase => FindIterable[Document], resultFun: MongoCursor[Document] => List[R])(implicit prop: Properties = new Properties()): DataStream[R] = {
    Utils.copyProperties(property, prop)
    val mongoFun = new MongoSourceFunction[R](prop,queryFun, resultFun)
    ctx.addSource(mongoFun)
  }

}


private[this] class MongoSourceFunction[R: TypeInformation](apiType:ApiType, prop: Properties = new Properties()) extends RichSourceFunction[R] with Logger {

  private[this] var isRunning = true
  var client: MongoClient = _
  var database: MongoDatabase = _

  private[this] var mongoFunc: MongoFunction[R] = _
  private[this] var queryFunc: MongoDatabase => FindIterable[Document] = _
  private[this] var resultFunc: MongoCursor[Document] => List[R] = _

  //for Scala
  def this(prop: Properties, queryFunc: MongoDatabase => FindIterable[Document], resultFunc: MongoCursor[Document] => List[R]) = {
    this(ApiType.SCALA, prop)
    this.queryFunc = queryFunc
    this.resultFunc = resultFunc
  }

  //for JAVA
  def this(prop: Properties, mongoFunc: MongoFunction[R]) {
    this(ApiType.JAVA, prop)
    this.mongoFunc = mongoFunc
  }

  override def cancel(): Unit = this.isRunning = false

  override def open(parameters: Configuration): Unit = {
    client = MongoConfig.getClient(prop)
    val db = MongoConfig.getProperty(prop, MongoConfig.database)
    database = client.getDatabase(db)
  }

  @throws[Exception]
  override def run(@(transient@param) ctx: SourceFunction.SourceContext[R]): Unit = {
    while (isRunning) {

      apiType match {
        case ApiType.SCALA =>
          val find = queryFunc(database)
          if (find != null) {
            resultFunc(find.iterator).foreach(ctx.collect)
          }
        case ApiType.JAVA =>
          val find = mongoFunc.getQuery(database)
          if (find != null) {
            mongoFunc.doResult(find.iterator).foreach(ctx.collect)
          }
      }
    }
  }

  override def close(): Unit = {
    client.close()
  }

}
