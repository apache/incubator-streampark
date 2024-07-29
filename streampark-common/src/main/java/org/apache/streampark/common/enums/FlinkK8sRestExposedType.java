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
package org.apache.streampark.common.enums;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** kubernetes.rest-service.exposed.type */
public enum FlinkK8sRestExposedType {

    /** LoadBalancer */
    LOAD_BALANCER("LoadBalancer", 0),

    /** ClusterIP */
    CLUSTER_IP("ClusterIP", 1),

    /** NodePort */
    NODE_PORT("NodePort", 2);

    private final String name;

    private final Integer type;

    FlinkK8sRestExposedType(@Nonnull String name, @Nonnull Integer type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Try to resolve the given Flink K8s Rest-Exposed type value into a known {@link
     * FlinkK8sRestExposedType} enum.
     */
    @Nullable
    public static FlinkK8sRestExposedType of(@Nullable Integer value) {
        for (FlinkK8sRestExposedType order : values()) {
            if (order.type.equals(value)) {
                return order;
            }
        }
        return null;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public Integer getType() {
        return type;
    }
}
