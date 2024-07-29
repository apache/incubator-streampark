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

import org.apache.streampark.common.util.Implicits._
import org.apache.streampark.flink.packer.maven.{Artifact, DependencyInfo, MavenTool}

import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.util.jar.JarFile

class MavenToolSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers {

  val outputDir = "MavenToolSpec-output/"
  val preWorkSpaceVal: String = "/streampark"

  override protected def beforeAll(): Unit = {
    val output = new File(outputDir)
    FileUtils.deleteDirectory(output)
    FileUtils.forceMkdir(output)
  }

  override protected def afterAll(): Unit = {
    FileUtils.deleteDirectory(new File(outputDir))
  }

  "MavenTools" when {
    "build fat-jar" should {
      "with jarlibs" in {
        val fatJarPath = outputDir.concat("fat-1.jar")
        val fatJar = MavenTool.buildFatJar(
          null,
          Set(path("jars/commons-cli-1.4.jar"), path("jars/commons-dbutils-1.7.jar")),
          fatJarPath)
        fatJar.exists() mustBe true
        assert(
          jarEquals(
            new JarFile(fatJarPath),
            new JarFile(path("jars/commons-cli-1.4.jar")),
            "org/apache/commons/cli/DefaultParser.class"))
        assert(
          jarEquals(
            new JarFile(fatJarPath),
            new JarFile(path("jars/commons-dbutils-1.7.jar")),
            "org/apache/commons/dbutils/DbUtils.class"))
      }
      "with jarlibs under directory" in {
        val fatJarPath = outputDir.concat("fat-2.jar")
        val fatJar = MavenTool.buildFatJar(null, Set(path("jars/")), fatJarPath)
        fatJar.exists() mustBe true
        assert(
          jarEquals(
            new JarFile(fatJarPath),
            new JarFile(path("jars/commons-cli-1.4.jar")),
            "org/apache/commons/cli/DefaultParser.class"))
        assert(
          jarEquals(
            new JarFile(fatJarPath),
            new JarFile(path("jars/commons-dbutils-1.7.jar")),
            "org/apache/commons/dbutils/DbUtils.class"))
        assert(
          jarEquals(
            new JarFile(fatJarPath),
            new JarFile(path("jars/commons-logging-1.2.jar")),
            "org/apache/commons/logging/Log.class"))
      }
      "with jarlibs and maven artifacts" in {
        val fatJarPath = outputDir.concat("fat-3.jar")
        val fatJar = MavenTool.buildFatJar(
          null,
          DependencyInfo(
            Set(Artifact.of("org.apache.flink:flink-connector-kafka_2.11:1.13.0")),
            Set(path("jars/commons-dbutils-1.7.jar"))),
          fatJarPath)
        fatJar.exists() mustBe true
        assert(
          jarEquals(
            new JarFile(fatJarPath),
            new JarFile(path("jars/commons-dbutils-1.7.jar")),
            "org/apache/commons/dbutils/DbUtils.class"))
        new JarFile(fatJarPath).getJarEntry(
          "org/apache/kafka/clients/ClientUtils.class") mustNot be(null)
        new JarFile(fatJarPath).getJarEntry(
          "org/apache/flink/connector/base/source/reader/SourceReaderBase.class") mustNot be(null)
      }
    }

    "resolve artifacts" should {
      "with single artifact" in {
        val jars = MavenTool.resolveArtifacts(
          Set(Artifact.of("org.apache.flink:flink-connector-kafka_2.11:1.13.0")))
        val expectJars = Array(
          "force-shading-1.13.0.jar",
          "flink-connector-base-1.13.0.jar",
          "kafka-clients-2.4.1.jar")
        jars.size mustNot be(0)
        jars.forall(jar => jar.exists()) mustBe true
        jars.map(jar => jar.getName).sameElements(expectJars) mustBe true
      }
      "with mutiply artifact" in {
        val jars = MavenTool.resolveArtifacts(
          Set(
            Artifact.of("org.apache.flink:flink-connector-kafka_2.11:1.13.0"),
            Artifact.of("org.apache.flink:flink-connector-base:1.13.0")))
        val expectJars = Array(
          "flink-core-1.13.0.jar",
          "force-shading-1.13.0.jar",
          "flink-connector-base-1.13.0.jar",
          "kafka-clients-2.4.1.jar")
        // scalastyle:off println
        jars.foreach(jar => println(jar.getName))
        // scalastyle:on println
        jars.forall(_.exists) mustBe true
        jars.map(_.getName).sameElements(expectJars) mustBe true
        FileUtils.deleteDirectory(new File(outputDir))
      }

    }
  }

}
