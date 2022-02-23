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

package com.streamxhub.streamx.flink.packer.docker

import com.github.dockerjava.api.model.AuthConfig

import javax.annotation.Nullable

/**
 * Authentication Configuration of Remote Docker Register
 *
 * @author Al-assad
 * @param registerAddress  docker image regoster address. when this configuration item
 *                         is empty, it means that the dockerhub public repository is used.
 * @param registerUsername login username of docker image regoster.
 * @param registerPassword login password of docker image regoster.
 */
case class DockerAuthConf(@Nullable registerAddress: String, registerUsername: String, registerPassword: String) {

  /**
   * covert to com.github.docker.java.api.model.AuthConfig
   */
  def toDockerAuthConf: AuthConfig = new AuthConfig()
    .withRegistryAddress(registerAddress)
    .withUsername(registerUsername)
    .withPassword(registerPassword)

}

object DockerAuthConf {
  /**
   * use dockerhub as remote image register
   */
  def withDockerHubRegister(registerUsername: String, registerPassword: String): DockerAuthConf =
    DockerAuthConf("", registerUsername, registerPassword)

  def of(@Nullable registerAddress: String, registerUsername: String, registerPassword: String): DockerAuthConf =
    DockerAuthConf(registerAddress, registerUsername, registerPassword)
}

