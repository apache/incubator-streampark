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

import java.util.Properties;

/** tracking identifier for flink on kubernetes */
@Builder(toBuilder = true)
@AllArgsConstructor
public class TrackId {

    private final FlinkK8sDeployMode executeMode;
    @Builder.Default
    private final String namespace = "default";
    private final String clusterId;
    private final Long appId;
    private final String jobId;
    private final String groupId;
    private final Properties properties;

    public FlinkK8sDeployMode executeMode() {
        return executeMode;
    }

    public String namespace() {
        return namespace;
    }

    public String clusterId() {
        return clusterId;
    }

    public Long appId() {
        return appId;
    }

    public String jobId() {
        return jobId;
    }

    public String groupId() {
        return groupId;
    }

    public Properties properties() {
        return properties;
    }

    public boolean isLegal() {
        switch (executeMode) {
            case APPLICATION:
                return isNotEmpty(namespace) && isNotEmpty(clusterId);
            case SESSION:
                return isNotEmpty(namespace) && isNotEmpty(clusterId) && isNotEmpty(jobId);
            default:
                return false;
        }
    }

    public boolean isActive() {
        return isLegal() && isNotEmpty(jobId);
    }

    /** covert to ClusterKey */
    public ClusterKey toClusterKey() {
        return ClusterKey.builder()
            .executeMode(executeMode)
            .namespace(namespace)
            .clusterId(clusterId)
            .build();
    }

    public static TrackId onSession(
                                    String namespace,
                                    String clusterId,
                                    Long appId,
                                    String jobId,
                                    String groupId,
                                    Properties properties) {
        return TrackId.builder()
            .executeMode(FlinkK8sDeployMode.SESSION)
            .namespace(namespace)
            .clusterId(clusterId)
            .appId(appId)
            .jobId(jobId)
            .groupId(groupId)
            .properties(properties)
            .build();
    }

    public static TrackId onApplication(
                                        String namespace,
                                        String clusterId,
                                        Long appId,
                                        String jobId,
                                        String groupId,
                                        Properties properties) {
        return TrackId.builder()
            .executeMode(FlinkK8sDeployMode.APPLICATION)
            .namespace(namespace)
            .clusterId(clusterId)
            .appId(appId)
            .jobId(jobId)
            .groupId(groupId)
            .properties(properties)
            .build();
    }

    private static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(executeMode, clusterId, namespace, appId, jobId, groupId, properties);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TrackId)) {
            return false;
        }
        TrackId that = (TrackId) obj;
        return executeMode == that.executeMode
            && java.util.Objects.equals(clusterId, that.clusterId)
            && java.util.Objects.equals(namespace, that.namespace)
            && java.util.Objects.equals(appId, that.appId)
            && java.util.Objects.equals(jobId, that.jobId)
            && java.util.Objects.equals(groupId, that.groupId)
            && java.util.Objects.equals(properties, that.properties);
    }
}
