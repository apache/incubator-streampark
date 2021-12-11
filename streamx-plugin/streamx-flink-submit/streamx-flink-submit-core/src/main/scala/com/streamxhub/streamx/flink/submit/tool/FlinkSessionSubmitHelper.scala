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

package com.streamxhub.streamx.flink.submit.tool

import com.fasterxml.jackson.annotation.JsonProperty
import com.streamxhub.streamx.common.util.JsonUtils.{Marshal, Unmarshal}
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.kubernetes.KubernetesRetriever
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration.{Configuration, CoreOptions}
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.util.Timeout

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * @author Al-assad
 */
object FlinkSessionSubmitHelper extends Logger {

  /**
   * Submit Flink Job via Rest API.
   *
   * @param jmRestUrl   jobmanager rest url of target flink cluster
   * @param flinkJobJar flink job jar file
   * @param flinkConfig flink configuration
   * @return jobID of submitted flink job
   */
  @throws[Exception] def submitViaRestApi(jmRestUrl: String, flinkJobJar: File, flinkConfig: Configuration): String = {
    // upload flink-job jar
    val uploadResult = Request.post(s"$jmRestUrl/jars/upload")
      .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
      .responseTimeout(Timeout.ofSeconds(60))
      .body(
        MultipartEntityBuilder.create()
          .addBinaryBody("jarfile", flinkJobJar, ContentType.create("application/java-archive"), flinkJobJar.getName)
          .build())
      .execute.returnContent().asString(StandardCharsets.UTF_8)
      .fromJson[JarUploadRsp]

    if (!uploadResult.isSuccessful) {
      throw new Exception(s"[flink-submit] upload flink jar to flink session cluster failed, jmRestUrl=$jmRestUrl, response=$uploadResult")
    }

    // run flink job
    val jarRunRsp = Request.post(s"$jmRestUrl/jars/${uploadResult.jarId}/run")
      .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
      .responseTimeout(Timeout.ofSeconds(60))
      .body(new StringEntity(new JarRunReq(flinkConfig).toJson))
      .execute.returnContent().asString(StandardCharsets.UTF_8)
      .fromJson[JarRunRsp]

    jarRunRsp.jobId
  }

}


/**
 * refer to https://ci.apache.org/projects/flink/flink-docs-stable/docs/ops/rest_api/#jars-upload
 */
private[submit] case class JarUploadRsp(@JsonProperty("filename") filename: String,
                                        @JsonProperty("status") status: String) {

  def isSuccessful: Boolean = "success".equalsIgnoreCase(status)

  def jarId: String = filename.substring(filename.lastIndexOf("/") + 1)
}

/**
 * refer to https://ci.apache.org/projects/flink/flink-docs-stable/docs/ops/rest_api/#jars-upload
 */
private[submit] case class JarRunReq(@JsonProperty("entryClass") entryClass: String,
                                     @JsonProperty("programArgs") programArgs: String,
                                     @JsonProperty("parallelism") parallelism: String,
                                     @JsonProperty("savepointPath") savepointPath: String,
                                     @JsonProperty("allowNonRestoredState") allowNonRestoredState: Boolean) {
  def this(flinkConf: Configuration) {
    this(
      entryClass = flinkConf.get(ApplicationConfiguration.APPLICATION_MAIN_CLASS),
      programArgs = Option(flinkConf.get(ApplicationConfiguration.APPLICATION_ARGS)).map(String.join(" ", _)).orNull,
      parallelism = String.valueOf(flinkConf.get(CoreOptions.DEFAULT_PARALLELISM)),
      savepointPath = flinkConf.get(SavepointConfigOptions.SAVEPOINT_PATH),
      allowNonRestoredState = flinkConf.getBoolean(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE)
    )
  }
}

/**
 * refer to https://ci.apache.org/projects/flink/flink-docs-stable/docs/ops/rest_api/#jars-upload
 */
private[submit] case class JarRunRsp(@JsonProperty("jobid") jobId: String)



