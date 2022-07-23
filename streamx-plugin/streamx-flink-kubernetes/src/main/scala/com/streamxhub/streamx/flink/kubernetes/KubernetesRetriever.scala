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

package com.streamxhub.streamx.flink.kubernetes

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.common.util.Utils.tryWithResource
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode
import com.streamxhub.streamx.flink.kubernetes.model.ClusterKey
import io.fabric8.kubernetes.client.{DefaultKubernetesClient, KubernetesClient, KubernetesClientException}
import org.apache.flink.client.cli.ClientOptions
import org.apache.flink.client.deployment.{ClusterClientFactory, DefaultClusterClientServiceLoader}
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.{Configuration, DeploymentOptions, RestOptions}
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

import java.time.Duration
import javax.annotation.Nullable
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
 * author:Al-assad
 */
object KubernetesRetriever extends Logger {

  // see org.apache.flink.client.cli.ClientOptions.CLIENT_TIMEOUT}
  val FLINK_CLIENT_TIMEOUT_SEC = 30L
  // see org.apache.flink.configuration.RestOptions.AWAIT_LEADER_TIMEOUT
  val FLINK_REST_AWAIT_TIMEOUT_SEC = 10L
  // see org.apache.flink.configuration.RestOptions.RETRY_MAX_ATTEMPTS
  val FLINK_REST_RETRY_MAX_ATTEMPTS = 2


  /**
   * get new KubernetesClient
   */
  @throws(classOf[KubernetesClientException])
  def newK8sClient(): KubernetesClient = {
    new DefaultKubernetesClient()
  }

  /**
   * check connection of kubernetes cluster
   */
  def checkK8sConnection(): Boolean = {
    Try(newK8sClient().getVersion != null).getOrElse(false)
  }

  private val clusterClientServiceLoader = new DefaultClusterClientServiceLoader()

  /**
   * get new flink cluster client of kubernetes mode
   */
  @throws(classOf[Exception])
  def newFinkClusterClient(clusterId: String,
                           @Nullable namespace: String,
                           executeMode: FlinkK8sExecuteMode.Value): ClusterClient[String] = {
    // build flink config
    val flinkConfig = new Configuration()
    flinkConfig.setString(DeploymentOptions.TARGET, executeMode.toString)
    flinkConfig.setString(KubernetesConfigOptions.CLUSTER_ID, clusterId)
    flinkConfig.set(ClientOptions.CLIENT_TIMEOUT, Duration.ofSeconds(FLINK_CLIENT_TIMEOUT_SEC))
    flinkConfig.setLong(RestOptions.AWAIT_LEADER_TIMEOUT, FLINK_REST_AWAIT_TIMEOUT_SEC * 1000)
    flinkConfig.setInteger(RestOptions.RETRY_MAX_ATTEMPTS, FLINK_REST_RETRY_MAX_ATTEMPTS)
    if (Try(namespace.isEmpty).getOrElse(true)) {
      flinkConfig.setString(KubernetesConfigOptions.NAMESPACE, KubernetesConfigOptions.NAMESPACE.defaultValue())
    } else {
      flinkConfig.setString(KubernetesConfigOptions.NAMESPACE, namespace)
    }
    // retrieve flink cluster client
    val clientFactory: ClusterClientFactory[String] = clusterClientServiceLoader.getClusterClientFactory(flinkConfig)

    val clusterProvider: KubernetesClusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[KubernetesClusterDescriptor]

    Try {
      clusterProvider
        .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
        .getClusterClient
    } match {
      case Success(v) => v
      case Failure(e) =>
        logError(s"Get flinkClient error, the error is: $e")
        throw e
    }
  }

  /**
   * check whether deployment exists on kubernetes cluster
   *
   * @param name      deployment name
   * @param namespace deployment namespace
   */
  def isDeploymentExists(name: String, namespace: String): Boolean =
    tryWithResource(Try(KubernetesRetriever.newK8sClient()).getOrElse(return false)) {
      client =>
        client.apps()
          .deployments()
          .inNamespace(namespace)
          .withLabel("type", "flink-native-kubernetes")
          .list()
          .getItems.asScala
          .exists(e => e.getMetadata.getName == name)
    } { _ => false }


  /**
   * retrieve flink jobmanger rest url
   */
  def retrieveFlinkRestUrl(clusterKey: ClusterKey): Option[String] = {
    tryWithResource(
      Try(KubernetesRetriever.newFinkClusterClient(clusterKey.clusterId, clusterKey.namespace, clusterKey.executeMode))
        .getOrElse(return None)) {
      client =>
        val url = IngressController.ingressUrlAddress(clusterKey.namespace, clusterKey.clusterId, client)
        Some(url)
    }
  }

}
