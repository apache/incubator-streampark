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

package org.apache.streampark.flink.client.bean;

import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.JsonUtils;
import org.apache.streampark.flink.client.configuration.FlinkSavepointOptions;

import org.apache.streampark.shaded.com.fasterxml.jackson.databind.JsonNode;

import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Submits Flink jobs to session clusters through the Flink REST API. */
public final class SessionClusterRestClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);

    private SessionClusterRestClient() {
    }

    /** Uploads a job JAR and starts it on the target session cluster. */
    public static String submit(
                                String jobManagerUrl,
                                File jobJar,
                                Configuration configuration) throws Exception {
        AssertUtils.required(jobJar.isFile(), "Flink job JAR does not exist: " + jobJar);
        HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

        String boundary = "----StreamParkBoundary" + System.currentTimeMillis();
        HttpRequest uploadRequest =
            HttpRequest.newBuilder()
                .uri(endpoint(jobManagerUrl, "/jars/upload"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(multipartBody(boundary, jobJar))
                .build();
        HttpResponse<String> uploadResponse = send(httpClient, uploadRequest);
        requireSuccess("upload job JAR", uploadResponse);
        JarUploadResponse upload = parseUploadResponse(uploadResponse.body());

        HttpRequest runRequest =
            HttpRequest.newBuilder()
                .uri(endpoint(jobManagerUrl, "/jars/" + upload.jarId() + "/run"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        JsonUtils.write(new JarRunRequest(configuration))))
                .build();
        HttpResponse<String> runResponse = send(httpClient, runRequest);
        requireSuccess("start uploaded job", runResponse);
        return parseJobId(runResponse.body());
    }

    private static URI endpoint(String jobManagerUrl, String path) {
        String normalized =
            jobManagerUrl.endsWith("/")
                ? jobManagerUrl.substring(0, jobManagerUrl.length() - 1)
                : jobManagerUrl;
        return URI.create(normalized + path);
    }

    private static HttpResponse<String> send(HttpClient client, HttpRequest request) throws Exception {
        return AccessController.doPrivileged(
            (PrivilegedExceptionAction<HttpResponse<String>>) () -> client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
    }

    private static void requireSuccess(String action, HttpResponse<String> response) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException(
                "Failed to " + action + ": HTTP " + status + ", response=" + response.body());
        }
    }

    private static HttpRequest.BodyPublisher multipartBody(String boundary, File jobJar) {
        byte[] header =
            ("--"
                + boundary
                + "\r\nContent-Disposition: form-data; name=\"jarfile\"; filename=\""
                + jobJar.getName()
                + "\"\r\nContent-Type: application/java-archive\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8);
        byte[] footer =
            ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        return HttpRequest.BodyPublishers.ofInputStream(
            () -> multipartStream(header, jobJar, footer));
    }

    private static InputStream multipartStream(byte[] header, File jobJar, byte[] footer) {
        try {
            List<InputStream> streams =
                Arrays.asList(
                    new ByteArrayInputStream(header),
                    new FileInputStream(jobJar),
                    new ByteArrayInputStream(footer));
            return new SequenceInputStream(Collections.enumeration(streams));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open Flink job JAR: " + jobJar, e);
        }
    }

    private static JarUploadResponse parseUploadResponse(String responseBody) {
        try {
            JsonNode node = JsonUtils.read(responseBody, JsonNode.class);
            JarUploadResponse response =
                new JarUploadResponse(
                    node.has("filename") ? node.get("filename").asText(null) : null,
                    node.has("status") ? node.get("status").asText(null) : null);
            AssertUtils.required(
                response.isSuccessful(),
                "Flink rejected the uploaded job JAR: " + responseBody);
            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Invalid response from Flink JAR upload endpoint: " + responseBody, e);
        }
    }

    private static String parseJobId(String responseBody) {
        try {
            JsonNode node = JsonUtils.read(responseBody, JsonNode.class);
            String jobId = null;
            if (node.has("jobid")) {
                jobId = node.get("jobid").asText(null);
            } else if (node.has("jobId")) {
                jobId = node.get("jobId").asText(null);
            }
            AssertUtils.required(
                jobId != null && !jobId.isBlank(),
                "Flink did not return a job ID: " + responseBody);
            return jobId;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Invalid response from Flink JAR run endpoint: " + responseBody, e);
        }
    }

    private static final class JarUploadResponse {

        private final String filename;
        private final String status;

        private JarUploadResponse(String filename, String status) {
            this.filename = filename;
            this.status = status;
        }

        private boolean isSuccessful() {
            return filename != null
                && !filename.isBlank()
                && "success".equalsIgnoreCase(status);
        }

        private String jarId() {
            return filename.substring(filename.lastIndexOf('/') + 1);
        }
    }

    private static final class JarRunRequest {

        private final String entryClass;
        private final String programArgs;
        private final String parallelism;
        private final String savepointPath;
        private final boolean allowNonRestoredState;

        private JarRunRequest(Configuration configuration) {
            this.entryClass =
                configuration.get(ApplicationConfiguration.APPLICATION_MAIN_CLASS);
            List<String> args =
                configuration.get(ApplicationConfiguration.APPLICATION_ARGS);
            this.programArgs = args == null ? null : String.join(" ", args);
            this.parallelism =
                String.valueOf(configuration.get(CoreOptions.DEFAULT_PARALLELISM));
            this.savepointPath =
                configuration.get(FlinkSavepointOptions.SAVEPOINT_PATH);
            this.allowNonRestoredState =
                configuration.get(
                    FlinkSavepointOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE);
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
}
