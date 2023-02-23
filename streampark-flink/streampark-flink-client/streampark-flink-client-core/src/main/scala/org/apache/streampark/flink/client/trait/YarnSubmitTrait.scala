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

package org.apache.streampark.flink.client.`trait`

import org.apache.flink.api.common.JobID

import java.lang.{Boolean => JavaBool}
import java.lang.reflect.Method
import scala.util.Try
import org.apache.flink.client.deployment.{ClusterDescriptor, ClusterSpecification, DefaultClusterClientServiceLoader}
import org.apache.flink.client.program.{ClusterClient, ClusterClientProvider}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.jobgraph.JobGraph
import org.apache.flink.util.FlinkException
import org.apache.flink.yarn.{YarnClusterClientFactory, YarnClusterDescriptor}
import org.apache.flink.yarn.configuration.YarnConfigOptions
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.client.bean._

/**
 * yarn application mode submit
 */
trait YarnSubmitTrait extends FlinkSubmitTrait {

  private[this] def executeClientAction[R <: SavepointRequestTrait, O](request: R, flinkConf: Configuration,
                                                         actionFunc: (JobID, ClusterClient[_]) => O): O = {
    val jobID = getJobID(request.jobId)
    val clusterClient = {
      flinkConf.safeSet(YarnConfigOptions.APPLICATION_ID, request.clusterId)
      val clusterClientFactory = new YarnClusterClientFactory
      val applicationId = clusterClientFactory.getClusterId(flinkConf)
      if (applicationId == null) {
        throw new FlinkException(
          "[StreamPark] getClusterClient error. No cluster id was specified. Please specify a cluster to which you would like to connect.")
      }
      val clusterDescriptor = clusterClientFactory.createClusterDescriptor(flinkConf)
      clusterDescriptor.retrieve(applicationId).getClusterClient
    }
    Try {
      actionFunc(jobID, clusterClient)
    }.recover {
      case e => throw new FlinkException(
        s"[StreamPark] Do ${request.getClass.getSimpleName} for the job ${request.jobId} failed. " +
          s"detail: ${Utils.stringifyException(e)}");
    }.get
  }

  override def doTriggerSavepoint(request: TriggerSavepointRequest, flinkConf: Configuration): SavepointResponse = {
    executeClientAction(request, flinkConf, (jid, client) => {
            SavepointResponse(super.triggerSavepoint(request, jid, client))
    })
  }

  override def doCancel(cancelRequest: CancelRequest, flinkConf: Configuration): CancelResponse = {
    executeClientAction(cancelRequest, flinkConf, (jid, client) => {
      CancelResponse(super.cancelJob(cancelRequest, jid, client))
    })
  }

  private lazy val deployInternalMethod: Method = {
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

  private[client] def deployInternal(
      clusterDescriptor: YarnClusterDescriptor,
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
      detached).asInstanceOf[ClusterClientProvider[ApplicationId]]
  }

  private[client] def getSessionClusterDescriptor[T <: ClusterDescriptor[ApplicationId]](flinkConfig: Configuration): (ApplicationId, T) = {
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory[ApplicationId](flinkConfig)
    val yarnClusterId: ApplicationId = clientFactory.getClusterId(flinkConfig)
    require(yarnClusterId != null)
    val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[T]
    (yarnClusterId, clusterDescriptor)
  }

  private[client] def getSessionClusterDeployDescriptor[T <: ClusterDescriptor[ApplicationId]](flinkConfig: Configuration): (ClusterSpecification, T) = {
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory[ApplicationId](flinkConfig)
    val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
    val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[T]
    (clusterSpecification, clusterDescriptor)
  }
}
