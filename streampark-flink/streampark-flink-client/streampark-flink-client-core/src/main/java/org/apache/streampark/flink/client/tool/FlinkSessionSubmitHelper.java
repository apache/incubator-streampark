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

import org.apache.streampark.shaded.com.fasterxml.jackson.databind.JsonNode;

import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.time.Duration;
import java.util.List;

/** Submit Flink jobs to session clusters via the REST API. */
public final class FlinkSessionSubmitHelper extends LoggerSupport {

    private static final FlinkSessionSubmitHelper INSTANCE = new FlinkSessionSubmitHelper();

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);

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
        HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

        String boundary = "----StreamParkBoundary" + System.currentTimeMillis();
        HttpRequest uploadRequest =
            HttpRequest.newBuilder()
                .uri(URI.create(jmRestUrl + "/jars/upload"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(buildMultipartBody(boundary, flinkJobJar)))
                .build();

        String uploadResult =
            AccessController.doPrivileged(
                (PrivilegedExceptionAction<String>) () -> httpClient
                    .send(
                        uploadRequest,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .body());

        JarUploadResponse jarUploadResponse = parseJarUploadResponse(uploadResult);

        AssertUtils.required(
            jarUploadResponse != null && jarUploadResponse.isSuccessful(),
            "[flink-submit] upload flink jar to flink session cluster failed, jmRestUrl="
                + jmRestUrl
                + ", response="
                + jarUploadResponse);

        HttpRequest runRequest =
            HttpRequest.newBuilder()
                .uri(URI.create(jmRestUrl + "/jars/" + jarUploadResponse.jarId() + "/run"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.write(new JarRunRequest(flinkConfig))))
                .build();

        String resp =
            AccessController.doPrivileged(
                (PrivilegedExceptionAction<String>) () -> httpClient
                    .send(
                        runRequest,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .body());

        String jobId = parseJobId(resp);
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalStateException(
                "[flink-submit] submit flink job via rest api failed, jmRestUrl="
                    + jmRestUrl
                    + ", response="
                    + resp);
        }
        return jobId;
    }

    private byte[] buildMultipartBody(String boundary, File jarFile) throws IOException {
        String header =
            "--"
                + boundary
                + "\r\n"
                + "Content-Disposition: form-data; name=\"jarfile\"; filename=\""
                + jarFile.getName()
                + "\"\r\n"
                + "Content-Type: application/java-archive\r\n\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] fileBytes = Files.readAllBytes(jarFile.toPath());
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);
        return body;
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
            if (node.has("errors") && node.get("errors").size() > 0) {
                return null;
            }
            if (node.has("jobid")) {
                return node.get("jobid").asText(null);
            }
            if (node.has("jobId")) {
                return node.get("jobId").asText(null);
            }
            return null;
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
