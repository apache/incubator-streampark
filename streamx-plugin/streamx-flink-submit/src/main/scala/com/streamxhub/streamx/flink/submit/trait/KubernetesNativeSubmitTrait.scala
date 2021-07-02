/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.submit.`trait`

import com.streamxhub.streamx.common.enums.{DevelopmentMode, ExecutionMode}
import com.streamxhub.streamx.common.util.Utils
import com.streamxhub.streamx.flink.submit.SubmitRequest
import org.apache.commons.collections.MapUtils
import org.apache.commons.lang.StringUtils
import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.deployment.{ClusterSpecification, DefaultClusterClientServiceLoader}
import org.apache.flink.configuration.{Configuration, DeploymentOptions}
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions

import java.lang
import java.lang.{Boolean => JavaBool}
import java.util.regex.Pattern
import javax.annotation.Nonnull
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
 * kubernetes native mode submit
 */
trait KubernetesNativeSubmitTrait extends FlinkSubmitTrait {

  // effective k-v regex pattern of submit.dynamicOption
  private val DYNAMIC_OPTION_ITEM_PATTERN = Pattern.compile("(-D)?(\\S+)=(\\S+)")


  // todo doStop method needs to be refactored to provide kubernetes.cluster-id, kubernetes.namespace
  // Tip: Perhaps it would be better to let users freely specify the savepoint directory
  protected def doStop(@Nonnull executeMode: ExecutionMode,
                       flinkHome: String,
                       appId: String,
                       jobStringId: String,
                       savePoint: lang.Boolean,
                       drain: lang.Boolean): String = {
    val flinkConfig = new Configuration()
      .set(DeploymentOptions.TARGET, executeMode)
      .set(KubernetesConfigOptions.CLUSTER_ID, appId)
      .set(KubernetesConfigOptions.NAMESPACE, KubernetesConfigOptions.NAMESPACE.defaultValue())

    val clusterDescriptor = getK8sClusterDescriptor(flinkConfig)
    val client = clusterDescriptor.retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID)).getClusterClient
    val jobID = JobID.fromHexString(jobStringId)
    val savePointDir = getOptionFromDefaultFlinkConfig(flinkHome, SavepointConfigOptions.SAVEPOINT_PATH) // todo request to refactor StreamX 's file system abstraction

    val actionResult = {
      if (drain) {
        client.stopWithSavepoint(jobID, drain, savePointDir).get()
      } else if (savePoint) {
        client.cancelWithSavepoint(jobID, savePointDir).get()
      } else {
        client.cancel(jobID).get()
        ""
      }
    }
    actionResult
  }

  /*
  tips:
  The default kubernetes cluster communication information will be obtained from ./kube/conf file.

  If you need to customize the kubernetes cluster context, such as multiple target kubernetes clusters
  or multiple kubernetes api-server accounts, there are two ways to achieve this：
   1. Get the KubernetesClusterDescriptor by manually, building the FlinkKubeClient and specify
       the kubernetes context contents in the FlinkKubeClient.
   2. Specify an explicit key kubernetes.config.file in flinkConfig instead of the default value.

  todo: Perhaps we need to manage the KubernetesClusterDescriptor instances through CachePool
  */
  def getK8sClusterDescriptorAndSpecification(flinkConfig: Configuration)
  : (KubernetesClusterDescriptor, ClusterSpecification) = {
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory(flinkConfig)
    val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[KubernetesClusterDescriptor]
    val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
    (clusterDescriptor, clusterSpecification)
  }

  def getK8sClusterDescriptor(flinkConfig: Configuration): KubernetesClusterDescriptor = {
    getK8sClusterDescriptorAndSpecification(flinkConfig)._1
  }


  /**
   * extract all necessary flink configuration from submitRequest
   */
  @Nonnull
  def extractEffectiveFlinkConfig(@Nonnull submitRequest: SubmitRequest): Configuration = {

    // base from default config
    val flinkConfig = {
      val defaultFlinkConfig = {
        try {
          submitRequest.flinkDefaultConfiguration
        } catch {
          case e: Exception => new Configuration
        }
      }
      if (defaultFlinkConfig != null) defaultFlinkConfig else new Configuration
    }

    // extract from submitRequest
    flinkConfig
      .set(DeploymentOptions.TARGET, submitRequest.executionMode.getName)
      .set(KubernetesConfigOptions.CLUSTER_ID, "") // todo information missing, or maybe overridden by submitRequest.property/dynamicOption
      .set(KubernetesConfigOptions.NAMESPACE, "") // todo Id.
      .set(DeploymentOptions.SHUTDOWN_IF_ATTACHED, JavaBool.FALSE)

    if (DevelopmentMode.CUSTOMCODE == submitRequest.developmentMode) {
      flinkConfig.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, submitRequest.appMain)
    }
    if (StringUtils.isNotBlank(submitRequest.savePoint)) {
      flinkConfig.set(SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.savePoint)
    }
    if (StringUtils.isNotBlank(submitRequest.option)
      && submitRequest.option.split("\\s+").contains("-n")) {
      // please transfer the execution.savepoint.ignore-unclaimed-state to submitRequest.property or dynamicOption
      flinkConfig.set(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE, JavaBool.TRUE)
    }
    if (StringUtils.isNotBlank(submitRequest.args)) {
      val args = new ArrayBuffer[String]
      submitRequest.args.split("\\s+").foreach(args += _)
      flinkConfig.set(ApplicationConfiguration.APPLICATION_ARGS, args.toList.asJava)
    }

    // copy from submitRequest.property
    if (MapUtils.isNotEmpty(submitRequest.property)) {
      submitRequest.property
        .filter(_._2 != null)
        .foreach(e => flinkConfig.setString(e._1, e._2.toString))
    }
    // copy from submitRequest.dynamicOption
    extractDynamicOption(submitRequest.dynamicOption)
      .foreach(e => flinkConfig.setString(e._1, e._2))

    if (flinkConfig.get(KubernetesConfigOptions.NAMESPACE).isEmpty) flinkConfig.removeConfig(KubernetesConfigOptions.NAMESPACE)

    flinkConfig
  }


  /**
   * extract flink configuration from submitRequest.dynamicOption
   */
  @Nonnull
  def extractDynamicOption(dynamicOption: Array[String]): Map[String, String] = {
    if (Utils.isEmpty(dynamicOption)) {
      return Map.empty
    }
    dynamicOption
      .filter(_ != null)
      .map(_.trim)
      .map(DYNAMIC_OPTION_ITEM_PATTERN.matcher(_))
      .filter(!_.matches())
      .map(m => m.group(2) -> m.group(3))
      .toMap
  }


}
