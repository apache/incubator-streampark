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

import java.io.Serializable;

/**
 * @since: 1.2.3
 */
public enum ClusterState implements Serializable {
    /**
     * The cluster was just created but not started
     */
    CREATED(0),
    /**
     * cluster started
     */
    STARTED(1),
    /**
     * cluster stopped
     */
    STOPED(2),

    /**
     * cluster lost
     */
    LOST(3);

    private final Integer value;

    ClusterState(Integer value) {
        this.value = value;
    }

    public static ClusterState of(Integer value) {
        for (ClusterState clusterState : values()) {
            if (clusterState.value.equals(value)) {
                return clusterState;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }
}
