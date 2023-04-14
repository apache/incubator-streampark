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

package org.apache.streampark.flink.core

import org.apache.streampark.common.conf.ConfigConst.{KEY_APP_NAME, KEY_FLINK_APP_NAME}
import org.apache.streampark.common.util.DeflaterUtils

import org.apache.flink.api.java.utils.ParameterTool

import scala.util.Try

object EnhancerImplicit {

  implicit class EnhanceParameterTool(parameterTool: ParameterTool) {

    def getAppName(name: String = null, required: Boolean = false): String = {
      val appName = name match {
        case null =>
          Try(DeflaterUtils.unzipString(parameterTool.get(KEY_APP_NAME(), null)))
            .getOrElse(parameterTool.get(KEY_FLINK_APP_NAME, null))
        case x => x
      }
      if (required) {
        require(appName != null, "[StreamPark] Application name cannot be null")
      }
      appName
    }

  }

}
