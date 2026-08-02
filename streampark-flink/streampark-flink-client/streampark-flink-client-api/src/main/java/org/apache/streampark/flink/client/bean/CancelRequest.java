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

import org.apache.streampark.common.conf.FlinkVersion;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.enums.FlinkDeployMode;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CancelRequest implements SavepointRequestTrait, Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final FlinkVersion flinkVersion;
    private final FlinkDeployMode deployMode;
    @Nullable
    private final Map<String, Serializable> properties;
    private final String clusterId;
    private final String jobId;
    private final boolean withSavepoint;
    private final boolean withDrain;
    private final String savepointPath;
    private final boolean nativeFormat;
    private final String kubernetesNamespace;

    @SuppressWarnings("java:S107")
    public CancelRequest(
                         long id,
                         FlinkVersion flinkVersion,
                         FlinkDeployMode deployMode,
                         @Nullable Map<String, Object> properties,
                         String clusterId,
                         String jobId,
                         boolean withSavepoint,
                         boolean withDrain,
                         String savepointPath,
                         boolean nativeFormat,
                         String kubernetesNamespace) {
        this.id = id;
        this.flinkVersion = flinkVersion;
        this.deployMode = deployMode;
        this.properties = ClientBeanUtils.toSerializableMap(properties);
        this.clusterId = clusterId;
        this.jobId = jobId;
        this.withSavepoint = withSavepoint;
        this.withDrain = withDrain;
        this.savepointPath = savepointPath;
        this.nativeFormat = nativeFormat;
        this.kubernetesNamespace =
            kubernetesNamespace != null ? kubernetesNamespace : Constants.DEFAULT;
    }

    public long id() {
        return id;
    }

    @Override
    public FlinkVersion flinkVersion() {
        return flinkVersion;
    }

    @Override
    public FlinkDeployMode deployMode() {
        return deployMode;
    }

    @Override
    @Nullable
    public Map<String, Object> properties() {
        if (properties == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.putAll(properties);
        return result;
    }

    @Override
    public String clusterId() {
        return clusterId;
    }

    @Override
    public String jobId() {
        return jobId;
    }

    @Override
    public boolean withSavepoint() {
        return withSavepoint;
    }

    public boolean withDrain() {
        return withDrain;
    }

    @Override
    public String savepointPath() {
        return savepointPath;
    }

    @Override
    public boolean nativeFormat() {
        return nativeFormat;
    }

    @Override
    public String kubernetesNamespace() {
        return kubernetesNamespace;
    }
}
