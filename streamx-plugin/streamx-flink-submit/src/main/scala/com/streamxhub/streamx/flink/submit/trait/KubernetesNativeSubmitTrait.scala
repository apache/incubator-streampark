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

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.enums.{DevelopmentMode, ExecutionMode, StorageType}
import com.streamxhub.streamx.common.fs.FsOperatorGetter
import com.streamxhub.streamx.common.util.{DeflaterUtils, Utils}
import com.streamxhub.streamx.flink.submit.domain._
import org.apache.commons.collections.MapUtils
import org.apache.commons.lang.StringUtils
import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.deployment.{ClusterSpecification, DefaultClusterClientServiceLoader}
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.{ConfigOption, Configuration, CoreOptions, DeploymentOptions, PipelineOptions}
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions

import java.io.File
import java.lang.{Boolean => JavaBool}
import java.util.regex.Pattern
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
trait KubernetesNativeSubmitTrait extends FlinkSubmitTrait {

  private[submit] val fatJarCached = new mutable.HashMap[String, File]()

  // effective k-v regex pattern of submit.dynamicOption
  private val DYNAMIC_OPTION_ITEM_PATTERN = Pattern.compile("(-D)?(\\S+)=(\\S+)")

  // Tip: Perhaps it would be better to let users freely specify the savepoint directory
  protected def doStop(@Nonnull executeMode: ExecutionMode,
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
      val savePointDir = getOptionFromDefaultFlinkConfig(stopRequest.flinkHome, SavepointConfigOptions.SAVEPOINT_PATH)
      val actionResult = {
        if (stopRequest.withDrain) {
          client.stopWithSavepoint(jobID, true, savePointDir).get()
        } else if (stopRequest.withSavePoint) {
          client.cancelWithSavepoint(jobID, savePointDir).get()
        } else {
          client.cancel(jobID).get()
          ""
        }
      }
      StopResponse(actionResult)

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
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory(flinkConfig)
    val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[KubernetesClusterDescriptor]
    val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
    (clusterDescriptor, clusterSpecification)
  }

  //noinspection DuplicatedCode
  def getK8sClusterDescriptor(flinkConfig: Configuration): KubernetesClusterDescriptor = {
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory(flinkConfig)
    val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[KubernetesClusterDescriptor]
    clusterDescriptor
  }

  /**
   * extract all necessary flink configuration from submitRequest
   */
  @Nonnull def extractEffectiveFlinkConfig(@Nonnull submitRequest: SubmitRequest): Configuration = {
    // base from default config
    val flinkConfig = Try(submitRequest.flinkDefaultConfiguration).getOrElse(new Configuration)

    // extract from submitRequest
    flinkConfig.set(DeploymentOptions.SHUTDOWN_IF_ATTACHED, JavaBool.FALSE)
      .safeSet(DeploymentOptions.TARGET, submitRequest.executionMode.getName)
      .safeSet(KubernetesConfigOptions.CLUSTER_ID, submitRequest.k8sSubmitParam.clusterId)
      .safeSet(KubernetesConfigOptions.NAMESPACE, submitRequest.k8sSubmitParam.kubernetesNamespace)
      .safeSet(SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.savePoint)
      .safeSet(KubernetesConfigOptions.CONTAINER_IMAGE, submitRequest.k8sSubmitParam.flinkDockerImage)
      .safeSet(PipelineOptions.NAME, submitRequest.appName)

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
    val args = extractProgarmArgs(submitRequest)
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

    // set parallism
    if (submitRequest.property.containsKey(KEY_FLINK_PARALLELISM())) {
      flinkConfig.set(CoreOptions.DEFAULT_PARALLELISM,
        Integer.valueOf(submitRequest.property.get(KEY_FLINK_PARALLELISM()).toString))
    } else {
      flinkConfig.set(CoreOptions.DEFAULT_PARALLELISM,
        CoreOptions.DEFAULT_PARALLELISM.defaultValue())
    }

    if (flinkConfig.get(KubernetesConfigOptions.NAMESPACE).isEmpty)
      flinkConfig.removeConfig(KubernetesConfigOptions.NAMESPACE)

    flinkConfig
  }

  /**
   * extract flink configuration from submitRequest.dynamicOption
   */
  @Nonnull def extractDynamicOption(dynamicOption: Array[String]): Map[String, String] = {
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

  private[submit] implicit class EnhanceFlinkConfiguration(flinkConfig: Configuration) {
    def safeSet(option: ConfigOption[String], value: String): Configuration = {
      flinkConfig match {
        case x if StringUtils.isNotBlank(value) => x.set(option, value)
        case x => x
      }
    }
  }

  private[submit] def extractProgarmArgs(submitRequest: SubmitRequest): ArrayBuffer[String] = {
    val programArgs = new ArrayBuffer[String]()
    Try(submitRequest.args.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) programArgs += x)
    programArgs += PARAM_KEY_FLINK_CONF
    programArgs += DeflaterUtils.zipString(submitRequest.flinkYaml)
    programArgs += PARAM_KEY_APP_NAME
    programArgs += submitRequest.effectiveAppName
    submitRequest.developmentMode match {
      case DevelopmentMode.FLINKSQL =>
        programArgs += PARAM_KEY_FLINK_SQL
        programArgs += submitRequest.flinkSQL
        if (submitRequest.appConf != null) {
          programArgs += PARAM_KEY_APP_CONF
          programArgs += submitRequest.appConf
        }
      case _ =>
        // Custom Code 必传配置文件...
        programArgs += PARAM_KEY_APP_CONF
        programArgs += submitRequest.appConf
    }
    programArgs
  }

  private[submit] def extractProvidedLibs(submitRequest: SubmitRequest): Set[String] = {
    val flinkLib = s"$APP_FLINK/${new File(submitRequest.flinkHome).getName}/lib"
    val providedLibs = ArrayBuffer(
      flinkLib,
      APP_JARS,
      APP_PLUGINS,
      submitRequest.flinkUserJar
    )
    val version = submitRequest.flinkVersion.split("\\.").map(_.trim.toInt)
    version match {
      case Array(1, 13, _) =>
        providedLibs += s"$APP_SHIMS/flink-1.13"
      case Array(1, 11 | 12, _) =>
        providedLibs += s"$APP_SHIMS/flink-1.12"
      case _ =>
        throw new UnsupportedOperationException(s"Unsupported flink version: ${submitRequest.flinkVersion}")
    }
    val jobLib = s"$APP_WORKSPACE/${submitRequest.jobID}/lib"
    if (FsOperatorGetter.get(StorageType.LFS).exists(jobLib)) {
      providedLibs += jobLib
    }

    val libSet = providedLibs.toSet
    // loop join md5sum per file
    //    val joinedMd5 = libSet.flatMap(lib =>
    //      new File(lib) match {
    //        case f if f.isFile => List(f)
    //        case d => d.listFiles().toList
    //      }
    //    ).map(f => DigestUtils.md5Hex(new FileInputStream(f))).mkString("")
    //    DigestUtils.md5Hex(joinedMd5) -> libSet
    libSet
  }


  protected def flinkConfIdentifierInfo(@Nonnull conf: Configuration): String =
    s"executionMode=${conf.get(DeploymentOptions.TARGET)}, clusterId=${conf.get(KubernetesConfigOptions.CLUSTER_ID)}, " +
      s"namespace=${conf.get(KubernetesConfigOptions.NAMESPACE)}"


}
