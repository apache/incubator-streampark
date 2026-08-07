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

package org.apache.streampark.e2e.core.api.flink;

import org.apache.streampark.e2e.core.api.ApiClient;
import org.apache.streampark.e2e.core.api.ApiResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/** REST helpers for Flink env / cluster / application lifecycle in E2E tests. */
public final class FlinkApiSupport {

    private static final Duration DEFAULT_POLL = Duration.ofMinutes(15);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);

    private final ApiClient api;

    public FlinkApiSupport(ApiClient api) {
        this.api = api;
    }

    public ApiClient api() {
        return api;
    }

    public void createFlinkEnv(String flinkName, String flinkHome, String description) {
        ObjectNode body = api.objectNode();
        body.put("flinkName", flinkName);
        body.put("flinkHome", flinkHome);
        body.put("description", description);
        ApiResponse response = api.postJson("/flink/env/create", api.writeJson(body));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Create flink env failed: " + response.text());
        }
    }

    public long requireVersionId(String flinkName) {
        ApiResponse response = api.postForm("/flink/env/list", new LinkedHashMap<>());
        if (!response.isSuccess() || response.getData() == null || !response.getData().isArray()) {
            throw new IllegalStateException("List flink env failed: " + response.text());
        }
        for (JsonNode env : response.getData()) {
            if (flinkName.equals(env.path("flinkName").asText())) {
                return env.path("id").asLong();
            }
        }
        throw new IllegalStateException("Flink env not found: " + flinkName);
    }

    public long createCluster(ObjectNode body) {
        ApiResponse response = api.postJson("/flink/cluster/create", api.writeJson(body));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Create cluster failed: " + response.text());
        }
        String clusterName = body.path("clusterName").asText();
        return requireClusterId(clusterName);
    }

    public long requireClusterId(String clusterName) {
        ApiResponse response = api.postForm("/flink/cluster/list", new LinkedHashMap<>());
        if (!response.isSuccess() || response.getData() == null || !response.getData().isArray()) {
            throw new IllegalStateException("List cluster failed: " + response.text());
        }
        for (JsonNode cluster : response.getData()) {
            if (clusterName.equals(cluster.path("clusterName").asText())) {
                return cluster.path("id").asLong();
            }
        }
        throw new IllegalStateException("Cluster not found: " + clusterName);
    }

    public void updateCluster(ObjectNode body) {
        ApiResponse response = api.postJson("/flink/cluster/update", api.writeJson(body));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Update cluster failed: " + response.text());
        }
    }

    public void startCluster(long clusterId) {
        ApiResponse response = api.postJson("/flink/cluster/start", jsonId(clusterId));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Start cluster failed: " + response.text());
        }
    }

    public void shutdownCluster(long clusterId) {
        ApiResponse response = api.postJson("/flink/cluster/shutdown", jsonId(clusterId));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Shutdown cluster failed: " + response.text());
        }
    }

    public void deleteCluster(long clusterId) {
        ApiResponse response = api.postJson("/flink/cluster/delete", jsonId(clusterId));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Delete cluster failed: " + response.text());
        }
    }

    public void waitClusterState(long clusterId, IntPredicate stateMatcher, Duration timeout) {
        waitUntil(
            () -> getClusterState(clusterId),
            Optional::isPresent,
            cluster -> stateMatcher.test(cluster.get()),
            timeout,
            "cluster " + clusterId);
    }

    public long createApp(ObjectNode body) {
        ApiResponse response = api.postJson("/flink/app/create", api.writeJson(body));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Create app failed: " + response.text());
        }
        return requireAppId(body.path("jobName").asText());
    }

    public long requireAppId(String jobName) {
        ApiResponse response = listApps(jobName);
        Optional<JsonNode> app = api.findInPageRecords(response, "jobName", jobName);
        if (app.isEmpty()) {
            throw new IllegalStateException("App not found: " + jobName);
        }
        return app.get().path("id").asLong();
    }

    public void releaseApp(long appId) {
        ObjectNode body = api.objectNode();
        body.put("appId", appId);
        body.put("forceBuild", false);
        ApiResponse response = api.postJson("/flink/pipe/build", api.writeJson(body));
        if (!response.isSuccess() || !response.getData().asBoolean(false)) {
            throw new IllegalStateException("Release app failed: " + response.text());
        }
        waitUntil(
            () -> getPipelineStatus(appId),
            Optional::isPresent,
            status -> status.get() == FlinkApiConstants.PIPELINE_SUCCESS,
            DEFAULT_POLL,
            "pipeline for app " + appId);
    }

    public void startApp(long appId) {
        ObjectNode body = api.objectNode();
        body.put("id", appId);
        body.put("teamId", api.getTeamId());
        body.put("restoreOrTriggerSavepoint", false);
        body.put("allowNonRestored", false);
        ApiResponse response = api.postJson("/flink/app/start", api.writeJson(body));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Start app failed: " + response.text());
        }
    }

    public void cancelApp(long appId) {
        ObjectNode body = api.objectNode();
        body.put("id", appId);
        body.put("teamId", api.getTeamId());
        body.put("restoreOrTriggerSavepoint", false);
        body.put("drain", false);
        ApiResponse response = api.postJson("/flink/app/cancel", api.writeJson(body));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Cancel app failed: " + response.text());
        }
    }

    public void deleteApp(long appId) {
        ObjectNode body = api.objectNode();
        body.put("id", appId);
        body.put("teamId", api.getTeamId());
        ApiResponse response = api.postJson("/flink/app/delete", api.writeJson(body));
        if (!response.isSuccess()) {
            throw new IllegalStateException("Delete app failed: " + response.text());
        }
    }

    public void waitAppState(long appId, IntPredicate stateMatcher, Duration timeout) {
        waitUntil(
            () -> getAppState(appId),
            Optional::isPresent,
            state -> stateMatcher.test(state.get()),
            timeout,
            "app " + appId);
    }

    public ObjectNode baseSqlAppBody(String jobName, int deployMode, long versionId) {
        ObjectNode body = api.objectNode();
        body.put("teamId", api.getTeamId());
        body.put("jobType", FlinkApiConstants.JOB_TYPE_FLINK_SQL);
        body.put("deployMode", deployMode);
        body.put("versionId", versionId);
        body.put("flinkSql", FlinkApiConstants.TEST_FLINK_SQL);
        body.put("appType", FlinkApiConstants.APP_TYPE_APACHE_FLINK);
        body.put("jobName", jobName);
        body.put("resolveOrder", FlinkApiConstants.RESOLVE_PARENT_FIRST);
        return body;
    }

    public ObjectNode baseClusterBody(String clusterName, int deployMode, long versionId) {
        ObjectNode body = api.objectNode();
        body.put("clusterName", clusterName);
        body.put("deployMode", deployMode);
        body.put("versionId", versionId);
        return body;
    }

    private ApiResponse listApps(String jobName) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("teamId", String.valueOf(api.getTeamId()));
        params.put("pageNum", "1");
        params.put("pageSize", "50");
        params.put("jobName", jobName);
        return api.postForm("/flink/app/list", params);
    }

    private Optional<Integer> getAppState(long appId) {
        ApiResponse response = api.postForm("/flink/app/get", api.params("id", String.valueOf(appId)));
        if (!response.isSuccess() || response.getData() == null) {
            return Optional.empty();
        }
        return Optional.of(response.getData().path("state").asInt(-1));
    }

    private Optional<Integer> getClusterState(long clusterId) {
        ApiResponse response =
            api.postForm("/flink/cluster/get", api.params("id", String.valueOf(clusterId)));
        if (!response.isSuccess() || response.getData() == null) {
            return Optional.empty();
        }
        return Optional.of(response.getData().path("clusterState").asInt(-1));
    }

    private Optional<Integer> getPipelineStatus(long appId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appId", String.valueOf(appId));
        ApiResponse response = api.postForm("/flink/pipe/detail", params);
        if (!response.isSuccess() || response.getData() == null) {
            return Optional.empty();
        }
        JsonNode pipeline = response.getData().path("pipeline");
        if (pipeline.isMissingNode() || pipeline.isNull()) {
            return Optional.empty();
        }
        return Optional.of(pipeline.path("pipeStatus").asInt(-1));
    }

    private ObjectNode jsonId(long id) {
        ObjectNode body = api.objectNode();
        body.put("id", id);
        return body;
    }

    private static <T> void waitUntil(
                                      java.util.function.Supplier<Optional<T>> supplier,
                                      Predicate<Optional<T>> present,
                                      Predicate<T> matcher,
                                      Duration timeout,
                                      String label) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Optional<T> value = supplier.get();
            if (present.test(value) && matcher.test(value.get())) {
                return;
            }
            sleep(POLL_INTERVAL);
        }
        throw new IllegalStateException("Timeout waiting for " + label);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting", e);
        }
    }
}
