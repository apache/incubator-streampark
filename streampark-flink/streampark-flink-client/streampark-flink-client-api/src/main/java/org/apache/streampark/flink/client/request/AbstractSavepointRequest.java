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

import org.apache.streampark.common.configuration.Constants;
import org.apache.streampark.common.core.FlinkVersion;
import org.apache.streampark.common.enums.FlinkDeployMode;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;

/**
 * Serializable base request for job operations that can create a savepoint.
 *
 * <p>The request keeps deployment identity, job identity, and configuration together while
 * concrete requests define the savepoint format and target path. Property maps are defensively
 * copied for transfer through the version-isolated client boundary.
 */
public abstract class AbstractSavepointRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final FlinkVersion flinkVersion;
    private final FlinkDeployMode deployMode;
    private final Map<String, Serializable> properties;
    private final JobClientTarget target;

    protected AbstractSavepointRequest(
                                       long id,
                                       FlinkVersion flinkVersion,
                                       FlinkDeployMode deployMode,
                                       @Nullable Map<String, Object> properties,
                                       JobClientTarget target) {
        this.id = id;
        this.flinkVersion = flinkVersion;
        this.deployMode = deployMode;
        this.properties = ClientRequestUtils.toSerializableMap(properties);
        this.target = target;
    }

    public long id() {
        return id;
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

    public String clusterId() {
        return target.clusterId();
    }

    public String jobId() {
        return target.jobId();
    }

    public boolean withSavepoint() {
        return true;
    }

    public abstract String savepointPath();

    public abstract boolean nativeFormat();

    public String kubernetesNamespace() {
        String namespace = target.kubernetesNamespace();
        return namespace == null ? Constants.DEFAULT : namespace;
    }
}
