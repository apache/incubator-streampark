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

package org.apache.flink.kubernetes.enums;

import org.apache.streampark.common.enums.ExecutionMode;

public enum FlinkK8sExecuteMode {

    SESSION("kubernetes-session"),
    APPLICATION("kubernetes-application");

    private final String value;

    FlinkK8sExecuteMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FlinkK8sExecuteMode of(ExecutionMode mode) {
        switch (mode) {
            case KUBERNETES_NATIVE_SESSION:
                return SESSION;
            case KUBERNETES_NATIVE_APPLICATION:
                return APPLICATION;
            default:
                throw new IllegalStateException("Illegal ExecutionMode " + mode.getName());
        }
    }

    public static ExecutionMode toExecutionMode(FlinkK8sExecuteMode mode) {
        switch (mode) {
            case SESSION:
                return ExecutionMode.KUBERNETES_NATIVE_SESSION;
            case APPLICATION:
                return ExecutionMode.KUBERNETES_NATIVE_APPLICATION;
            default:
                // never got in there
                throw new IllegalStateException("Illegal K8sExecuteMode " + mode);
        }
    }
}
