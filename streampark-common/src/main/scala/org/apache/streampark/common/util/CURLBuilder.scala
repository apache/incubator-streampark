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

import org.apache.streampark.common.util.Implicits._

import java.util

class CURLBuilder(val url: String) {

  private[this] val headers: util.Map[String, String] = new JavaHashMap[String, String]

  private[this] val formData: util.Map[String, String] = new JavaHashMap[String, String]

  def addHeader(k: String, v: String): CURLBuilder = {
    this.headers.put(k, v)
    this
  }

  def addFormData(k: String, v: java.io.Serializable): CURLBuilder = {
    this.formData.put(k, v.toString)
    this
  }

  def build: String = {
    require(url != null, "[StreamPark] CURL build failed, url must not be null")
    val cURL = new StringBuilder("curl -X POST ")
    cURL.append(String.format("'%s' \\\n", url))
    headers.keySet.foreach(h => cURL.append(String.format("-H \'%s: %s\' \\\n", h, headers.get(h))))
    formData.foreach(k =>
      cURL.append(String.format("--data-urlencode \'%s=%s\' \\\n", k._1, k._2)))
    cURL.toString.trim.dropRight(1)
  }

}
