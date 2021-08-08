/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.common.enums;


import java.io.Serializable;

/**
 * @author benjobs
 */

public enum ExecutionMode implements Serializable {

    /**
     * Local mode
     */
    LOCAL(0, "local"),
    /**
     * remote
     */
    REMOTE(1, "remote"),
    /**
     * yarn-pre-job mode
     */
    YARN_PRE_JOB(2, "yarn-pre-job"),
    /**
     * yarn session
     */
    YARN_SESSION(3, "yarn-session"),
    /**
     * yarn application
     */
    YARN_APPLICATION(4, "yarn-application"),
    /**
     * kubernetes session
     */
    KUBERNETES_NATIVE_SESSION(5, "kubernetes-session"),
    /**
     * kubernetes application
     */
    KUBERNETES_NATIVE_APPLICATION(6, "kubernetes-application");

    private Integer mode;
    private String name;

    ExecutionMode(Integer mode, String name) {
        this.mode = mode;
        this.name = name;
    }

    public static ExecutionMode of(Integer value) {
        for (ExecutionMode executionMode : values()) {
            if (executionMode.mode.equals(value)) {
                return executionMode;
            }
        }
        return null;
    }

    public static ExecutionMode of(String name) {
        for (ExecutionMode executionMode : values()) {
            if (executionMode.name.equals(name)) {
                return executionMode;
            }
        }
        return null;
    }

    public int getMode() {
        return mode;
    }

    public String getName() {
        return name;
    }
}
