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

package org.apache.streampark.flink.submit

import org.apache.streampark.common.util.{Logger, Utils}
import org.apache.streampark.flink.proxy.FlinkShimsProxy
import org.apache.streampark.flink.submit.bean._
import org.apache.commons.lang3.StringUtils

import java.util
import java.util.regex.Pattern
import java.util.{Map => JavaMap}
import javax.annotation.Nonnull
import scala.collection.JavaConverters._
import scala.collection.mutable

object FlinkSubmitter extends Logger {

  private[this] lazy val PROPERTY_PATTERN = Pattern.compile("(.*?)=(.*?)")

  private[this] lazy val MULTI_PROPERTY_REGEXP = "-D(.*?)\\s*=\\s*\\\"(.*)\\\""

  private[this] lazy val MULTI_PROPERTY_PATTERN = Pattern.compile(MULTI_PROPERTY_REGEXP)

  private[this] val FLINK_SUBMIT_CLASS_NAME = "org.apache.streampark.flink.submit.FlinkSubmit"

  private[this] val SUBMIT_REQUEST_CLASS_NAME = "org.apache.streampark.flink.submit.bean.SubmitRequest"

  private[this] val DEPLOY_REQUEST_CLASS_NAME = "org.apache.streampark.flink.submit.bean.DeployRequest"

  private[this] val CANCEL_REQUEST_CLASS_NAME = "org.apache.streampark.flink.submit.bean.CancelRequest"

  private[this] val SHUTDOWN_REQUEST_CLASS_NAME = "org.apache.streampark.flink.submit.bean.ShutDownRequest"

  def submit(submitRequest: SubmitRequest): SubmitResponse = {
    FlinkShimsProxy.proxy(submitRequest.flinkVersion, (classLoader: ClassLoader) => {
      val submitClass = classLoader.loadClass(FLINK_SUBMIT_CLASS_NAME)
      val requestClass = classLoader.loadClass(SUBMIT_REQUEST_CLASS_NAME)
      val method = submitClass.getDeclaredMethod("submit", requestClass)
      method.setAccessible(true)
      val obj = method.invoke(null, FlinkShimsProxy.getObject(classLoader, submitRequest))
      FlinkShimsProxy.getObject[SubmitResponse](this.getClass.getClassLoader, obj)
    })
  }

  def cancel(stopRequest: CancelRequest): CancelResponse = {
    FlinkShimsProxy.proxy(stopRequest.flinkVersion, (classLoader: ClassLoader) => {
      val submitClass = classLoader.loadClass(FLINK_SUBMIT_CLASS_NAME)
      val requestClass = classLoader.loadClass(CANCEL_REQUEST_CLASS_NAME)
      val method = submitClass.getDeclaredMethod("cancel", requestClass)
      method.setAccessible(true)
      val obj = method.invoke(null, FlinkShimsProxy.getObject(classLoader, stopRequest))
      if (obj == null) null; else {
        FlinkShimsProxy.getObject[CancelResponse](this.getClass.getClassLoader, obj)
      }
    })
  }

  def deploy(deployRequest: DeployRequest): DeployResponse = {
    FlinkShimsProxy.proxy(deployRequest.flinkVersion, (classLoader: ClassLoader) => {
      val submitClass = classLoader.loadClass(FLINK_SUBMIT_CLASS_NAME)
      val requestClass = classLoader.loadClass(DEPLOY_REQUEST_CLASS_NAME)
      val method = submitClass.getDeclaredMethod("deploy", requestClass)
      method.setAccessible(true)
      val obj = method.invoke(null, FlinkShimsProxy.getObject(classLoader, deployRequest))
      FlinkShimsProxy.getObject[DeployResponse](this.getClass.getClassLoader, obj)
    })
  }

  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    FlinkShimsProxy.proxy(shutDownRequest.flinkVersion, (classLoader: ClassLoader) => {
      val submitClass = classLoader.loadClass(FLINK_SUBMIT_CLASS_NAME)
      val requestClass = classLoader.loadClass(SHUTDOWN_REQUEST_CLASS_NAME)
      val method = submitClass.getDeclaredMethod("shutdown", requestClass)
      method.setAccessible(true)
      val obj = method.invoke(null, FlinkShimsProxy.getObject(classLoader, shutDownRequest))
      FlinkShimsProxy.getObject[ShutDownResponse](this.getClass.getClassLoader, obj)
    })
  }

  /**
   * extract flink configuration from application.properties
   */
  @Nonnull def extractDynamicProperties(properties: String): Map[String, String] = {
    if (StringUtils.isEmpty(properties)) Map.empty[String, String] else {
      val map = mutable.Map[String, String]()
      val simple = properties.replaceAll(MULTI_PROPERTY_REGEXP, "")
      simple.split("\\s?-D") match {
        case d if Utils.notEmpty(d) => d.foreach(x => {
          if (x.nonEmpty) {
            val p = PROPERTY_PATTERN.matcher(x.trim)
            if (p.matches) {
              map += p.group(1).trim -> p.group(2).trim
            }
          }
        })
        case _ =>
      }
      val matcher = MULTI_PROPERTY_PATTERN.matcher(properties)
      while (matcher.find()) {
        val opts = matcher.group()
        val index = opts.indexOf("=")
        val key = opts.substring(2, index).trim
        val value = opts.substring(index + 1).trim.replaceAll("(^\"|\"$)", "")
        map += key -> value
      }
      map.toMap
    }
  }

  @Nonnull def extractDynamicPropertiesAsJava(properties: String): JavaMap[String, String] = new util.HashMap[String, String](extractDynamicProperties(properties).asJava)


}
