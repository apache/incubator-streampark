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
import com.mongodb.client.{FindIterable, MongoCollection, MongoCursor}
import com.streamxhub.common.util.{Logger, MongoConfig, Utils}
import com.streamxhub.flink.core.scala.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.RichSourceFunction
import org.apache.flink.streaming.api.scala.DataStream
import org.bson.Document

import java.util.{Date, Properties}
import com.streamxhub.flink.core.java.function.MongoFunction
import com.streamxhub.flink.core.scala.enums.ApiType
import com.streamxhub.flink.core.scala.enums.ApiType.ApiType
import com.streamxhub.flink.core.scala.util.FlinkUtils
import org.apache.flink.runtime.state.{CheckpointListener, FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.api.common.state.ListState
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext

import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.util.{Success, Try}


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

  def getDataStream[R: TypeInformation](collection: String, queryFun: (R, MongoCollection[Document]) => FindIterable[Document], resultFun: MongoCursor[Document] => List[R])(implicit prop: Properties = new Properties()): DataStream[R] = {
    Utils.copyProperties(property, prop)
    val mongoFun = new MongoSourceFunction[R](collection, prop, queryFun, resultFun)
    ctx.addSource(mongoFun)
  }

}


private[this] class MongoSourceFunction[R: TypeInformation](apiType: ApiType, prop: Properties = new Properties(), collection: String) extends RichSourceFunction[R] with CheckpointedFunction with CheckpointListener with Logger {

  private[this] var running = true
  var client: MongoClient = _
  var mongoCollection: MongoCollection[Document] = _

  private[this] var mongoFunc: MongoFunction[R] = _
  private[this] var queryFunc: (R, MongoCollection[Document]) => FindIterable[Document] = _
  private[this] var resultFunc: MongoCursor[Document] => List[R] = _
  @transient private var state: ListState[R] = _
  private val OFFSETS_STATE_NAME: String = "mongo-source-query-states"
  private[this] var lastOne: R = _

  //for Scala
  def this(collectionName: String, prop: Properties, queryFunc: (R, MongoCollection[Document]) => FindIterable[Document], resultFunc: MongoCursor[Document] => List[R]) = {
    this(ApiType.scala, prop, collectionName)
    this.queryFunc = queryFunc
    this.resultFunc = resultFunc
  }

  //for JAVA
  def this(collectionName: String, prop: Properties, mongoFunc: MongoFunction[R]) {
    this(ApiType.java, prop, collectionName)
    this.mongoFunc = mongoFunc
  }

  override def cancel(): Unit = this.running = false

  override def open(parameters: Configuration): Unit = {
    client = MongoConfig.getClient(prop)
    val db = MongoConfig.getProperty(prop, MongoConfig.database)
    val database = client.getDatabase(db)
    mongoCollection = database.getCollection(collection)
  }

  @throws[Exception]
  override def run(ctx: SourceContext[R]): Unit = {
    while (running) {
      apiType match {
        case ApiType.scala =>
          val find = queryFunc(lastOne, mongoCollection)
          if (find != null) {
            resultFunc(find.iterator).foreach(x => {
              lastOne = x
              ctx.collectWithTimestamp(lastOne, new Date().getTime)
            })
          }
        case ApiType.java =>
          val find = mongoFunc.getQuery(lastOne, mongoCollection)
          if (find != null) {
            mongoFunc.doResult(find.iterator).foreach(x => {
              lastOne = x
              ctx.collectWithTimestamp(lastOne, new Date().getTime)
            })
          }
      }
    }
  }

  override def close(): Unit = {
    client.close()
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    if (running) {
      state.clear()
      if (lastOne != null) {
        state.add(lastOne)
      }
    } else {
      logger.error("[StreamX] MongoSource snapshotState called on closed source")
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    //从checkpoint中恢复...
    logger.info("[StreamX] MongoSource snapshotState initialize")
    state = FlinkUtils.getUnionListState[R](context, OFFSETS_STATE_NAME)
    Try(state.get.head) match {
      case Success(q) => lastOne = q
      case _ =>
    }
  }

  override def notifyCheckpointComplete(checkpointId: Long): Unit = {
    logger.info(s"[StreamX] MongoSource checkpointComplete: $checkpointId")
  }

}
