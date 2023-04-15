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

package org.apache.streampark.flink.packer.docker

import org.apache.streampark.common.conf.{CommonConfig, InternalConfigHolder}
import org.apache.streampark.common.util.Utils

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientConfig, HackDockerClient}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient

import java.net.URI
import java.time.Duration

object DockerRetriever {

  /**
   * docker config param from system properties, see in
   * https://github.com/docker-java/docker-java/blob/master/docs/getting_started.md#properties-docker-javaproperties
   * todo support custom docker configuration parameters in unified configurations in the future
   */
  lazy val dockerClientConf: DockerClientConfig = {
    DefaultDockerClientConfig.createDefaultConfigBuilder().build()
  }

  /**
   * docker http client builder, use ApacheDockerHttpClient by default todo support custom http
   * client configuration parameters in unified configurations in the future
   */
  lazy val dockerHttpClientBuilder: ApacheDockerHttpClient.Builder =
    new ApacheDockerHttpClient.Builder()
      .dockerHost(dockerClientConf.getDockerHost)
      .sslConfig(dockerClientConf.getSSLConfig)
      .maxConnections(InternalConfigHolder.get(CommonConfig.DOCKER_MAX_CONNECTIONS))
      .connectionTimeout(
        Duration.ofSeconds(InternalConfigHolder.get(CommonConfig.DOCKER_CONNECTION_TIMEOUT_SEC)))
      .responseTimeout(Duration.ofSeconds(
        InternalConfigHolder.get(CommonConfig.DOCKER_RESPONSE_TIMEOUT_SEC)))

  /** get new DockerClient instance */
  def newDockerClient(): DockerClient = {
    setDockerHost()
    HackDockerClient.getInstance(dockerClientConf, dockerHttpClientBuilder.build())
  }

  /** set docker-host for kata */
  def setDockerHost(): Unit = {
    val dockerhost: String = InternalConfigHolder.get(CommonConfig.DOCKER_HOST)
    if (Utils.notEmpty(dockerhost)) {
      val dockerHostUri: URI = new URI(dockerhost)
      dockerHttpClientBuilder.dockerHost(dockerHostUri)
    }
  }

}
