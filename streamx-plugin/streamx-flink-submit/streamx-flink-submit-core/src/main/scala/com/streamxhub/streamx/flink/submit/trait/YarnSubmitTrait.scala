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

import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.util.ExceptionUtils
import com.streamxhub.streamx.flink.submit.bean._
import org.apache.flink.client.cli.ClientOptions
import org.apache.flink.client.deployment.ClusterSpecification
import org.apache.flink.client.program.ClusterClientProvider
import org.apache.flink.configuration.{CheckpointingOptions, ConfigOptions, Configuration}
import org.apache.flink.runtime.jobgraph.JobGraph
import org.apache.flink.util.FlinkException
import org.apache.flink.yarn.configuration.YarnConfigOptions
import org.apache.flink.yarn.{YarnClusterClientFactory, YarnClusterDescriptor}
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.lang.reflect.Method
import java.lang.{Boolean => JavaBool}
import java.util.concurrent.TimeUnit
import scala.util.Try

/**
 * yarn application mode submit
 */
trait YarnSubmitTrait extends FlinkSubmitTrait {

  lazy val workspace: Workspace = Workspace.remote

  override def doStop(stopRequest: StopRequest, flinkConf: Configuration): StopResponse = {

    val jobID = getJobID(stopRequest.jobId)

    val clusterClient = {
      flinkConf.safeSet(YarnConfigOptions.APPLICATION_ID, stopRequest.clusterId)
      val clusterClientFactory = new YarnClusterClientFactory
      val applicationId = clusterClientFactory.getClusterId(flinkConf)
      if (applicationId == null) {
        throw new FlinkException("[StreamX] getClusterClient error. No cluster id was specified. Please specify a cluster to which you would like to connect.")
      }
      val clusterDescriptor = clusterClientFactory.createClusterDescriptor(flinkConf)
      clusterDescriptor.retrieve(applicationId).getClusterClient
    }

    val savePointDir = getOptionFromDefaultFlinkConfig(
      stopRequest.flinkVersion.flinkHome,
      ConfigOptions.key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
        .stringType()
        .defaultValue(s"${workspace.APP_SAVEPOINTS}")
    )

    try {
      val clientTimeout = getOptionFromDefaultFlinkConfig(stopRequest.flinkVersion.flinkHome, ClientOptions.CLIENT_TIMEOUT)
      val savepointDir = (Try(stopRequest.withSavePoint).getOrElse(false), Try(stopRequest.withDrain).getOrElse(false)) match {
        case (false, false) =>
          clusterClient.cancel(jobID).get()
          null
        case (true, false) => clusterClient.cancelWithSavepoint(jobID, savePointDir).get(clientTimeout.toMillis, TimeUnit.MILLISECONDS)
        case (_, _) => clusterClient.stopWithSavepoint(jobID, stopRequest.withDrain, savePointDir).get(clientTimeout.toMillis, TimeUnit.MILLISECONDS)
      }
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
