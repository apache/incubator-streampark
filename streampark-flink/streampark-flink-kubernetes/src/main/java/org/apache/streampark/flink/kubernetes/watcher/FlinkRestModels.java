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

package org.apache.streampark.flink.kubernetes.watcher;

import org.apache.streampark.common.util.JsonUtils;
import org.apache.streampark.flink.kubernetes.enums.FlinkJobState;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;

import org.apache.streampark.shaded.com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Flink REST API response models and parsers. */
final class FlinkRestModels {

    private FlinkRestModels() {
    }

    static Optional<JobDetails> parseJobDetails(String json) {
        try {
            JsonNode root = JsonUtils.read(json, JsonNode.class);
            JsonNode jobsNode = root.get("jobs");
            if (jobsNode == null || jobsNode.isNull() || !jobsNode.isArray()) {
                return Optional.empty();
            }
            List<JobDetail> details = new ArrayList<>();
            for (JsonNode node : jobsNode) {
                details.add(new JobDetail(node));
            }
            return Optional.of(new JobDetails(details.toArray(new JobDetail[0])));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static Optional<FlinkRestOverview> parseOverview(String json) {
        try {
            JsonNode root = JsonUtils.read(json, JsonNode.class);
            return Optional.of(new FlinkRestOverview(root));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static List<FlinkRestJmConfigItem> parseJmConfig(String json) {
        try {
            JsonNode root = JsonUtils.read(json, JsonNode.class);
            if (!root.isArray()) {
                return Collections.emptyList();
            }
            List<FlinkRestJmConfigItem> items = new ArrayList<>();
            for (JsonNode node : root) {
                items.add(new FlinkRestJmConfigItem(text(node, "key"), text(node, "value")));
            }
            return items;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    static Optional<CheckpointResponse> parseCheckpoint(String json) {
        try {
            JsonNode root = JsonUtils.read(json, JsonNode.class);
            JsonNode completed = root.path("latest").path("completed");
            if (completed.isMissingNode() || completed.isNull()) {
                return Optional.empty();
            }
            return Optional.of(new CheckpointResponse(completed));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static int intVal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? 0 : value.asInt();
    }

    private static long longVal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? 0L : value.asLong();
    }

    static final class JobDetails {

        private final JobDetail[] jobs;

        JobDetails(JobDetail[] jobs) {
            this.jobs = jobs;
        }

        JobDetail[] jobs() {
            return jobs;
        }
    }

    static final class JobDetail {

        private final String jid;
        private final String name;
        private final String state;
        private final long startTime;
        private final long endTime;
        private final long duration;
        private final long lastModification;
        private final JobTask tasks;

        JobDetail(JsonNode node) {
            this.jid = text(node, "jid");
            this.name = text(node, "name");
            this.state = text(node, "state");
            this.startTime = longVal(node, "start-time");
            this.endTime = longVal(node, "end-time");
            this.duration = longVal(node, "duration");
            this.lastModification = longVal(node, "last-modification");
            this.tasks = new JobTask(node.get("tasks"));
        }

        JobStatusCV toJobStatusCV(long pollEmitTime, long pollAckTime) {
            return new JobStatusCV(
                FlinkJobState.of(state),
                jid,
                name,
                startTime,
                endTime,
                duration,
                tasks.total,
                pollEmitTime,
                pollAckTime);
        }

        String jid() {
            return jid;
        }
    }

    static final class JobTask {

        private final int total;
        private final int created;
        private final int scheduled;
        private final int deploying;
        private final int running;
        private final int finished;
        private final int canceling;
        private final int canceled;
        private final int failed;
        private final int reconciling;
        private final int initializing;

        JobTask(JsonNode task) {
            this.total = intVal(task, "total");
            this.created = intVal(task, "created");
            this.scheduled = intVal(task, "scheduled");
            this.deploying = intVal(task, "deploying");
            this.running = intVal(task, "running");
            this.finished = intVal(task, "finished");
            this.canceling = intVal(task, "canceling");
            this.canceled = intVal(task, "canceled");
            this.failed = intVal(task, "failed");
            this.reconciling = intVal(task, "reconciling");
            this.initializing = intVal(task, "initializing");
        }
    }

    static final class FlinkRestOverview {

        private final Integer taskManagers;
        private final Integer slotsTotal;
        private final Integer slotsAvailable;
        private final Integer jobsRunning;
        private final Integer jobsFinished;
        private final Integer jobsCancelled;
        private final Integer jobsFailed;
        private final String flinkVersion;

        FlinkRestOverview(JsonNode root) {
            this.taskManagers = intVal(root, "taskmanagers");
            this.slotsTotal = intVal(root, "slots-total");
            this.slotsAvailable = intVal(root, "slots-available");
            this.jobsRunning = intVal(root, "jobs-running");
            this.jobsFinished = intVal(root, "jobs-finished");
            this.jobsCancelled = intVal(root, "jobs-cancelled");
            this.jobsFailed = intVal(root, "jobs-failed");
            this.flinkVersion = text(root, "flink-version");
        }

        Integer taskManagers() {
            return taskManagers;
        }
        Integer slotsTotal() {
            return slotsTotal;
        }
        Integer slotsAvailable() {
            return slotsAvailable;
        }
        Integer jobsRunning() {
            return jobsRunning;
        }
        Integer jobsFinished() {
            return jobsFinished;
        }
        Integer jobsCancelled() {
            return jobsCancelled;
        }
        Integer jobsFailed() {
            return jobsFailed;
        }
    }

    static final class FlinkRestJmConfigItem {

        private final String key;
        private final String value;

        FlinkRestJmConfigItem(String key, String value) {
            this.key = key;
            this.value = value;
        }

        String key() {
            return key;
        }

        String value() {
            return value;
        }
    }

    static final class CheckpointResponse {

        private final long id;
        private final String status;
        private final String externalPath;
        private final boolean isSavepoint;
        private final String checkpointType;
        private final long triggerTimestamp;

        CheckpointResponse(JsonNode completed) {
            this.id = longVal(completed, "id");
            this.status = text(completed, "status");
            this.externalPath = text(completed, "external_path");
            this.isSavepoint = completed.path("is_savepoint").asBoolean(false);
            this.checkpointType = text(completed, "checkpoint_type");
            this.triggerTimestamp = longVal(completed, "trigger_timestamp");
        }

        long id() {
            return id;
        }
        String status() {
            return status;
        }
        String externalPath() {
            return externalPath;
        }
        boolean isSavepoint() {
            return isSavepoint;
        }
        String checkpointType() {
            return checkpointType;
        }
        long triggerTimestamp() {
            return triggerTimestamp;
        }
    }
}
