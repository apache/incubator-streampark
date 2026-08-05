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

import org.apache.streampark.common.enums.FlinkDeployMode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** execution mode of flink on kubernetes */
public enum FlinkK8sDeployMode {

    SESSION("kubernetes-session"),
    APPLICATION("kubernetes-application");

    private final String value;

    FlinkK8sDeployMode(String value) {
        this.value = value;
    }

    @JsonCreator
    public static FlinkK8sDeployMode of(String value) {
        for (FlinkK8sDeployMode mode : FlinkK8sDeployMode.values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Illegal FlinkK8sDeployMode value: " + value);
    }

    public static FlinkK8sDeployMode of(FlinkDeployMode mode) {
        switch (mode) {
            case KUBERNETES_NATIVE_SESSION:
                return SESSION;
            case KUBERNETES_NATIVE_APPLICATION:
                return APPLICATION;
            default:
                throw new IllegalStateException("Illegal K8sExecuteMode, " + mode.name());
        }
    }

    public static FlinkDeployMode toFlinkDeployMode(FlinkK8sDeployMode mode) {
        switch (mode) {
            case SESSION:
                return FlinkDeployMode.KUBERNETES_NATIVE_SESSION;
            case APPLICATION:
                return FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION;
            default:
                throw new IllegalStateException("Illegal K8sExecuteMode, " + mode);
        }
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
