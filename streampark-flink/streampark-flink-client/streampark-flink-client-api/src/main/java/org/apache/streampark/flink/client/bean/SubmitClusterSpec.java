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

import org.apache.streampark.common.enums.FlinkK8sRestExposedType;

import javax.annotation.Nullable;

import java.io.Serializable;

/** Cluster deployment context for Flink submit requests. */
public final class SubmitClusterSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    @Nullable
    private final String clusterId;
    @Nullable
    private final String hadoopUser;
    @Nullable
    private final String kubernetesNamespace;
    @Nullable
    private final FlinkK8sRestExposedType flinkRestExposedType;

    public SubmitClusterSpec(
                             @Nullable String clusterId,
                             @Nullable String hadoopUser,
                             @Nullable String kubernetesNamespace,
                             @Nullable FlinkK8sRestExposedType flinkRestExposedType) {
        this.clusterId = clusterId;
        this.hadoopUser = hadoopUser;
        this.kubernetesNamespace = kubernetesNamespace;
        this.flinkRestExposedType = flinkRestExposedType;
    }

    @Nullable
    public String clusterId() {
        return clusterId;
    }

    @Nullable
    public String hadoopUser() {
        return hadoopUser;
    }

    @Nullable
    public String kubernetesNamespace() {
        return kubernetesNamespace;
    }

    @Nullable
    public FlinkK8sRestExposedType flinkRestExposedType() {
        return flinkRestExposedType;
    }
}
