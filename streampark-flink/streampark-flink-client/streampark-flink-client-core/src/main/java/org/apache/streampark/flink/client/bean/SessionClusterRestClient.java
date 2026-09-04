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
import org.apache.streampark.common.util.OkHttpUtils;
import org.apache.streampark.flink.client.configuration.FlinkSavepointOptions;

import org.apache.streampark.shaded.com.fasterxml.jackson.databind.JsonNode;

import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.List;

/** Submits Flink jobs to session clusters through the Flink REST API. */
public final class SessionClusterRestClient {

    private static final MediaType JAR_MEDIA_TYPE =
        MediaType.parse("application/java-archive");
    private static final MediaType JSON_MEDIA_TYPE =
        MediaType.parse("application/json; charset=utf-8");

    private SessionClusterRestClient() {
    }

    /** Uploads a job JAR and starts it on the target session cluster. */
    public static String submit(
                                String jobManagerUrl,
                                File jobJar,
                                Configuration configuration) throws Exception {
        AssertUtils.required(
            jobManagerUrl != null && !jobManagerUrl.isBlank(),
            "Flink JobManager URL must not be empty");
        AssertUtils.required(
            jobJar != null && jobJar.isFile(), "Flink job JAR does not exist: " + jobJar);
        RequestBody uploadBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "jarfile", jobJar.getName(), RequestBody.create(jobJar, JAR_MEDIA_TYPE))
            .build();
        Request uploadRequest = new Request.Builder()
            .url(endpoint(jobManagerUrl, "/jars/upload"))
            .post(uploadBody)
            .build();
        JarUploadResponse upload = parseUploadResponse(execute("upload job JAR", uploadRequest));

        RequestBody runBody = RequestBody.create(
            JsonUtils.write(new JarRunRequest(configuration)), JSON_MEDIA_TYPE);
        Request runRequest = new Request.Builder()
            .url(endpoint(jobManagerUrl, "/jars/" + upload.jarId() + "/run"))
            .post(runBody)
            .build();
        return parseJobId(execute("start uploaded job", runRequest));
    }

    /** Joins a normalized JobManager base URL with a Flink REST resource path. */
    private static String endpoint(String jobManagerUrl, String path) {
        String normalized =
            jobManagerUrl.endsWith("/")
                ? jobManagerUrl.substring(0, jobManagerUrl.length() - 1)
                : jobManagerUrl;
        return normalized + path;
    }

    /** Executes one REST request, consumes its body, and always closes the response. */
    private static String execute(String action, Request request) throws Exception {
        try (
            Response response = AccessController.doPrivileged(
                (PrivilegedExceptionAction<Response>) () -> OkHttpUtils.call(request))) {
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IllegalStateException(
                    "Failed to " + action + ": HTTP " + response.code() + ", response=" + body);
            }
            return body;
        }
    }

    /** Validates the Flink upload response and extracts the server-side JAR identity. */
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

    /** Extracts a job ID while accepting the field casing used by supported Flink versions. */
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
