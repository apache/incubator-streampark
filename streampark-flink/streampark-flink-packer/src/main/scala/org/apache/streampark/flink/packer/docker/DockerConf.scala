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

import com.github.dockerjava.api.model.AuthConfig

import javax.annotation.Nullable

/**
 * Authentication Configuration of Remote Docker Register
 *
 * @param registerAddress
 *   docker image regoster address. when this configuration item is empty, it means that the
 *   dockerhub public repository is used.
 * @param registerUsername
 *   login username of docker image regoster.
 * @param registerPassword
 *   login password of docker image regoster.
 */
case class DockerConf(
    @Nullable registerAddress: String,
    imageNamespace: String,
    registerUsername: String,
    registerPassword: String) {

  /** covert to com.github.docker.java.api.model.AuthConfig */
  def toAuthConf: AuthConfig = new AuthConfig()
    .withRegistryAddress(registerAddress)
    .withUsername(registerUsername)
    .withPassword(registerPassword)

}

object DockerConf {

  def of(
      @Nullable registerAddress: String,
      imageNameSpace: String,
      registerUsername: String,
      registerPassword: String): DockerConf =
    DockerConf(registerAddress, imageNameSpace, registerUsername, registerPassword)
}
