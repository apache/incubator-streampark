/*
 * Copyright (c) 2020 The StreamX Project
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
package com.streamxhub.streamx.flink.submit

import com.streamxhub.streamx.common.util.{Logger, Utils}
import com.streamxhub.streamx.flink.proxy.FlinkShimsProxy
import com.streamxhub.streamx.flink.submit.domain.{StopRequest, StopResponse, SubmitRequest, SubmitResponse}
import org.apache.commons.lang.StringUtils

import java.util.regex.Pattern
import java.util.{Map => JavaMap}
import javax.annotation.Nonnull
import scala.collection.JavaConverters._
import scala.util.Try

object FlinkSubmitHelper extends Logger {

  // effective k-v regex pattern of submit.dynamicOption
  private val DYNAMIC_OPTION_ITEM_PATTERN = Pattern.compile("(-D)?(\\S+)=(\\S+)")

  private[this] val FLINKSUBMIT_CLASS_NAME = "com.streamxhub.streamx.flink.submit.FlinkSubmit"

  private[this] val SUBMITREQUEST_CLASS_NAME = "com.streamxhub.streamx.flink.submit.domain.SubmitRequest"

  private[this] val STOPREQUEST_CLASS_NAME = "com.streamxhub.streamx.flink.submit.domain.StopRequest"

  @throws[Throwable] def submit(submitRequest: SubmitRequest): SubmitResponse = {
    FlinkShimsProxy.proxy(submitRequest.flinkVersion, (classLoader: ClassLoader) => {
      val submitClass = classLoader.loadClass(FLINKSUBMIT_CLASS_NAME)
      val requestClass = classLoader.loadClass(SUBMITREQUEST_CLASS_NAME)
      val method = submitClass.getDeclaredMethod("submit", requestClass)
      method.setAccessible(true)
      val obj = method.invoke(null, FlinkShimsProxy.getObject(classLoader, submitRequest))
      require(obj != null)
      FlinkShimsProxy.getObject(this.getClass.getClassLoader, obj).asInstanceOf[SubmitResponse]
    })
  }

  @throws[Throwable] def stop(stopRequest: StopRequest): StopResponse = {
    FlinkShimsProxy.proxy(stopRequest.flinkVersion, (classLoader: ClassLoader) => {
      val submitClass = classLoader.loadClass(FLINKSUBMIT_CLASS_NAME)
      val requestClass = classLoader.loadClass(STOPREQUEST_CLASS_NAME)
      val method = submitClass.getDeclaredMethod("stop", requestClass)
      method.setAccessible(true)
      val obj = method.invoke(null, FlinkShimsProxy.getObject(classLoader, stopRequest))
      if (obj == null) null; else {
        FlinkShimsProxy.getObject(this.getClass.getClassLoader, obj).asInstanceOf[StopResponse]
      }
    })
  }

  /**
   * extract flink configuration from submitRequest.dynamicOption
   */
  @Nonnull def extractDynamicOption(dynamicOption: Array[String]): Map[String, String] = {
    dynamicOption match {
      case x if Utils.isEmpty(x) =>
        Map.empty
      case _ =>
        Try(dynamicOption
          .filter(_ != null)
          .map(_.trim)
          .map(DYNAMIC_OPTION_ITEM_PATTERN.matcher(_))
          .filter(_.matches())
          .map(m => m.group(2) -> m.group(3))
          .toMap
        ).getOrElse(Map.empty)
    }
  }

  /**
   * extract flink configuration from application.dynamicOption
   */
  @Nonnull def extractDynamicOption(dynamicOptions: String): Map[String, String] = {
    if (StringUtils.isEmpty(dynamicOptions)) {
      Map.empty[String, String]
    } else {
      extractDynamicOption(dynamicOptions.split("\\s+"))
    }
  }

  @Nonnull def extractDynamicOptionAsJava(dynamicOptions: String): JavaMap[String, String] = extractDynamicOption(dynamicOptions).asJava


}
