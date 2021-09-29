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

import com.streamxhub.streamx.flink.packer.docker.FlinkDockerfileTemplate
import com.streamxhub.streamx.flink.packer.docker.FlinkDockerfileTemplate.DEFAULT_DOCKER_FILE_NAME
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File

class FlinkDockerfileTemplateSpec extends AnyWordSpec with BeforeAndAfter with Matchers {

  val outputDir = new File("FlinkDockerfileTemplateSpec-output/")

  val assertDockerFileContent: String =
    """FROM 1.13-scala_2.11
      |RUN mkdir -p $FLINK_HOME/usrlib
      |COPY /WordCountSQL.jar $FLINK_HOME/usrlib/WordCountSQL.jar
      |""".stripMargin

  before {
    outputDir.mkdir()
  }
  after {
    FileUtils.forceDelete(outputDir)
  }

  "FlinkDockerfileTemplate" when {

    "create dockerfile" should {
      val template = new FlinkDockerfileTemplate("1.13-scala_2.11", path("flink/WordCountSQL.jar"))
      val assertDockerFileContent =
        """FROM 1.13-scala_2.11
          |RUN mkdir -p $FLINK_HOME/usrlib
          |COPY /WordCountSQL.jar $FLINK_HOME/usrlib/WordCountSQL.jar
          |""".stripMargin

      "build Dockerfile content" in {
        template.dockerfileContent mustBe assertDockerFileContent
      }
      "write Dockerfile to file" in {
        val output = outputDir.getAbsolutePath + "/my-dockerfile"
        val outFile = template.writeDockerfile(output)
        FileUtils.readFileToString(outFile, "UTF-8") mustBe assertDockerFileContent
      }
      "write Dockerfile to directory" in {
        val outFile = template.writeDockerfile(outputDir.getAbsolutePath)
        outFile.getName mustBe DEFAULT_DOCKER_FILE_NAME
        FileUtils.readFileToString(outFile, "UTF-8") mustBe assertDockerFileContent
      }
    }

  }

}
