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

package org.apache.streampark.flink.kubernetes.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.Set;

/** flink job status on kubernetes */
public enum FlinkJobState {

    /** Flink job has been submitted by StreamPark. */
    STARTING,
    /** Flink Kubernetes resources are being initialized. */
    K8S_INITIALIZING,
    /** Lost track of Flink job temporarily. */
    SILENT,
    /** Flink job terminated positively (FINISHED or CANCELED). */
    POS_TERMINATED,
    /** Flink job terminated (FINISHED, CANCELED, or FAILED). */
    TERMINATED,
    /** Lost track of Flink job completely. */
    LOST,
    /** Other Flink job state. */
    OTHER,

    /** Native Flink state: initializing. */
    INITIALIZING,
    /** Native Flink state: created. */
    CREATED,
    /** Native Flink state: running. */
    RUNNING,
    /** Native Flink state: failing. */
    FAILING,
    /** Native Flink state: failed. */
    FAILED,
    /** Native Flink state: cancelling. */
    CANCELLING,
    /** Native Flink state: canceled. */
    CANCELED,
    /** Native Flink state: finished. */
    FINISHED,
    /** Native Flink state: restarting. */
    RESTARTING;

    private static final Set<FlinkJobState> ENDING_STATES =
        EnumSet.of(FAILED, CANCELED, FINISHED, POS_TERMINATED, TERMINATED, LOST);

    @JsonCreator
    public static FlinkJobState of(String value) {
        if (value == null) {
            return OTHER;
        }
        for (FlinkJobState state : FlinkJobState.values()) {
            if (state.name().equals(value)) {
                return state;
            }
        }
        return OTHER;
    }

    /** whether flink job state is ending state */
    public static boolean isEndState(FlinkJobState state) {
        return state != null && ENDING_STATES.contains(state);
    }

    @JsonValue
    public String getValue() {
        return name();
    }
}
