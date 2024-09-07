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

import org.apache.streampark.common.util.{ExceptionUtils, HadoopUtils}
import org.apache.streampark.common.util.Implicits._
import org.apache.streampark.flink.client.bean._

import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.ClusterSpecification
import org.apache.flink.client.program.{ClusterClient, ClusterClientProvider}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.jobgraph.JobGraph
import org.apache.flink.util.FlinkException
import org.apache.flink.yarn.{YarnClusterClientFactory, YarnClusterDescriptor}
import org.apache.flink.yarn.configuration.YarnConfigOptions
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.lang.{Boolean => JavaBool}
import java.lang.reflect.Method
import java.security.PrivilegedAction

import scala.util.{Failure, Success, Try}

/** yarn application mode submit */
trait YarnClientTrait extends FlinkClientTrait {

  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    flinkConfig
      .safeSet(YarnConfigOptions.APPLICATION_NAME, submitRequest.effectiveAppName)
      .safeSet(YarnConfigOptions.APPLICATION_TYPE, submitRequest.applicationType.getName)
      .safeSet(YarnConfigOptions.APPLICATION_TAGS, "streampark")
  }

  private[this] def executeClientAction[R <: SavepointRequestTrait, O](
      request: R,
      flinkConf: Configuration,
      actionFunc: (JobID, ClusterClient[_]) => O): O = {
    val jobID = getJobID(request.jobId)
    flinkConf.safeSet(YarnConfigOptions.APPLICATION_ID, request.clusterId)
    // Get the ClusterClient from the YarnClusterDescriptor
    val (applicationId: ApplicationId, clusterDescriptor: YarnClusterDescriptor) = getYarnClusterDescriptor(flinkConf)
    val clusterClient = clusterDescriptor.retrieve(applicationId).getClusterClient

    Try {
      actionFunc(jobID, clusterClient)
    }.recover {
      case e =>
        throw new FlinkException(
          s"[StreamPark] Do ${request.getClass.getSimpleName} for the job ${request.jobId} failed. " +
            s"detail: ${ExceptionUtils.stringifyException(e)}");
    }.get
  }

  override def doTriggerSavepoint(
      savepointRequest: TriggerSavepointRequest,
      flinkConf: Configuration): SavepointResponse = {
    executeClientAction(
      savepointRequest,
      flinkConf,
      (jid, client) => {
        SavepointResponse(super.triggerSavepoint(savepointRequest, jid, client))
      })
  }

  override def doCancel(cancelRequest: CancelRequest, flinkConf: Configuration): CancelResponse = {
    executeClientAction(
      cancelRequest,
      flinkConf,
      (jid, client) => {
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
    val deployInternal =
      classOf[YarnClusterDescriptor].getDeclaredMethod("deployInternal", paramClass: _*)
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
    deployInternalMethod
      .invoke(
        clusterDescriptor,
        clusterSpecification,
        applicationName,
        yarnClusterEntrypoint,
        jobGraph,
        detached)
      .asInstanceOf[ClusterClientProvider[ApplicationId]]
  }

  /**
   * Retrieves the YarnClusterDescriptor and the application ID.
   *
   * @param flinkConfig
   *   the Flink configuration
   * @return
   *   a tuple containing the application ID and the YarnClusterDescriptor
   */
  private[client] def getYarnClusterDescriptor(
      flinkConfig: Configuration,
      user: String = ""): (ApplicationId, YarnClusterDescriptor) = {
    Try {
      doAsYarnClusterDescriptor[ApplicationId](
        user,
        () => {
          val clientFactory = new YarnClusterClientFactory
          // Get the cluster ID
          val yarnClusterId: ApplicationId = clientFactory.getClusterId(flinkConfig)
          require(yarnClusterId != null)
          // Create the ClusterDescriptor
          val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
          (yarnClusterId, clusterDescriptor)
        })
    } match {
      case Success(result) => result
      case Failure(e) =>
        throw new IllegalArgumentException(s"[StreamPark] access ClusterDescriptor error: $e")
    }
  }

  /**
   * Retrieves the ClusterSpecification and the YarnClusterDescriptor for deployment.
   *
   * @param flinkConfig
   *   the Flink configuration
   * @return
   *   a tuple containing the ClusterSpecification and the YarnClusterDescriptor
   */
  private[client] def getYarnClusterDeployDescriptor(
      flinkConfig: Configuration,
      user: String = ""): (ClusterSpecification, YarnClusterDescriptor) = {
    Try {
      doAsYarnClusterDescriptor[ClusterSpecification](
        user,
        () => {
          val clientFactory = new YarnClusterClientFactory
          // Get the ClusterSpecification
          val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
          // Create the ClusterDescriptor
          val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
          clusterSpecification -> clusterDescriptor
        })
    } match {
      case Success(result) => result
      case Failure(e) =>
        throw new IllegalArgumentException(s"[StreamPark] access ClusterDescriptor error: $e")
    }
  }

  private[this] def doAsYarnClusterDescriptor[T](
      user: String,
      func: () => (T, YarnClusterDescriptor)): (T, YarnClusterDescriptor) = {
    // Wrap the operation in ugi.doAs()
    val ugi = HadoopUtils.getUgi()
    val finalUgi = if (user != null && user.nonEmpty && ugi.getShortUserName != user) UserGroupInformation.createProxyUser(user, ugi) else ugi

    try {
      finalUgi.doAs(new PrivilegedAction[(T, YarnClusterDescriptor)] {
        override def run(): (T, YarnClusterDescriptor) = func()
      })
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"[StreamPark] Error executing YarnClusterDescriptor operation as user $user", e)
    }
  }

}
