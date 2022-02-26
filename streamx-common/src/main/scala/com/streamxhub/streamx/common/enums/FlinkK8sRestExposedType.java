/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.common.enums;

import java.io.Serializable;

/**
 * kubernetes.rest-service.exposed.type
 */
public enum FlinkK8sRestExposedType implements Serializable {

    /**
     * LoadBalancer
     */
    LoadBalancer("LoadBalancer", 0),

    /**
     * ClusterIP
     */
    ClusterIP("ClusterIP", 1),

    /**
     * NodePort
     */
    NodePort("NodePort", 2);

    private final String name;

    private final Integer value;

    FlinkK8sRestExposedType(String name, Integer value) {
        this.name = name;
        this.value = value;
    }

    public static FlinkK8sRestExposedType of(Integer value) {
        for (FlinkK8sRestExposedType order : values()) {
            if (order.value.equals(value)) {
                return order;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public Integer getValue() {
        return value;
    }
}
