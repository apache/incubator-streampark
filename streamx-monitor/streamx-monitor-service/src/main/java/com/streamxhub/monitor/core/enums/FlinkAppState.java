/**
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
package com.streamxhub.monitor.core.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FlinkAppState {

    /** Job is newly created, no task has started to run. */
    CREATED(0),
    /**
     * Application which is currently deploying.
     */
    DEPLOYING(1),
    /**
     * Application which is currently deploying.
     */
    DEPLOYED(2),

    /**
     * Application which is currently running.
     */
    STARTING(3),

    /**
     * Application which is currently running.
     */
    RESTARTING(4),

    /** Some tasks are scheduled or running, some may be pending, some may be finished. */
    RUNNING(5),

    /** The job has failed and is currently waiting for the cleanup to complete. */
    FAILING(6),

    /** The job has failed with a non-recoverable task failure. */
    FAILED(7),

    /** Job is being cancelled. */
    CANCELLING(8),

    /** Job has been cancelled. */
    CANCELED(9),

    /** All of the job's tasks have successfully finished. */
    FINISHED(10),
    /**
     * The job has been suspended which means that it has been stopped but not been removed from a
     * potential HA job store.
     */
    SUSPENDED(11),

    /** The job is currently reconciling and waits for task execution report to recover state. */
    RECONCILING(12),
    /**
     * 失联
     */
    LOST(13);

    int value;

    FlinkAppState(int value) {
        this.value = value;
    }

    public static FlinkAppState of(Integer state) {
        return Arrays.stream(values()).filter((x) -> x.value == state).findFirst().orElse(null);
    }
}
