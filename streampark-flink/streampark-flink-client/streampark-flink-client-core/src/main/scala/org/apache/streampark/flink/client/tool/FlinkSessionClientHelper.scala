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

package org.apache.streampark.flink.client.tool

import org.apache.streampark.common.util.{AssertUtils, Logger}
import org.apache.streampark.flink.kubernetes.KubernetesRetriever

import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration.{Configuration, CoreOptions}
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization

import java.io.File
import java.nio.charset.StandardCharsets

import scala.util.{Failure, Success, Try}

object FlinkSessionSubmitHelper extends Logger {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  /**
   * Submit Flink Job via Rest API.
   *
   * @param jmRestUrl
   *   jobmanager rest url of target flink cluster
   * @param flinkJobJar
   *   flink job jar file
   * @param flinkConfig
   *   flink configuration
   * @return
   *   jobID of submitted flink job
   */
  @throws[Exception]
  def submitViaRestApi(jmRestUrl: String, flinkJobJar: File, flinkConfig: Configuration): String = {
    // upload flink-job jar
    val uploadResult = Request
      .post(s"$jmRestUrl/jars/upload")
      .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
      .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
      .body(
        MultipartEntityBuilder
          .create()
          .addBinaryBody(
            "jarfile",
            flinkJobJar,
            ContentType.create("application/java-archive"),
            flinkJobJar.getName)
          .build())
      .execute
      .returnContent()
      .asString(StandardCharsets.UTF_8)

    val jarUploadResponse = Try(parse(uploadResult)) match {
      case Success(ok) =>
        JarUploadResponse(
          (ok \ "filename").extract[String],
          (ok \ "status").extract[String])
      case Failure(_) => null
    }

    AssertUtils.required(
      jarUploadResponse.isSuccessful,
      s"[flink-submit] upload flink jar to flink session cluster failed, jmRestUrl=$jmRestUrl, response=$jarUploadResponse")

    // refer to https://ci.apache.org/projects/flink/flink-docs-stable/docs/ops/rest_api/#jars-upload
    val resp = Request
      .post(s"$jmRestUrl/jars/${jarUploadResponse.jarId}/run")
      .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
      .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
      .body(new StringEntity(Serialization.write(new JarRunRequest(flinkConfig))))
      .execute
      .returnContent()
      .asString(StandardCharsets.UTF_8)

    Try(parse(resp)) match {
      case Success(ok) => (ok \ "jobid").extract[String]
      case Failure(_) => null
    }
  }

}

/**
 * refer to https://ci.apache.org/projects/flink/flink-docs-stable/docs/ops/rest_api/#jars-upload
 */
private[client] case class JarUploadResponse(filename: String, status: String) {

  def isSuccessful: Boolean = "success".equalsIgnoreCase(status)

  def jarId: String = filename.substring(filename.lastIndexOf("/") + 1)
}

/**
 * refer to https://ci.apache.org/projects/flink/flink-docs-stable/docs/ops/rest_api/#jars-upload
 */
private[client] case class JarRunRequest(
    entryClass: String,
    programArgs: String,
    parallelism: String,
    savepointPath: String,
    allowNonRestoredState: Boolean) {
  def this(flinkConf: Configuration) {
    this(
      entryClass = flinkConf.get(ApplicationConfiguration.APPLICATION_MAIN_CLASS),
      programArgs = Option(flinkConf.get(ApplicationConfiguration.APPLICATION_ARGS))
        .map(String.join(" ", _))
        .orNull,
      parallelism = String.valueOf(flinkConf.get(CoreOptions.DEFAULT_PARALLELISM)),
      savepointPath = flinkConf.get(SavepointConfigOptions.SAVEPOINT_PATH),
      allowNonRestoredState =
        flinkConf.getBoolean(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE))
  }

}
