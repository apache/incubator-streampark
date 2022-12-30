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
package org.apache.streampark.common.util

import java.util

import scala.collection.JavaConversions._

class CURLBuilder(val url: String) {

  val headers: util.Map[String, String] = new util.HashMap[String, String]

  val formDatas: util.Map[String, String] = new util.HashMap[String, String]

  def addHeader(k: String, v: String): CURLBuilder = {
    this.headers.put(k, v)
    this
  }

  def addFormData(k: String, v: String): CURLBuilder = {
    this.formDatas.put(k, v)
    this
  }

  def build: String = {
    require(url != null, "[StreamPark] cURL build failed, url must not be null")
    val cURL = new StringBuilder("curl -X POST ")
    cURL.append(String.format("'%s' \\\n", url))
    for (headerKey <- headers.keySet) {
      cURL.append(String.format("-H \'%s: %s\' \\\n", headerKey, headers.get(headerKey)))
    }
    for (field <- formDatas.keySet) {
      cURL.append(String.format("--data-urlencode \'%s=%s\' \\\n", field, formDatas.get(field)))
    }
    cURL.append("-i")
    cURL.toString
  }

}
