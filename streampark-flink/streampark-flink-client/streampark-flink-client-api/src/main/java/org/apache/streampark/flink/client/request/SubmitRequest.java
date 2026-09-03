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

package org.apache.streampark.flink.client.request;

import org.apache.streampark.common.core.FlinkVersion;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.enums.FlinkKubernetesRestExposedType;
import org.apache.streampark.common.enums.FlinkRestoreMode;
import org.apache.streampark.flink.packer.pipeline.BuildResult;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;

/** Immutable input for submitting a Flink job. */
public final class SubmitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final FlinkVersion flinkVersion;
    private final FlinkDeployMode deployMode;
    private final Map<String, Serializable> properties;
    private final SubmitApplicationSpec application;
    @Nullable
    private final SubmitClusterSpec cluster;
    @Nullable
    private final BuildResult buildResult;
    private final Map<String, Serializable> extraParameter;

    public SubmitRequest(
                         FlinkVersion flinkVersion,
                         FlinkDeployMode deployMode,
                         Map<String, Object> properties,
                         SubmitApplicationSpec application,
                         @Nullable SubmitClusterSpec cluster,
                         @Nullable BuildResult buildResult,
                         @Nullable Map<String, Object> extraParameter) {
        this.flinkVersion = flinkVersion;
        this.deployMode = deployMode;
        this.properties = ClientRequestUtils.toSerializableMap(properties);
        this.application = application;
        this.cluster = cluster;
        this.buildResult = buildResult;
        this.extraParameter = ClientRequestUtils.toSerializableMap(extraParameter);
    }

    public FlinkVersion flinkVersion() {
        return flinkVersion;
    }

    public FlinkDeployMode deployMode() {
        return deployMode;
    }

    public Map<String, Object> properties() {
        return ClientRequestUtils.copyPropertiesMap(properties);
    }

    public String flinkYaml() {
        return application.flinkYaml();
    }

    /** Returns the YAML syntax captured when the Flink configuration was persisted. */
    public boolean standardYaml() {
        return application.standardYaml();
    }

    public FlinkJobType jobType() {
        return application.jobType();
    }

    public long id() {
        return application.id();
    }

    public String jobId() {
        return application.jobId();
    }

    public String appName() {
        return application.appName();
    }

    public String appConf() {
        return application.appConf();
    }

    public ApplicationType applicationType() {
        return application.applicationType();
    }

    public String savePoint() {
        return application.savePoint();
    }

    public FlinkRestoreMode restoreMode() {
        return application.restoreMode();
    }

    public String args() {
        return application.args();
    }

    @Nullable
    public String clusterId() {
        return cluster == null ? null : cluster.clusterId();
    }

    @Nullable
    public String hadoopUser() {
        return cluster == null ? null : cluster.hadoopUser();
    }

    @Nullable
    public String kubernetesNamespace() {
        return cluster == null ? null : cluster.kubernetesNamespace();
    }

    @Nullable
    public FlinkKubernetesRestExposedType flinkRestExposedType() {
        return cluster == null ? null : cluster.flinkRestExposedType();
    }

    @Nullable
    public BuildResult buildResult() {
        return buildResult;
    }

    public Map<String, Object> extraParameter() {
        return ClientRequestUtils.copyPropertiesMap(extraParameter);
    }
}
