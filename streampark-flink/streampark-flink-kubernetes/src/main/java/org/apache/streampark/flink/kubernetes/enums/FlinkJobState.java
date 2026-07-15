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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/** Flink job status on kubernetes. */
public enum FlinkJobState {

    STARTING,
    K8S_INITIALIZING,
    SILENT,
    POS_TERMINATED,
    TERMINATED,
    LOST,
    OTHER,
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

    public static FlinkJobState of(String value) {
        return Arrays.stream(values())
            .filter(state -> state.name().equals(value))
            .findFirst()
            .orElse(OTHER);
    }

    public static boolean isEndState(FlinkJobState state) {
        return ENDING_STATES.contains(state);
    }
}
