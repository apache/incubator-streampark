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
import org.apache.streampark.common.enums.FlinkDeployMode;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/** Trigger savepoint request. */
public class TriggerSavepointRequest implements SavepointRequestTrait, Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final FlinkVersion flinkVersion;
    private final FlinkDeployMode deployMode;
    @Nullable
    private final Map<String, Serializable> properties;
    private final JobClientTarget target;
    private final SavepointTriggerOptions savepointOptions;

    public TriggerSavepointRequest(
                                   long id,
                                   FlinkVersion flinkVersion,
                                   FlinkDeployMode deployMode,
                                   @Nullable Map<String, Object> properties,
                                   JobClientTarget target,
                                   SavepointTriggerOptions savepointOptions) {
        this.id = id;
        this.flinkVersion = flinkVersion;
        this.deployMode = deployMode;
        this.properties = ClientBeanUtils.toSerializableMap(properties);
        this.target = target;
        this.savepointOptions = savepointOptions;
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
        return target.clusterId();
    }

    @Override
    public String jobId() {
        return target.jobId();
    }

    @Override
    public String savepointPath() {
        return savepointOptions.savepointPath();
    }

    @Override
    public boolean nativeFormat() {
        return savepointOptions.nativeFormat();
    }

    @Override
    public String kubernetesNamespace() {
        return target.kubernetesNamespace();
    }
}
