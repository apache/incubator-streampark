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

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class K8sEventKey {

    private final String namespace;
    private final String clusterId;

    public String namespace() {
        return namespace;
    }

    public String clusterId() {
        return clusterId;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(namespace, clusterId);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof K8sEventKey)) {
            return false;
        }
        K8sEventKey that = (K8sEventKey) obj;
        return namespace.equals(that.namespace) && clusterId.equals(that.clusterId);
    }
}
