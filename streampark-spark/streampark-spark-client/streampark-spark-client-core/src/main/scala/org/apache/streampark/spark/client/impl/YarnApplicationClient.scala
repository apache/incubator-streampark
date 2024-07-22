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
import org.apache.streampark.common.enums.SparkExecutionMode
import org.apache.streampark.common.util.HadoopUtils
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse
import org.apache.streampark.spark.client.`trait`.SparkClientTrait
import org.apache.streampark.spark.client.bean._

import org.apache.commons.collections.MapUtils
import org.apache.commons.lang.StringUtils
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.spark.launcher.{SparkAppHandle, SparkLauncher}

import java.util.{Map => JavaMap}
import java.util.concurrent.{CountDownLatch, Executors, ExecutorService}

/** yarn application mode submit */
object YarnApplicationClient extends SparkClientTrait {

  private val threadPool: ExecutorService = Executors.newFixedThreadPool(1)

  private[this] lazy val workspace = Workspace.remote

  override def doStop(stopRequest: StopRequest): StopResponse = {
    HadoopUtils.yarnClient.killApplication(ApplicationId.fromString(stopRequest.appId))
    null
  }

  override def setConfig(submitRequest: SubmitRequest): Unit = {}

  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {
    launch(submitRequest)
  }

  private def launch(submitRequest: SubmitRequest): SubmitResponse = {
    val launcher: SparkLauncher = new SparkLauncher()
      .setSparkHome(submitRequest.sparkVersion.sparkHome)
      .setAppResource(submitRequest.buildResult
        .asInstanceOf[ShadedBuildResponse]
        .shadedJarPath)
      .setMainClass(submitRequest.appMain)
      .setMaster("yarn")
      .setDeployMode(submitRequest.executionMode match {
        case SparkExecutionMode.YARN_CLIENT => "client"
        case SparkExecutionMode.YARN_CLUSTER => "cluster"
        case _ =>
          throw new IllegalArgumentException(
            "[StreamPark][YarnApplicationClient] Yarn mode only support \"client\" and \"cluster\".")

      })
      .setAppName(submitRequest.appName)
      .setConf(
        "spark.yarn.jars",
        submitRequest
          .hdfsWorkspace
          .sparkLib + "/*.jar")
      .setVerbose(true)

    setYarnQueue(launcher, submitRequest.extraParameter)
    // setSparkConfiguration(launcher, submitRequest.properties)

    // TODO: Adds command line arguments for the application.
    // launcher.addAppArgs()

    if (MapUtils.isNotEmpty(submitRequest.extraParameter) && submitRequest.extraParameter
        .containsKey("sql")) {
      launcher.addAppArgs("--sql", submitRequest.extraParameter.get("sql").toString)
    }

    logger.info("[StreamPark][YarnApplicationClient] The spark task start")
    val cdlForApplicationId: CountDownLatch = new CountDownLatch(1)

    var sparkAppHandle: SparkAppHandle = null
    threadPool.execute(new Runnable {
      override def run(): Unit = {
        try {
          val countDownLatch: CountDownLatch = new CountDownLatch(1)
          sparkAppHandle = launcher.startApplication(new SparkAppHandle.Listener() {
            override def stateChanged(handle: SparkAppHandle): Unit = {
              if (handle.getAppId != null) {
                if (cdlForApplicationId.getCount != 0) {
                  cdlForApplicationId.countDown()
                }
                logger.info("{} stateChanged :{}", Array(handle.getAppId, handle.getState.toString))
              } else logger.info("stateChanged :{}", handle.getState.toString)

              if (SparkAppHandle.State.FAILED.toString == handle.getState.toString) {
                logger.error("Task run failure stateChanged :{}", handle.getState.toString)
              }

              if (handle.getState.isFinal) {
                countDownLatch.countDown()
              }
            }

            override def infoChanged(handle: SparkAppHandle): Unit = {}
          })
          countDownLatch.await()
        } catch {
          case e: Exception =>
            logger.error(e.getMessage, e)
        }
      }
    })

    cdlForApplicationId.await()
    logger.info(
      "[StreamPark][YarnApplicationClient] The task is executing, handle current state is {}, appid is {}",
      Array(sparkAppHandle.getState.toString, sparkAppHandle.getAppId))
    SubmitResponse(sparkAppHandle.getAppId)
  }

//  private def setSparkConfiguration(sparkLauncher: SparkLauncher, properties: Map[String, Any]): Unit = {
//    logger.info("[StreamPark][YarnApplicationClient] Spark launcher start configuration.")
//    val finalProperties: Map[String, Any] = SparkConfiguration.defaultParameters ++ properties
//    for ((k, v) <- finalProperties) {
//      if (k.startsWith("spark.")) {
//        sparkLauncher.setConf(k, v.toString)
//      } else {
//        logger.info("[StreamPark][YarnApplicationClient] \"{}\" doesn't start with \"spark.\". Skip it.", k)
//      }
//    }
//  }

  private def setYarnQueue(sparkLauncher: SparkLauncher, map: JavaMap[String, Any]): Unit = {
    logger.info("[StreamPark][YarnApplicationClient] Spark launcher start setting yarn queue.")
    Option(map.get("yarnQueueName")).map(_.asInstanceOf[String]).filter(StringUtils.isNotBlank).foreach(sparkLauncher.setConf("spark.yarn.queue", _))
    Option(map.get("yarnQueueLabel")).map(_.asInstanceOf[String]).filter(StringUtils.isNotBlank).foreach(_ => {
      sparkLauncher.setConf("spark.yarn.am.nodeLabelExpression", _)
      sparkLauncher.setConf("spark.yarn.executor.nodeLabelExpression", _)
    })
  }

}
