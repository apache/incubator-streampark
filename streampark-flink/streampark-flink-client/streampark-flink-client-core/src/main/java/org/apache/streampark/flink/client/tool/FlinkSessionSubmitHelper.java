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

package org.apache.streampark.flink.client.tool;

import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.JsonUtils;
import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.flink.client.conf.FlinkSavepointOptions;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;

import org.apache.streampark.shaded.com.fasterxml.jackson.databind.JsonNode;

import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Submit Flink jobs to session clusters via the REST API. */
public final class FlinkSessionSubmitHelper extends LoggerSupport {

    private static final FlinkSessionSubmitHelper INSTANCE = new FlinkSessionSubmitHelper();

    private FlinkSessionSubmitHelper() {
    }

    /**
     * Submit Flink Job via Rest API.
     *
     * @param jmRestUrl jobmanager rest url of target flink cluster
     * @param flinkJobJar flink job jar file
     * @param flinkConfig flink configuration
     * @return jobID of submitted flink job
     */
    public static String submitViaRestApi(
                                          String jmRestUrl, File flinkJobJar,
                                          Configuration flinkConfig) throws Exception {
        return INSTANCE.doSubmitViaRestApi(jmRestUrl, flinkJobJar, flinkConfig);
    }

    private String doSubmitViaRestApi(String jmRestUrl, File flinkJobJar, Configuration flinkConfig) throws Exception {
        String uploadResult =
            Request.post(jmRestUrl + "/jars/upload")
                .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
                .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
                .body(
                    MultipartEntityBuilder.create()
                        .addBinaryBody(
                            "jarfile",
                            flinkJobJar,
                            ContentType.create("application/java-archive"),
                            flinkJobJar.getName())
                        .build())
                .execute()
                .returnContent()
                .asString(StandardCharsets.UTF_8);

        JarUploadResponse jarUploadResponse = parseJarUploadResponse(uploadResult);

        AssertUtils.required(
            jarUploadResponse != null && jarUploadResponse.isSuccessful(),
            "[flink-submit] upload flink jar to flink session cluster failed, jmRestUrl="
                + jmRestUrl
                + ", response="
                + jarUploadResponse);

        String resp =
            Request.post(jmRestUrl + "/jars/" + jarUploadResponse.jarId() + "/run")
                .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
                .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
                .body(new StringEntity(JsonUtils.write(new JarRunRequest(flinkConfig))))
                .execute()
                .returnContent()
                .asString(StandardCharsets.UTF_8);

        return parseJobId(resp);
    }

    private JarUploadResponse parseJarUploadResponse(String uploadResult) {
        try {
            JsonNode node = JsonUtils.read(uploadResult, JsonNode.class);
            return new JarUploadResponse(
                node.has("filename") ? node.get("filename").asText(null) : null,
                node.has("status") ? node.get("status").asText(null) : null);
        } catch (Exception e) {
            return null;
        }
    }

    private String parseJobId(String resp) {
        try {
            JsonNode node = JsonUtils.read(resp, JsonNode.class);
            return node.has("jobid") ? node.get("jobid").asText(null) : null;
        } catch (Exception e) {
            return null;
        }
    }
}

/**
 * refer to https://ci.apache.org/projects/flink/flink-docs-stable/docs/ops/rest_api/#jars-upload
 */
class JarUploadResponse {

    private final String filename;
    private final String status;

    JarUploadResponse(String filename, String status) {
        this.filename = filename;
        this.status = status;
    }

    boolean isSuccessful() {
        return "success".equalsIgnoreCase(status);
    }

    String jarId() {
        return filename.substring(filename.lastIndexOf('/') + 1);
    }

    @Override
    public String toString() {
        return "JarUploadResponse{filename='" + filename + "', status='" + status + "'}";
    }
}

/**
 * refer to https://ci.apache.org/projects/flink/flink-docs-stable/docs/ops/rest_api/#jars-upload
 */
class JarRunRequest {

    private final String entryClass;
    private final String programArgs;
    private final String parallelism;
    private final String savepointPath;
    private final boolean allowNonRestoredState;

    JarRunRequest(Configuration flinkConf) {
        this.entryClass = flinkConf.get(ApplicationConfiguration.APPLICATION_MAIN_CLASS);
        List<String> args = flinkConf.get(ApplicationConfiguration.APPLICATION_ARGS);
        this.programArgs = args == null ? null : String.join(" ", args);
        this.parallelism = String.valueOf(flinkConf.get(CoreOptions.DEFAULT_PARALLELISM));
        this.savepointPath = flinkConf.get(FlinkSavepointOptions.SAVEPOINT_PATH);
        this.allowNonRestoredState =
            flinkConf.get(FlinkSavepointOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE);
    }

    public String getEntryClass() {
        return entryClass;
    }

    public String getProgramArgs() {
        return programArgs;
    }

    public String getParallelism() {
        return parallelism;
    }

    public String getSavepointPath() {
        return savepointPath;
    }

    public boolean isAllowNonRestoredState() {
        return allowNonRestoredState;
    }
}
