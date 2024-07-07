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

import org.apache.streampark.flink.packer.MavenArtifactSpec.illegalArtifactCoordsCases
import org.apache.streampark.flink.packer.maven.Artifact

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

object MavenArtifactSpec {

  private[packer] lazy val illegalArtifactCoordsCases = Array(
    ":::",
    "org.apache.flink:flink-table:",
    ":flink-table:1.13.0",
    "org.apache.flink::1.13.0",
    "org.apache.flink:flink-table:",
    "org.apache.flink:")
}

class MavenArtifactSpec extends AnyWordSpec with Matchers {

  "Artifact" when {
    "created" should {
      "with legal coords" in {
        val art = Artifact.of("org.apache.flink:flink-table:1.13.0")
        art.groupId mustBe "org.apache.flink"
        art.artifactId mustBe "flink-table"
        art.version mustBe "1.13.0"
      }
      "with illegal coords" in {
        illegalArtifactCoordsCases.foreach(coord => {
          assertThrows[IllegalArgumentException] {
            Artifact.of(coord)
          }
        })
      }
    }
  }

}
