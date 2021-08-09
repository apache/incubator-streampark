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
package com.streamxhub.streamx.flink.packer

import com.streamxhub.streamx.common.conf.K8sConfigConst._
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File

class DockerToolSpec extends AnyWordSpec with BeforeAndAfter with Matchers {

  val outputDir = new File("DockerToolSpec-output/")

  before {
    outputDir.mkdir()
  }

  after {
    FileUtils.forceDelete(outputDir)
  }

  "DockerTool" when {
    "build flink image without push" should {
      "when remote register is not set" in {
        val template = new FlinkDockerfileTemplate("flink:1.13.0-scala_2.11", path("flink/WordCountSQL.jar"))
        val tag = DockerTool.buildFlinkImage(null, null, null, outputDir.getAbsolutePath, template, "myflink-job")
        tag mustBe s"$IMAGE_NAMESPACE/myflink-job"
      }
      "when remote register is set" in {
        System.setProperty(KEY_K8S_IMAGE_REGISTER_ADDRESS, "registry.cn-hangzhou.aliyuncs.com")
        val template = new FlinkDockerfileTemplate("flink:1.13.0-scala_2.11", path("flink/WordCountSQL.jar"))
        val tag = DockerTool.buildFlinkImage(null, null, null, outputDir.getAbsolutePath, template, "myflink-job")
        tag mustBe s"$K8S_IMAGE_REGISTER_ADDRESS/$IMAGE_NAMESPACE/myflink-job"

      }
    }
    "operate image" should {
      "build image" ignore {}
      "push image" ignore {}
    }

  }

}

