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

package org.apache.flink.kubernetes.model;

import org.apache.flink.kubernetes.enums.FlinkK8sExecuteMode;

public class ClusterKey {
  private final FlinkK8sExecuteMode executeMode;

  private final String clusterId;

  private String namespace = "default";

  public ClusterKey(FlinkK8sExecuteMode executeMode, String clusterId, String namespace) {
    this.executeMode = executeMode;
    this.clusterId = clusterId;
    this.namespace = namespace;
  }

  public ClusterKey(FlinkK8sExecuteMode executeMode, String clusterId) {
    this.executeMode = executeMode;
    this.clusterId = clusterId;
  }

  public FlinkK8sExecuteMode getExecuteMode() {
    return executeMode;
  }

  public String getClusterId() {
    return clusterId;
  }

  public String getNamespace() {
    return namespace;
  }
}
