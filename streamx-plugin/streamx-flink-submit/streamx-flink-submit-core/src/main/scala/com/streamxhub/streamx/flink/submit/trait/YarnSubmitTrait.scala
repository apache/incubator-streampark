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

package com.streamxhub.streamx.flink.submit.`trait`

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.enums.DevelopmentMode
import com.streamxhub.streamx.common.util.ExceptionUtils
import com.streamxhub.streamx.flink.submit.domain._
import org.apache.commons.cli.CommandLine
import org.apache.flink.client.cli.{ClientOptions, CustomCommandLine}
import org.apache.flink.client.deployment.ClusterSpecification
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClientProvider
import org.apache.flink.configuration.{CheckpointingOptions, ConfigOptions, Configuration, CoreOptions}
import org.apache.flink.runtime.jobgraph.JobGraph
import org.apache.flink.util.FlinkException
import org.apache.flink.yarn.configuration.YarnConfigOptions
import org.apache.flink.yarn.{YarnClusterClientFactory, YarnClusterDescriptor}
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.lang.reflect.Method
import java.lang.{Boolean => JavaBool}
import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import scala.util.Try

/**
 * yarn application mode submit
 */
trait YarnSubmitTrait extends FlinkSubmitTrait {

  lazy val workspace: Workspace = Workspace.remote

  override def doStop(stopRequest: StopRequest): StopResponse = {

    val jobID = getJobID(stopRequest.jobId)

    val clusterClient = {
      val flinkConfiguration = new Configuration
      flinkConfiguration.set(YarnConfigOptions.APPLICATION_ID, stopRequest.clusterId)
      val clusterClientFactory = new YarnClusterClientFactory
      val applicationId = clusterClientFactory.getClusterId(flinkConfiguration)
      if (applicationId == null) {
        throw new FlinkException("[StreamX] getClusterClient error. No cluster id was specified. Please specify a cluster to which you would like to connect.")
      }
      val clusterDescriptor = clusterClientFactory.createClusterDescriptor(flinkConfiguration)
      clusterDescriptor.retrieve(applicationId).getClusterClient
    }

    val savePointDir = getOptionFromDefaultFlinkConfig(
      stopRequest.flinkVersion.flinkHome,
      ConfigOptions.key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
        .stringType()
        .defaultValue(s"${workspace.APP_SAVEPOINTS}")
    )

    val savepointPathFuture = (Try(stopRequest.withSavePoint).getOrElse(false), Try(stopRequest.withDrain).getOrElse(false)) match {
      case (false, false) =>
        clusterClient.cancel(jobID)
        null
      case (true, false) => clusterClient.cancelWithSavepoint(jobID, savePointDir)
      case (_, _) => clusterClient.stopWithSavepoint(jobID, stopRequest.withDrain, savePointDir)
    }

    if (savepointPathFuture == null) null else try {
      val clientTimeout = getOptionFromDefaultFlinkConfig(stopRequest.flinkVersion.flinkHome, ClientOptions.CLIENT_TIMEOUT)
      val savepointDir = savepointPathFuture.get(clientTimeout.toMillis, TimeUnit.MILLISECONDS)
      StopResponse(savepointDir)
    } catch {
      case e: Exception =>
        val cause = ExceptionUtils.stringifyException(e)
        throw new FlinkException(s"[StreamX] Triggering a savepoint for the job ${stopRequest.jobId} failed. $cause");
    }
  }

  lazy private val deployInternalMethod: Method = {
    val paramClass = Array(
      classOf[ClusterSpecification],
      classOf[String],
      classOf[String],
      classOf[JobGraph],
      Boolean2boolean(true).getClass // get boolean class.
    )
    val deployInternal = classOf[YarnClusterDescriptor].getDeclaredMethod("deployInternal", paramClass: _*)
    deployInternal.setAccessible(true)
    deployInternal
  }

  private[submit] def getParallelism(submitRequest: SubmitRequest): Integer = {
    if (submitRequest.property.containsKey(KEY_FLINK_PARALLELISM())) {
      Integer.valueOf(submitRequest.property.get(KEY_FLINK_PARALLELISM()).toString)
    } else {
      val parallelism = getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome).getInteger(CoreOptions.DEFAULT_PARALLELISM, -1)
      if (parallelism == -1) null else parallelism
    }
  }

  /**
   * 页面定义参数优先级 > flink-conf.yaml中配置优先级
   *
   * @param submitRequest
   * @param activeCustomCommandLine
   * @param commandLine
   * @return
   */
  private[submit] def applyConfiguration(submitRequest: SubmitRequest,
                                         activeCustomCommandLine: CustomCommandLine,
                                         commandLine: CommandLine): Configuration = {

    require(activeCustomCommandLine != null, "YarnSubmitTrait.applyConfiguration: activeCustomCommandLine must not be null.")
    val executorConfig = activeCustomCommandLine.toConfiguration(commandLine)
    val customConfiguration = new Configuration(executorConfig)
    val configuration = new Configuration()
    //flink-conf.yaml配置
    val flinkDefaultConfiguration = getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome)
    flinkDefaultConfiguration.keySet.foreach(x => {
      flinkDefaultConfiguration.getString(x, null) match {
        case v if v != null => configuration.setString(x, v)
        case _ =>
      }
    })
    configuration.addAll(customConfiguration)
    //main class
    if (submitRequest.developmentMode == DevelopmentMode.CUSTOMCODE) {
      configuration.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, submitRequest.appMain)
    }
    configuration
  }

  private[submit] def deployInternal(clusterDescriptor: YarnClusterDescriptor,
                                     clusterSpecification: ClusterSpecification,
                                     applicationName: String,
                                     yarnClusterEntrypoint: String,
                                     jobGraph: JobGraph,
                                     detached: JavaBool): ClusterClientProvider[ApplicationId] = {
    deployInternalMethod.invoke(
      clusterDescriptor,
      clusterSpecification,
      applicationName,
      yarnClusterEntrypoint,
      jobGraph,
      detached
    ).asInstanceOf[ClusterClientProvider[ApplicationId]]
  }

}
