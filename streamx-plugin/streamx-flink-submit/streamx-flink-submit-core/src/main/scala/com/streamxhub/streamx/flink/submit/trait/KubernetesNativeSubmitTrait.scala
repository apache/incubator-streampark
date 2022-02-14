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
import com.streamxhub.streamx.common.enums.{DevelopmentMode, ExecutionMode, FlinkK8sRestExposedType}
import com.streamxhub.streamx.flink.packer.pipeline.FlinkK8sApplicationBuildResponse
import com.streamxhub.streamx.flink.submit.FlinkSubmitHelper.extractDynamicOption
import com.streamxhub.streamx.flink.submit.domain._
import org.apache.commons.collections.MapUtils
import org.apache.commons.lang3.StringUtils
import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.ClusterSpecification
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions.ServiceExposedType
import org.apache.flink.kubernetes.{KubernetesClusterClientFactory, KubernetesClusterDescriptor}
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions

import java.io.File
import java.lang.{Boolean => JavaBool}
import javax.annotation.Nonnull
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.util.Try

/**
 * kubernetes native mode submit
 */
//noinspection DuplicatedCode
trait KubernetesNativeSubmitTrait extends FlinkSubmitTrait {

  lazy val workspace: Workspace = Workspace.local

  private[submit] val fatJarCached = new mutable.HashMap[String, File]()

  @throws[Exception]
  protected def checkBuildResult(submitRequest: SubmitRequest): Unit = {
    val result = submitRequest.buildResult
    if (result == null) {
      throw new Exception("[flink-submit] current flink app was not yet built, buildResult is empty" +
        s",clusterId=${submitRequest.k8sSubmitParam.clusterId}," +
        s",namespace=${submitRequest.k8sSubmitParam.kubernetesNamespace}")
    }
    if (!result.pass) {
      throw new Exception(s"[flink-submit] current flink app build failed, clusterId" +
        s",clusterId=${submitRequest.k8sSubmitParam.clusterId}," +
        s",namespace=${submitRequest.k8sSubmitParam.kubernetesNamespace}")
    }
  }

  // Tip: Perhaps it would be better to let users freely specify the savepoint directory
  @throws[Exception] protected def doStop(@Nonnull executeMode: ExecutionMode,
                                          stopRequest: StopRequest): StopResponse = {
    assert(StringUtils.isNotBlank(stopRequest.clusterId))

    val flinkConfig = new Configuration()
      .safeSet(DeploymentOptions.TARGET, executeMode.getName)
      .safeSet(KubernetesConfigOptions.CLUSTER_ID, stopRequest.clusterId)
      .safeSet(KubernetesConfigOptions.NAMESPACE, stopRequest.kubernetesNamespace)

    var clusterDescriptor: KubernetesClusterDescriptor = null
    var client: ClusterClient[String] = null

    try {
      clusterDescriptor = getK8sClusterDescriptor(flinkConfig)
      client = clusterDescriptor.retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID)).getClusterClient
      val jobID = JobID.fromHexString(stopRequest.jobId)
      var savePointDir = stopRequest.customSavePointPath

      if (StringUtils.isBlank(savePointDir)) {
        savePointDir = getOptionFromDefaultFlinkConfig(
          stopRequest.flinkVersion.flinkHome,
          ConfigOptions.key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
            .stringType()
            .defaultValue(s"${workspace.APP_SAVEPOINTS}")
        )
      }

      val actionResult = (stopRequest.withSavePoint, stopRequest.withDrain) match {
        case (true, true) if savePointDir.nonEmpty => client.stopWithSavepoint(jobID, true, savePointDir).get()
        case (true, false) if savePointDir.nonEmpty => client.cancelWithSavepoint(jobID, savePointDir).get()
        case _ => client.cancel(jobID).get()
          ""
      }
      StopResponse(actionResult)
    } catch {
      case e: Exception =>
        logger.error(s"[flink-submit] stop flink job failed, mode=${executeMode.toString}, stopRequest=${stopRequest}")
        throw e
    } finally {
      if (client != null) client.close()
      if (clusterDescriptor != null) clusterDescriptor.close()
    }
  }

  //noinspection DuplicatedCode
  /*
    tips:
    The default kubernetes cluster communication information will be obtained from ./kube/conf file.

    If you need to customize the kubernetes cluster context, such as multiple target kubernetes clusters
    or multiple kubernetes api-server accounts, there are two ways to achieve this：
     1. Get the KubernetesClusterDescriptor by manually, building the FlinkKubeClient and specify
         the kubernetes context contents in the FlinkKubeClient.
     2. Specify an explicit key kubernetes.config.file in flinkConfig instead of the default value.
    */
  def getK8sClusterDescriptorAndSpecification(flinkConfig: Configuration)
  : (KubernetesClusterDescriptor, ClusterSpecification) = {
    val clientFactory = new KubernetesClusterClientFactory()
    val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
    val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
    (clusterDescriptor, clusterSpecification)
  }

  def getK8sClusterDescriptor(flinkConfig: Configuration): KubernetesClusterDescriptor = {
    val clientFactory = new KubernetesClusterClientFactory()
    val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
    clusterDescriptor
  }

  /**
   * extract all necessary flink configuration from submitRequest
   */
  @Nonnull def extractEffectiveFlinkConfig(@Nonnull submitRequest: SubmitRequest): Configuration = {
    // base from default config
    val flinkConfig = Try(getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome)).getOrElse(new Configuration)

    // extract from submitRequest
    flinkConfig.set(DeploymentOptions.SHUTDOWN_IF_ATTACHED, JavaBool.FALSE)
      .safeSet(DeploymentOptions.TARGET, submitRequest.executionMode.getName)
      .safeSet(KubernetesConfigOptions.CLUSTER_ID, submitRequest.k8sSubmitParam.clusterId)
      .safeSet(KubernetesConfigOptions.NAMESPACE, submitRequest.k8sSubmitParam.kubernetesNamespace)
      .safeSet(SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.savePoint)
      .safeSet(PipelineOptions.NAME, submitRequest.appName)
      .safeSet(CoreOptions.CLASSLOADER_RESOLVE_ORDER, submitRequest.resolveOrder.getName)
      .set(KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE, covertToServiceExposedType(submitRequest.k8sSubmitParam.flinkRestExposedType))

    if (submitRequest.buildResult != null) {
      val buildResult = submitRequest.buildResult.asInstanceOf[FlinkK8sApplicationBuildResponse]

      buildResult.podTemplatePaths.foreach(p => {
        if (p._1 == KubernetesConfigOptions.KUBERNETES_POD_TEMPLATE.key()) {
          flinkConfig.set(KubernetesConfigOptions.KUBERNETES_POD_TEMPLATE, p._2)
        }
        if (p._1 == KubernetesConfigOptions.JOB_MANAGER_POD_TEMPLATE.key()) {
          flinkConfig.set(KubernetesConfigOptions.JOB_MANAGER_POD_TEMPLATE, p._2)
        }
        if (p._1 == KubernetesConfigOptions.TASK_MANAGER_POD_TEMPLATE.key()) {
          flinkConfig.set(KubernetesConfigOptions.TASK_MANAGER_POD_TEMPLATE, p._2)
        }
      })
    }

    if (DevelopmentMode.FLINKSQL == submitRequest.developmentMode) {
      flinkConfig.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, "com.streamxhub.streamx.flink.cli.SqlClient")
    } else {
      flinkConfig.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, submitRequest.appMain)
    }
    if (StringUtils.isNotBlank(submitRequest.option)
      && submitRequest.option.split("\\s+").contains("-n")) {
      // please transfer the execution.savepoint.ignore-unclaimed-state to submitRequest.property or dynamicOption
      flinkConfig.set(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE, JavaBool.TRUE)
    }
    val args = extractProgramArgs(submitRequest)
    flinkConfig.set(ApplicationConfiguration.APPLICATION_ARGS, args.toList.asJava)

    // copy from submitRequest.property
    if (MapUtils.isNotEmpty(submitRequest.property)) {
      submitRequest.property
        .filter(_._2 != null)
        .foreach(e => flinkConfig.setString(e._1, e._2.toString))
    }

    // copy from submitRequest.dynamicOption
    extractDynamicOption(submitRequest.dynamicOption)
      .foreach(e => flinkConfig.setString(e._1, e._2))


    if (flinkConfig.get(KubernetesConfigOptions.NAMESPACE).isEmpty) {
      flinkConfig.removeConfig(KubernetesConfigOptions.NAMESPACE)
    }

    flinkConfig
  }

  private[submit] implicit class EnhanceFlinkConfiguration(flinkConfig: Configuration) {
    def safeSet(option: ConfigOption[String], value: String): Configuration = {
      flinkConfig match {
        case x if StringUtils.isNotBlank(value) => x.set(option, value)
        case x => x
      }
    }
  }

  private[submit] def extractProgramArgs(submitRequest: SubmitRequest): ArrayBuffer[String] = {
    val programArgs = new ArrayBuffer[String]()
    Try(submitRequest.args.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) programArgs += x)
    programArgs += PARAM_KEY_FLINK_CONF
    programArgs += submitRequest.flinkYaml
    programArgs += PARAM_KEY_APP_NAME
    programArgs += submitRequest.effectiveAppName
    programArgs += PARAM_KEY_FLINK_PARALLELISM
    programArgs += s"${getParallelism(submitRequest)}"
    submitRequest.developmentMode match {
      case DevelopmentMode.FLINKSQL =>
        programArgs += PARAM_KEY_FLINK_SQL
        programArgs += submitRequest.flinkSQL
        if (submitRequest.appConf != null) {
          programArgs += PARAM_KEY_APP_CONF
          programArgs += submitRequest.appConf
        }
      case _ =>
        programArgs += PARAM_KEY_APP_CONF
        programArgs += submitRequest.appConf
    }
    programArgs
  }

  protected def flinkConfIdentifierInfo(@Nonnull conf: Configuration): String =
    s"executionMode=${conf.get(DeploymentOptions.TARGET)}, clusterId=${conf.get(KubernetesConfigOptions.CLUSTER_ID)}, " +
      s"namespace=${conf.get(KubernetesConfigOptions.NAMESPACE)}"


  private def covertToServiceExposedType(exposedType: FlinkK8sRestExposedType): ServiceExposedType = exposedType match {
    case FlinkK8sRestExposedType.ClusterIP => ServiceExposedType.ClusterIP
    case FlinkK8sRestExposedType.LoadBalancer => ServiceExposedType.LoadBalancer
    case FlinkK8sRestExposedType.NodePort => ServiceExposedType.NodePort
    case _ => ServiceExposedType.LoadBalancer
  }

}
