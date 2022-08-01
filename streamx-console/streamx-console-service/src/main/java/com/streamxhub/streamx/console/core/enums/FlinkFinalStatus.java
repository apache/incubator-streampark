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

package com.streamxhub.streamx.console.core.enums;

/**
 * @author xujiangfeng001
 */
public enum FlinkFinalStatus {

    /** Undefined state when either the application has not yet finished */
    UNDEFINED(0),

    /** Application which finished successfully. */
    SUCCEEDED(1),

    /** Application which failed. */
    FAILED(2),

    /** Application which was terminated by a user or admin. */
    KILLED(3),

    /** Application which has subtasks with multiple end states. */
    ENDED(4),

    /** 其他不关心的状态... */
    OTHER(5);

    private final int value;

    FlinkFinalStatus(int value) {
        this.value = value;
    }

    public static FlinkFinalStatus of(Integer status) {
        for (FlinkFinalStatus finalStatus : values()) {
            if (finalStatus.value == status) {
                return finalStatus;
            }
        }
        return FlinkFinalStatus.OTHER;
    }

    public static FlinkFinalStatus of(String name) {
        for (FlinkFinalStatus finalStatus : values()) {
            if (finalStatus.name().equals(name)) {
                return finalStatus;
            }
        }
        return FlinkFinalStatus.OTHER;
    }
}
