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

package org.apache.streampark.spark.client.`trait`

import org.apache.streampark.common.util._
import org.apache.streampark.spark.client.bean._

import scala.collection.convert.ImplicitConversions._

trait SparkClientTrait extends Logger {

  @throws[Exception]
  def submit(submitRequest: SubmitRequest): SubmitResponse = {
    logInfo(
      s"""
         |--------------------------------------- spark job start ---------------------------------------
         |    userSparkHome    : ${submitRequest.sparkVersion.sparkHome}
         |    sparkVersion     : ${submitRequest.sparkVersion.version}
         |    appName          : ${submitRequest.appName}
         |    devMode          : ${submitRequest.developmentMode.name()}
         |    execMode         : ${submitRequest.executionMode.name()}
         |    applicationType  : ${submitRequest.applicationType.getName}
         |    properties       : ${submitRequest.properties.mkString(" ")}
         |    args             : ${submitRequest.args}
         |    appConf          : ${submitRequest.appConf}
         |------------------------------------------------------------------------------`-------------
         |""".stripMargin)

    submitRequest.developmentMode match {
      case _ =>
        if (submitRequest.userJarFile != null) {
          val uri = submitRequest.userJarFile.getAbsolutePath
        }
    }

    setConfig(submitRequest)

    doSubmit(submitRequest)

  }

  def setConfig(submitRequest: SubmitRequest): Unit

  @throws[Exception]
  def cancel(cancelRequest: CancelRequest): CancelResponse = {
    logInfo(
      s"""
         |----------------------------------------- spark job cancel --------------------------------
         |     userSparkHome     : ${cancelRequest.sparkVersion.sparkHome}
         |     sparkVersion      : ${cancelRequest.sparkVersion.version}
         |     withDrain         : ${cancelRequest.withDrain}
         |     nativeFormat      : ${cancelRequest.nativeFormat}
         |     jobId             : ${cancelRequest.jobId}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)

    doCancel(cancelRequest)
  }

  @throws[Exception]
  def doSubmit(submitRequest: SubmitRequest): SubmitResponse

  @throws[Exception]
  def doCancel(cancelRequest: CancelRequest): CancelResponse

}
