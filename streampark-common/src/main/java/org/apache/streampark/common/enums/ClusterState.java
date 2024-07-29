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

/**
 * Cluster state enum.
 *
 * @since 1.2.3
 */
public enum ClusterState {

    /** The cluster was just created but not started */
    CREATED(0),

    /** cluster started */
    RUNNING(1),

    /** cluster stopped */
    CANCELED(2),

    /** cluster lost */
    LOST(3),

    /** cluster unknown */
    UNKNOWN(4),

    /** cluster starting */
    STARTING(5),

    /** cluster cancelling */
    CANCELLING(6),

    /** cluster failed */
    FAILED(7),

    /** cluster killed */
    KILLED(8);

    private final Integer state;

    ClusterState(@Nonnull Integer state) {
        this.state = state;
    }

    /**
     * Try to resolve the value into {@link ClusterState}.
     *
     * @param value the state value of potential cluster state.
     * @return the parsed cluster state enum.
     */
    @Nonnull
    public static ClusterState of(@Nullable Integer value) {
        for (ClusterState clusterState : values()) {
            if (clusterState.state.equals(value)) {
                return clusterState;
            }
        }
        return ClusterState.UNKNOWN;
    }

    /**
     * Try to resolve the name into {@link ClusterState}.
     *
     * @param name The name of potential cluster state.
     * @return The parsed cluster state enum.
     */
    @Nonnull
    public static ClusterState of(@Nullable String name) {
        for (ClusterState clusterState : values()) {
            if (clusterState.name().equals(name)) {
                return clusterState;
            }
        }
        return ClusterState.UNKNOWN;
    }

    /** Get the state value of the current cluster state enum. */
    @Nonnull
    public Integer getState() {
        return state;
    }

    /**
     * Judge the given state enum whether is the running state.
     *
     * @param state The give state enum to judge.
     * @return The result of the judging.
     */
    public static boolean isRunning(@Nullable ClusterState state) {
        return RUNNING.equals(state);
    }
}
