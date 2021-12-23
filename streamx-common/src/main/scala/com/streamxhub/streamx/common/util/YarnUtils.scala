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

import java.util
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

}
