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
package com.streamxhub.streamx.flink.packer

import com.streamxhub.streamx.flink.packer.MavenArtifactSpec.illegalArtifactCoordsCases
import com.streamxhub.streamx.flink.packer.maven.MavenArtifact
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

object MavenArtifactSpec {

  lazy private[packer] val illegalArtifactCoordsCases = Array(
    ":::",
    "org.apache.flink:flink-table:",
    ":flink-table:1.13.0",
    "org.apache.flink::1.13.0",
    "org.apache.flink:flink-table:",
    "org.apache.flink:"
  )
}

class MavenArtifactSpec extends AnyWordSpec with Matchers {

  "MavenArtifact" when {
    "created" should {
      "with legal coords" in {
        val art = MavenArtifact.of("org.apache.flink:flink-table:1.13.0")
        art.groupId mustBe "org.apache.flink"
        art.artifactId mustBe "flink-table"
        art.version mustBe "1.13.0"
      }
      "with illegal coords" in {
        illegalArtifactCoordsCases.foreach(coord => {
          assertThrows[IllegalArgumentException] {
            MavenArtifact.of(coord)
          }
        })
      }
    }
  }

}
