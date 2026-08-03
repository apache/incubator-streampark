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
@SuppressWarnings("AlibabaEnumConstantsMustHaveComment")
public enum FlinkJobState {

    // flink job has been submit by the streampark.
    STARTING,
    // flink k8s resources are being initialized.
    K8S_INITIALIZING,
    // lost track of flink job temporarily.
    SILENT,
    // flink job has terminated positively (maybe FINISHED or CANCELED)
    POS_TERMINATED,
    // flink job has terminated (maybe FINISHED, CANCELED or FAILED)
    TERMINATED,
    // lost track of flink job completely.
    LOST,
    // other flink state
    OTHER,

    // the following enum have the same meaning as the native flink state enum.
    // @see org.apache.flink.api.common.JobStatus
    INITIALIZING,
    CREATED,
    RUNNING,
    FAILING,
    FAILED,
    CANCELLING,
    CANCELED,
    FINISHED,
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
