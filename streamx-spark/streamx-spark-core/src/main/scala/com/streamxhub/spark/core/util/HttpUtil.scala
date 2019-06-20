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

package com.streamxhub.spark.core.util


import scalaj.http.Http

import scala.collection.Map

/**
  *
  * SQLContext 单例
  */
object HttpUtil {

  def httpPost(url: String, data: String, headers: Map[String, String] = Map.empty[String, String]): (Int, String) = {
    var req = Http(url).postData(data)
    headers.foreach { case (k, v) => req = req.header(k, v) }
    val res = req.asString
    (res.code, res.body)
  }

  def httpGet(url: String, param: Seq[(String, String)] = Seq.empty[(String, String)]): (Int, String) = {
    val req = Http(url).params(param)
    val res = req.asString
    (res.code, res.body)
  }
}
