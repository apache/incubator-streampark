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

package org.apache.streampark.spark.client.impl

import org.apache.streampark.common.conf.Workspace
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse
import org.apache.streampark.spark.client.`trait`.SparkClientTrait
import org.apache.streampark.spark.client.bean._

import org.apache.commons.collections.MapUtils
import org.apache.spark.launcher.{SparkAppHandle, SparkLauncher}

import java.util.concurrent.{CountDownLatch, Executors, ExecutorService}

import scala.util.control.Breaks.break

/** yarn application mode submit */
object YarnApplicationClient extends SparkClientTrait {

  private val threadPool: ExecutorService = Executors.newFixedThreadPool(1)

  private[this] lazy val workspace = Workspace.remote

  override def doCancel(cancelRequest: CancelRequest): CancelResponse = {
    null
  }

  override def setConfig(submitRequest: SubmitRequest): Unit = {}

  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {
    launch(submitRequest)
    null

  }

  private def launch(submitRequest: SubmitRequest): Unit = {
    val launcher: SparkLauncher = new SparkLauncher()
      .setSparkHome(submitRequest.sparkVersion.sparkHome)
      .setAppResource(submitRequest.buildResult
        .asInstanceOf[ShadedBuildResponse]
        .shadedJarPath)
      .setMainClass(submitRequest.appMain)
      .setMaster("yarn")
      .setDeployMode("cluster")
      .setAppName(submitRequest.appName)
      .setConf("spark.executor.memory", "5g")
      .setConf("spark.executor.cores", "4")
      .setConf("spark.num.executors", "1")
      .setConf(
        "spark.yarn.jars",
        submitRequest
          .asInstanceOf[SubmitRequest]
          .hdfsWorkspace
          .sparkLib + "/*.jar")
      .setVerbose(true)

    if (MapUtils.isNotEmpty(submitRequest.extraParameter) && submitRequest.extraParameter
        .containsKey("sql")) {
      launcher.addAppArgs("--sql", submitRequest.extraParameter.get("sql").toString)
    }

    logger.info("The spark task start")

    threadPool.execute(new Runnable {
      override def run(): Unit = {
        try {
          val countDownLatch: CountDownLatch = new CountDownLatch(1)
          val sparkAppHandle: SparkAppHandle =
            launcher.startApplication(new SparkAppHandle.Listener() {
              override def stateChanged(handle: SparkAppHandle): Unit = {
                if (handle.getAppId != null) {
                  logInfo(
                    String.format("%s stateChanged :%s", handle.getAppId, handle.getState.toString))
                } else logger.info("stateChanged :{}", handle.getState.toString)

                if (SparkAppHandle.State.FAILED.toString == handle.getState.toString) {
                  logger.error("Task run failure stateChanged :{}", handle.getState.toString)
                }

                if (handle.getState.isFinal) countDownLatch.countDown()
              }

              override def infoChanged(handle: SparkAppHandle): Unit = {}
            })
          logger.info(
            "The task is executing, current is get application id before,please wait ........")
          var applicationId: String = null
          while ({
            !(SparkAppHandle.State.RUNNING == sparkAppHandle.getState)
          }) {
            applicationId = sparkAppHandle.getAppId
            if (applicationId != null) {
              logInfo(
                String.format(
                  "handle current state is %s, appid is %s",
                  sparkAppHandle.getState.toString,
                  applicationId))
              break // todo: break is not supported

            }
          }
          logInfo(
            String.format(
              "handle current state is %s, appid is %s",
              sparkAppHandle.getState.toString,
              applicationId))
          countDownLatch.await()
        } catch {
          case e: Exception =>
            logger.error(e.getMessage, e)
        }
      }
    })

  }

}
