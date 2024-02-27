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

package org.apache.streampark.flink.kubernetes

import org.apache.streampark.common.util.{Logger, Utils}
import org.apache.streampark.common.util.Utils.using
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode
import org.apache.streampark.flink.kubernetes.ingress.IngressController
import org.apache.streampark.flink.kubernetes.model.ClusterKey

import io.fabric8.kubernetes.client.{DefaultKubernetesClient, KubernetesClient, KubernetesClientException}
import org.apache.flink.client.cli.ClientOptions
import org.apache.flink.client.deployment.{ClusterClientFactory, DefaultClusterClientServiceLoader}
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.{Configuration, DeploymentOptions, RestOptions}
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.hc.core5.util.Timeout

import javax.annotation.Nullable

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object KubernetesRetriever extends Logger {

  // see org.apache.flink.client.cli.ClientOptions.CLIENT_TIMEOUT}
  val FLINK_CLIENT_TIMEOUT_SEC: Timeout =
    Timeout.ofMilliseconds(ClientOptions.CLIENT_TIMEOUT.defaultValue().toMillis)

  // see org.apache.flink.configuration.RestOptions.AWAIT_LEADER_TIMEOUT
  val FLINK_REST_AWAIT_TIMEOUT_SEC: Timeout =
    Timeout.ofMilliseconds(RestOptions.AWAIT_LEADER_TIMEOUT.defaultValue())

  /** get new KubernetesClient */
  @throws(classOf[KubernetesClientException])
  def newK8sClient(): KubernetesClient = {
    new DefaultKubernetesClient()
  }

  /** check connection of kubernetes cluster */
  def checkK8sConnection(): Boolean = {
    Try(newK8sClient().getVersion != null).getOrElse(false)
  }

  private val clusterClientServiceLoader = new DefaultClusterClientServiceLoader()

  /** get new flink cluster client of kubernetes mode */
  private def newFinkClusterClient(
      clusterId: String,
      @Nullable namespace: String,
      executeMode: FlinkK8sExecuteMode.Value): Option[ClusterClient[String]] = {
    // build flink config
    val flinkConfig = new Configuration()
    flinkConfig.setString(DeploymentOptions.TARGET, executeMode.toString)
    flinkConfig.setString(KubernetesConfigOptions.CLUSTER_ID, clusterId)
    flinkConfig.set(ClientOptions.CLIENT_TIMEOUT, ClientOptions.CLIENT_TIMEOUT.defaultValue())
    flinkConfig.set(
      RestOptions.AWAIT_LEADER_TIMEOUT,
      RestOptions.AWAIT_LEADER_TIMEOUT.defaultValue())
    flinkConfig.set(RestOptions.RETRY_MAX_ATTEMPTS, RestOptions.RETRY_MAX_ATTEMPTS.defaultValue())
    if (Try(namespace.isEmpty).getOrElse(true)) {
      flinkConfig.setString(
        KubernetesConfigOptions.NAMESPACE,
        KubernetesConfigOptions.NAMESPACE.defaultValue())
    } else {
      flinkConfig.setString(KubernetesConfigOptions.NAMESPACE, namespace)
    }
    // retrieve flink cluster client
    val clientFactory: ClusterClientFactory[String] =
      clusterClientServiceLoader.getClusterClientFactory(flinkConfig)

    val clusterProvider: KubernetesClusterDescriptor =
      clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[KubernetesClusterDescriptor]

    Try {
      clusterProvider
        .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
        .getClusterClient
    } match {
      case Success(v) => Some(v)
      case Failure(e) =>
        logError(s"Get flinkClient error, the error is: $e")
        None
    }
  }

  /**
   * check whether deployment exists on kubernetes cluster
   *
   * @param namespace
   *   deployment namespace
   * @param deploymentName
   *   deployment name
   */
  def isDeploymentExists(namespace: String, deploymentName: String): Boolean = {
    using(KubernetesRetriever.newK8sClient()) {
      client =>
        client
          .apps()
          .deployments()
          .inNamespace(namespace)
          .withLabel("type", "flink-native-kubernetes")
          .list()
          .getItems
          .asScala
          .exists(_.getMetadata.getName == deploymentName)
    } {
      e =>
        logError(
          s"""
             |[StreamPark] check deploymentExists error,
             |namespace: $namespace,
             |deploymentName: $deploymentName,
             |error: $e
             |""".stripMargin
        )
        false
    }
  }

  /** retrieve flink jobManager rest url */
  def retrieveFlinkRestUrl(clusterKey: ClusterKey): Option[String] = {
    Utils.using(
      KubernetesRetriever
        .newFinkClusterClient(clusterKey.clusterId, clusterKey.namespace, clusterKey.executeMode)
        .getOrElse(return None)) {
      client =>
        val url =
          IngressController.getIngressUrl(clusterKey.namespace, clusterKey.clusterId, client)
        logger.info(s"retrieve flink jobManager rest url: $url")
        client.close()
        Some(url)
    }
  }

  def getSessionClusterIngressURL(namespace: String, clusterId: String): String = {
    retrieveFlinkRestUrl(ClusterKey(FlinkK8sExecuteMode.SESSION, namespace, clusterId)).orNull
  }

}
