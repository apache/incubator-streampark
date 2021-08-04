/*
 * Copyright (c) 2021 The StreamX Project
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
package com.streamxhub.streamx.codebuild

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.streamxhub.streamx.common.conf.K8sConfigConst

import java.time.Duration

/**
 * author: Al-assad
 */
object DockerRetriver {

  /**
   * docker config param from system properties,
   * see in https://github.com/docker-java/docker-java/blob/master/docs/getting_started.md#properties-docker-javaproperties
   * todo support custom docker configuration parameters in unifined configurations in the future
   */
  lazy val dockerClientConf: DockerClientConfig = {
    DefaultDockerClientConfig.createDefaultConfigBuilder().build()
  }

  /**
   * docker http client, use ApacheDockerHttpClient by default
   * todo support custom http client configuration parameters in unifined configurations in the future
   */
  lazy val dockerHttpClient: DockerHttpClient = {
    new ApacheDockerHttpClient.Builder()
      .dockerHost(dockerClientConf.getDockerHost)
      .sslConfig(dockerClientConf.getSSLConfig)
      .maxConnections(100)
      .connectionTimeout(Duration.ofSeconds(30))
      .responseTimeout(Duration.ofSeconds(45))
      .build
  }

  /**
   * remote image regoster authentication configuration which used by K8s cluster
   */
  lazy val remoteImageRegisterAuthConfig: AuthConfig = {
    new AuthConfig()
      .withRegistryAddress(K8sConfigConst.K8S_IMAGE_REGISTER_ADDRESS)
      .withUsername(K8sConfigConst.K8S_IMAGE_REGISTER_USERNAME)
      .withPassword(K8sConfigConst.K8S_IMAGE_REGISTER_PASSWORD)
  }

  /**
   * get new DockerClient instance
   */
  def newDockerClient(): DockerClient = DockerClientImpl.getInstance(dockerClientConf, dockerHttpClient)


}
