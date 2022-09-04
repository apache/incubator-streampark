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


package org.apache.streampark.flink.connector.elasticsearch7.internal

import org.apache.streampark.common.enums.ApiType
import org.apache.streampark.common.enums.ApiType.ApiType
import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.connector.function.TransformFunction
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.streaming.connectors.elasticsearch.{ElasticsearchSinkFunction, RequestIndexer}
import org.elasticsearch.action.ActionRequest
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.update.UpdateRequest

class ESSinkFunction[T](apiType: ApiType = ApiType.scala) extends ElasticsearchSinkFunction[T] with Logger {
  private[this] var scalaFunc: (T => ActionRequest) with Serializable = _
  private[this] var javaFunc: TransformFunction[T, ActionRequest] = _

  // for Scala
  def this(scalaFunc: T => ActionRequest) = {
    this(ApiType.scala)
    this.scalaFunc = new (T => ActionRequest) with Serializable {
      override def apply(v: T): ActionRequest = scalaFunc.apply(v)
    }
  }

  // for Java
  def this(javaFunc: TransformFunction[T, ActionRequest]) = {
    this(ApiType.java)
    this.javaFunc = javaFunc
  }

  def createIndexRequest(element: T): ActionRequest = apiType match {
    case ApiType.java => javaFunc.transform(element)
    case ApiType.scala => scalaFunc(element)
  }

  override def process(element: T, ctx: RuntimeContext, requestIndexer: RequestIndexer): Unit = {
    val request = createIndexRequest(element)
    request match {
      case indexRequest: IndexRequest => requestIndexer.add(indexRequest)
      case deleteRequest: DeleteRequest => requestIndexer.add(deleteRequest)
      case updateRequest: UpdateRequest => requestIndexer.add(updateRequest)
      case _ =>
        logError("ElasticsearchSinkFunction add ActionRequest is deprecated, please use IndexRequest|DeleteRequest|UpdateRequest ")
        requestIndexer.add(request)
    }
  }
}
