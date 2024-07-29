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
package org.apache.streampark.flink.packer

import org.apache.streampark.common.conf.{CommonConfig, InternalConfigHolder}
import org.apache.streampark.flink.packer.docker.DockerImageExist
import org.apache.streampark.flink.packer.docker.DockerRetriever.dockerClientConf

import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Duration

class DockerClientTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {

  "Docker Client Config" when {
    val dockerHttpClientBuilder: ApacheDockerHttpClient.Builder =
      new ApacheDockerHttpClient.Builder()
        .dockerHost(dockerClientConf.getDockerHost)
        .sslConfig(dockerClientConf.getSSLConfig)
        .maxConnections(InternalConfigHolder.get(CommonConfig.DOCKER_MAX_CONNECTIONS))
        .connectionTimeout(
          Duration.ofSeconds(InternalConfigHolder.get(CommonConfig.DOCKER_CONNECTION_TIMEOUT_SEC)))
        .responseTimeout(Duration.ofSeconds(
          InternalConfigHolder.get(CommonConfig.DOCKER_RESPONSE_TIMEOUT_SEC)))
  }

  "Docker Image Exist" should {
    "return true if the image exists" in {
      val dockerImageExist = new DockerImageExist()
      val imageName = "flink:1.18.1-scala_2.12-java8"
      val result = dockerImageExist.doesDockerImageExist(imageName)
      assert(result)
    }

    "return false if the image does not exist" in {
      val dockerImageExist = new DockerImageExist()
      val imageName = "flink:1.18.1-scala_2.12-java8-fail"
      val result = dockerImageExist.doesDockerImageExist(imageName)
      assert(!result)
    }
  }

}
