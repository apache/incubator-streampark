/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.common.util

import org.apache.flink.configuration.{JobManagerOptions, RestOptions}

import java.util
import scala.util.Try
import java.lang.{Integer => JavaInt}

object StandaloneUtils {

  lazy val DEFAULT_REST_ADDRESS: String = "localhost"

  lazy val DEFAULT_REST_PORT: JavaInt = 8081

  /**
   * Append the http Rest Api
   * User dynamicOptions -> Flink version to config -> default
   *
   * @param dynamicOptions
   * @param flinkUrl
   * @return
   */

  def getRestWebAppURL(flinkConf: util.Map[String, String],
                       address: String,
                       port: Integer,
                       flinkUrl: String): String = {
    val lastAddress = Option(address).getOrElse(getRestAddress(flinkConf))
    val lastPort = Option(port).getOrElse(getRestPort(flinkConf))
    s"http://$lastAddress:$lastPort/$flinkUrl"
  }

  def getRestAddress(flinkConf: util.Map[String, String]): String = {
    Option(flinkConf.get(RestOptions.ADDRESS)) match {
      case None => flinkConf.getOrDefault(JobManagerOptions.ADDRESS, DEFAULT_REST_ADDRESS)
      case Some(x) => x
    }
  }

  def getRestPort(flinkConf: util.Map[String, String]): Int = {
    Try(flinkConf.get(RestOptions.PORT).toInt).getOrElse(DEFAULT_REST_PORT)
  }
}
