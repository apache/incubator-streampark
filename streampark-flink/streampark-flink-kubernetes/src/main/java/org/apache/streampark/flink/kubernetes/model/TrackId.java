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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Properties;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class TrackId {

    private FlinkK8sDeployMode executeMode;
    private String namespace = "default";
    private String clusterId;
    private Long appId = null;
    private String jobId;
    private String groupId;
    private Properties properties;

    public boolean isLegal() {
        switch (executeMode) {
            case APPLICATION:
                return namespace != null && !namespace.isEmpty()
                    && clusterId != null && !clusterId.isEmpty();
            case SESSION:
                return namespace != null && !namespace.isEmpty()
                    && clusterId != null && !clusterId.isEmpty()
                    && jobId != null && !jobId.isEmpty();
            default:
                return false;
        }
    }

    public boolean isActive() {
        return isLegal() && jobId != null && !jobId.isEmpty();
    }

    public ClusterKey toClusterKey() {
        return ClusterKey.of(this);
    }

    public TrackId copy() {
        return new TrackId(
            executeMode, namespace, clusterId, appId, jobId, groupId, properties);
    }

    public TrackId jobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    public TrackId appId(Long appId) {
        this.appId = appId;
        return this;
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
            && clusterId.equals(that.clusterId)
            && namespace.equals(that.namespace)
            && java.util.Objects.equals(appId, that.appId)
            && java.util.Objects.equals(jobId, that.jobId)
            && groupId.equals(that.groupId)
            && properties.equals(that.properties);
    }

    public static TrackId onSession(
                                    String namespace,
                                    String clusterId,
                                    Long appId,
                                    String jobId,
                                    String groupId,
                                    Properties properties) {
        return new TrackId(
            FlinkK8sDeployMode.SESSION, namespace, clusterId, appId, jobId, groupId, properties);
    }

    public static TrackId onApplication(
                                        String namespace,
                                        String clusterId,
                                        Long appId,
                                        String jobId,
                                        String groupId,
                                        Properties properties) {
        return new TrackId(
            FlinkK8sDeployMode.APPLICATION, namespace, clusterId, appId, jobId, groupId, properties);
    }
}
