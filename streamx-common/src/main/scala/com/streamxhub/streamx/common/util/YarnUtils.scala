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

import org.apache.hadoop.yarn.api.records.YarnApplicationState._
import org.apache.hadoop.yarn.api.records._
import org.apache.hadoop.yarn.util.ConverterUtils
import org.apache.http.client.config.RequestConfig

import java.io.IOException
import java.util
import java.util.concurrent.Callable
import java.util.{List => JavaList}
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer


object YarnUtils {


  /**
   *
   * @param appName
   * @return
   */
  def getAppId(appName: String): JavaList[ApplicationId] = {
    val appStates = util.EnumSet.of(RUNNING, ACCEPTED, SUBMITTED)
    val appIds = try {
      HadoopUtils.yarnClient.getApplications(appStates).filter(_.getName == appName).map(_.getApplicationId)
    } catch {
      case e: Exception => e.printStackTrace()
        ArrayBuffer.empty[ApplicationId]
    }
    appIds.toList
  }

  /**
   * 查询 state
   *
   * @param appId
   * @return
   */
  def getState(appId: String): YarnApplicationState = {
    val applicationId = ConverterUtils.toApplicationId(appId)
    val state = try {
      val applicationReport = HadoopUtils.yarnClient.getApplicationReport(applicationId)
      applicationReport.getYarnApplicationState
    } catch {
      case e: Exception => e.printStackTrace()
        null
    }
    state
  }

  /**
   * 判断任务名为appName的任务，是否在yarn中运行，状态为RUNNING
   *
   * @return boolean
   * @param appName
   * @return
   */
  def isContains(appName: String): Boolean = {
    val runningApps = HadoopUtils.yarnClient.getApplications(util.EnumSet.of(RUNNING))
    if (runningApps != null) {
      runningApps.exists(_.getName == appName)
    } else {
      false
    }
  }

  def httpYarnAppInfo(appId: String): String = {
    if (null == appId) return null
    defaultHttpRetryRequest("ws/v1/cluster/apps/%s".format(appId))
  }

  /**
   * 获取app任务内proxy结果
   *
   * @param appId  applicationId
   * @param subUrl subUrl， 相关app 完整子url
   * @return
   */
  def httpYarnAppContent(appId: String, subUrl: String): String = {
    if (null == appId) return null
    defaultHttpRetryRequest(s"$appId/$subUrl")
  }

  private def defaultHttpRetryRequest(url: String): String = {
    if (null == url) return null
    val format = "%s/proxy/%s"
    try {
      defaultHttpRequest(format.format(HadoopUtils.getRMWebAppURL(), url))
    } catch {
      case e: IOException =>
        defaultHttpRequest(format.format(HadoopUtils.getRMWebAppURL(true), url))
    }
  }

  private def defaultHttpRequest(url: String): String = {
    if (HadoopUtils.hasYarnHttpKerberosAuth) HadoopUtils.doAs(new Callable[String]() {
      override def call(): String = AuthHttpClientUtils.httpGetRequest(url, RequestConfig.custom.setConnectTimeout(5000).build)
    }) else {
      HttpClientUtils.httpGetRequest(url, RequestConfig.custom.setConnectTimeout(5000).build)
    }

  }


}



