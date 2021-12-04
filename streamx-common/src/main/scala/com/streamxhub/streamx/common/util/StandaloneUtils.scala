/*
 * Copyright (c) 2021 The StreamX Project
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
package com.streamxhub.streamx.common.util

import org.apache.flink.configuration.{JobManagerOptions, RestOptions}

import java.util
import scala.collection.immutable.Map

object StandaloneUtils {

  val DEFAULT_REST_ADDRESS: String = "localhost"
  val DEFAULT_REST_PORT: Integer = 8081

  /**
   * Append the http Rest Api
   * User dynamicOptions -> Flink version to config -> default
   *
   * @param dynamicOptions
   * @param flinkUrl
   * @return
   */

  def getRestWebAppURL(flinkConf: util.Map[String, String],
                       address:String,
                       port:Integer,
                       flinkUrl: String): String = {
    var lastAddress = address
    var lastPort = port
    if (address.isEmpty){
      lastAddress = getRestAddress(flinkConf)
    } else if (port == null){
      lastPort = getRestPort(flinkConf)
    }
    "http://" + lastAddress + ":" + lastPort + "/" + flinkUrl
  }

  def getRestAddress(flinkConf: util.Map[String, String]): String = {
    val address: Option[String] = Some(flinkConf.get(flinkConf.get(RestOptions.ADDRESS)))
    if (address.isEmpty){
      flinkConf.getOrDefault(JobManagerOptions.ADDRESS,DEFAULT_REST_ADDRESS)
    }else{
      address.get
    }
  }

  def getRestPort(flinkConf: util.Map[String, String]): Integer = {
    if (flinkConf.get(RestOptions.PORT) != null) {
      flinkConf.get(RestOptions.PORT).toInt
    }
    DEFAULT_REST_PORT
  }
}
