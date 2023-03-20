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

import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class TrackId {

  private final FlinkK8sExecuteMode executeMode;

  private final String clusterId;

  private final long appId;

  private final String jobId;

  private final String groupId;

  private String namespace = "default";

  public TrackId(
      FlinkK8sExecuteMode executeMode, String clusterId, long appId, String jobId, String groupId) {
    this.executeMode = executeMode;
    this.clusterId = clusterId;
    this.appId = appId;
    this.jobId = jobId;
    this.groupId = groupId;
  }

  public TrackId(
      FlinkK8sExecuteMode executeMode,
      String namespace,
      String clusterId,
      long appId,
      String jobId,
      String groupId) {
    this.executeMode = executeMode;
    this.clusterId = clusterId;
    this.appId = appId;
    this.jobId = jobId;
    this.groupId = groupId;
    this.namespace = namespace;
  }

  public FlinkK8sExecuteMode getExecuteMode() {
    return executeMode;
  }

  public TrackId copy(String jobId) {
      return new TrackId(executeMode, namespace, clusterId, appId, jobId, groupId);
  }

  public ClusterKey toClusterKey() {
      return new ClusterKey(executeMode, namespace, clusterId);
  }

  public String getClusterId() {
    return clusterId;
  }

  public long getAppId() {
    return appId;
  }

  public String getJobId() {
    return jobId;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  @Override
  public int hashCode() {
    return Objects.hash(executeMode, clusterId, appId, jobId, groupId, namespace);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TrackId trackId = (TrackId) o;
    return appId == trackId.appId
        && executeMode == trackId.executeMode
        && Objects.equals(clusterId, trackId.clusterId)
        && Objects.equals(jobId, trackId.jobId)
        && Objects.equals(groupId, trackId.groupId)
        && Objects.equals(namespace, trackId.namespace);
  }

  public boolean isLegal() {
    switch (executeMode) {
      case APPLICATION:
        return StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(clusterId);
      case SESSION:
        return StringUtils.isNotBlank(namespace)
            && StringUtils.isNotBlank(clusterId)
            && StringUtils.isNotBlank(jobId);
      default:
        return false;
    }
  }

  public boolean isActive() {
    return isLegal() && StringUtils.isNotBlank(jobId);
  }
}
