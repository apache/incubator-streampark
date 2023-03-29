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

package org.apache.streampark.flink.connector.elasticsearch6.util

import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.xcontent.XContentType

object ElasticsearchUtils {

  def indexRequest(index: String, indexType: String, id: String, source: String)(implicit xContentType: XContentType = XContentType.JSON): IndexRequest = {
    require(source != null, "indexRequest error:source can not be null...")
    require(xContentType != null, "indexRequest error:xContentType can not be null...")
    val indexReq = new IndexRequest(index, indexType, id)
    val mapping = List("source" -> new BytesArray(source), "contentType" -> xContentType)
    mapping.foreach { x =>
      val field = indexReq.getClass.getDeclaredField(x._1)
      field.setAccessible(true)
      field.set(indexReq, x._2)
    }
    indexReq
  }
}
