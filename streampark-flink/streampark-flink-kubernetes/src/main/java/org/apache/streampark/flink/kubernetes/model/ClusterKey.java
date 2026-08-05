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

package org.apache.streampark.flink.kubernetes.model;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sDeployMode;

import lombok.AllArgsConstructor;
import lombok.Builder;

/** flink cluster identifier on kubernetes */
@Builder
@AllArgsConstructor
public class ClusterKey {

    private final FlinkK8sDeployMode executeMode;
    @Builder.Default
    private final String namespace = "default";
    private final String clusterId;

    public FlinkK8sDeployMode executeMode() {
        return executeMode;
    }

    public String namespace() {
        return namespace;
    }

    public String clusterId() {
        return clusterId;
    }

    public static ClusterKey of(TrackId trackId) {
        return ClusterKey.builder()
            .executeMode(trackId.executeMode())
            .namespace(trackId.namespace())
            .clusterId(trackId.clusterId())
            .build();
    }

    @Override
    public String toString() {
        return executeMode.toString() + namespace + clusterId;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(executeMode, namespace, clusterId);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClusterKey)) {
            return false;
        }
        ClusterKey that = (ClusterKey) obj;
        return executeMode == that.executeMode
            && namespace.equals(that.namespace)
            && clusterId.equals(that.clusterId);
    }
}
