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

import org.apache.streampark.common.constants.Constants;

import java.io.Serializable;

/** Cluster and job identity for client cancel/savepoint requests. */
public final class JobClientTarget implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String clusterId;
    private final String jobId;
    private final String kubernetesNamespace;

    public JobClientTarget(String clusterId, String jobId, String kubernetesNamespace) {
        this.clusterId = clusterId;
        this.jobId = jobId;
        this.kubernetesNamespace =
            kubernetesNamespace != null ? kubernetesNamespace : Constants.DEFAULT;
    }

    public String clusterId() {
        return clusterId;
    }

    public String jobId() {
        return jobId;
    }

    public String kubernetesNamespace() {
        return kubernetesNamespace;
    }
}
