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

import org.apache.streampark.common.configuration.Constants;
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.option.ApplicationOptions;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.flink.client.configuration.FlinkSavepointOptions;
import org.apache.streampark.flink.client.request.SubmitRequest;

import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;

import javax.annotation.Nullable;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable submission values derived from one {@link SubmitRequest} snapshot.
 *
 * <p>All filesystem access and build validation happen in {@link SubmitRequestResolver}. This
 * object contains only immutable values, so every submission stage observes the same artifacts and
 * classpath.
 */
public final class ResolvedSubmitRequest {

    private final SubmitRequest request;
    private final Map<String, String> jobProperties;
    private final Map<String, String> jobOptions;
    @Nullable
    private final String jobMainClass;
    private final String jobName;
    @Nullable
    private final String flinkSqlContent;
    private final boolean nonRestoredStateAllowed;
    private final SavepointRestoreSettings savepointRestoreSettings;
    @Nullable
    private final File userJarFile;
    private final List<URL> submissionClassPath;

    ResolvedSubmitRequest(
                          SubmitRequest request,
                          Map<String, String> jobConfig,
                          String jobName,
                          @Nullable File userJarFile,
                          List<URL> submissionClassPath) {
        this.request = Objects.requireNonNull(request, "request must not be null");
        Map<String, String> config = Map.copyOf(jobConfig);
        this.jobProperties = Collections.unmodifiableMap(
            filterByPrefix(config, FlinkOptions.PROPERTY_PREFIX.value()));

        this.jobOptions = Collections.unmodifiableMap(
            filterByPrefix(config, FlinkOptions.OPTION_PREFIX.value()));

        this.jobMainClass = resolveJobMainClass(config);

        this.jobName = Objects.requireNonNull(jobName, "jobName must not be null");

        Object sql = request.extraParameter().get(ApplicationOptions.SQL.key());
        this.flinkSqlContent = sql == null ? null : sql.toString();

        Object allowNonRestored =
            request.properties().get(FlinkSavepointOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key());

        this.nonRestoredStateAllowed =
            allowNonRestored != null && Boolean.parseBoolean(allowNonRestored.toString());

        this.savepointRestoreSettings =
            request.savePoint() == null || request.savePoint().isEmpty()
                ? SavepointRestoreSettings.none()
                : SavepointRestoreSettings.forPath(
                    request.savePoint(), nonRestoredStateAllowed);
        this.userJarFile = userJarFile;
        this.submissionClassPath = List.copyOf(submissionClassPath);
    }

    /** Returns the original immutable submission request. */
    public SubmitRequest request() {
        return request;
    }

    /** Returns job properties without their transport prefix. */
    public Map<String, String> jobProperties() {
        return jobProperties;
    }

    /** Returns job options without their transport prefix. */
    public Map<String, String> jobOptions() {
        return jobOptions;
    }

    /** Returns the entry class selected for the submitted job type. */
    @Nullable
    public String getJobMainClass() {
        return jobMainClass;
    }

    /** Returns the validated name used throughout this submission. */
    public String getJobName() {
        return jobName;
    }

    /** Returns the SQL payload carried by a Flink SQL request. */
    @Nullable
    public String getFlinkSqlContent() {
        return flinkSqlContent;
    }

    /** Returns whether state not represented in a savepoint may be skipped. */
    public boolean isNonRestoredStateAllowed() {
        return nonRestoredStateAllowed;
    }

    /** Returns restore settings derived from the immutable request snapshot. */
    public SavepointRestoreSettings savepointRestoreSettings() {
        return savepointRestoreSettings;
    }

    /** Returns the classpath captured for JobGraph construction. */
    public List<URL> getSubmissionClassPath() {
        return submissionClassPath;
    }

    /** Returns the built user JAR required by the selected deployment mode. */
    @Nullable
    public File getUserJarFile() {
        return userJarFile;
    }

    /** Resolves the job entry class without reparsing job configuration. */
    @Nullable
    private String resolveJobMainClass(Map<String, String> jobConfig) {
        if (request.jobType() == FlinkJobType.FLINK_SQL) {
            return Constants.STREAMPARK_FLINKSQL_CLIENT_CLASS;
        }
        if (request.jobType() == FlinkJobType.PYFLINK) {
            return Constants.PYTHON_FLINK_DRIVER_CLASS_NAME;
        }
        String mainClass =
            jobProperties.get(FlinkOptions.APPLICATION_MAIN_CLASS.key());
        return mainClass == null
            ? jobConfig.get(FlinkOptions.APPLICATION_MAIN_CLASS.key())
            : mainClass;
    }

    /** Selects non-empty entries under a transport namespace and strips that namespace. */
    private static Map<String, String> filterByPrefix(
                                                      Map<String, String> values,
                                                      String prefix) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue();
            if (entry.getKey().startsWith(prefix) && value != null && !value.isEmpty()) {
                result.put(entry.getKey().substring(prefix.length()), value);
            }
        }
        return result;
    }
}
