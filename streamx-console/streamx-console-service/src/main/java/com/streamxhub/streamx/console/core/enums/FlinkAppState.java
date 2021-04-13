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
package com.streamxhub.streamx.console.core.enums;

import lombok.Getter;

/**
 * @author benjobs
 */
@Getter
public enum FlinkAppState {

    /**
     * added new job to database
     */
    ADDED(0),
    /**
     * Application which is currently deploying.
     */
    DEPLOYING(1),

    /**
     * Application which is currently deployfailed.
     */
    DEPLOYFAILED(2),
    /**
     * Application which is currently deploying.
     */
    DEPLOYED(3),
    /**
     * The job has been received by the Dispatcher, and is waiting for the job manager to be
     * created.
     */
    INITIALIZING(4),
    /**
     * Job is newly created, no task has started to run.
     */
    CREATED(5),
    /**
     * Application which is currently running.
     */
    STARTING(6),

    /**
     * Application which is currently running.
     */
    RESTARTING(7),
    /**
     * Some tasks are scheduled or running, some may be pending, some may be finished.
     */
    RUNNING(8),

    /**
     * The job has failed and is currently waiting for the cleanup to complete.
     */
    FAILING(9),

    /**
     * The job has failed with a non-recoverable task failure.
     */
    FAILED(10),

    /**
     * Job is being cancelled.
     */
    CANCELLING(11),

    /**
     * Job has been cancelled.
     */
    CANCELED(12),

    /**
     * All of the job's tasks have successfully finished.
     */
    FINISHED(13),
    /**
     * The job has been suspended which means that it has been stopped but not been removed from a
     * potential HA job store.
     */
    SUSPENDED(14),
    /**
     * The job is currently reconciling and waits for task execution report to recover state.
     */
    RECONCILING(15),
    /**
     * 失联
     */
    LOST(16),

    /**
     * 射影中...
     */
    MAPPING(17),

    /**
     * 其他不关心的状态...
     */
    OTHER(18),

    /**
     * yarn 中检查到被killed
     */
    KILLED(-9);

    int value;

    FlinkAppState(int value) {
        this.value = value;
    }

    public static FlinkAppState of(Integer state) {
        for (FlinkAppState appState : values()) {
            if (appState.value == state) {
                return appState;
            }
        }
        return FlinkAppState.OTHER;
    }

    public static FlinkAppState of(String name) {
        for (FlinkAppState appState : values()) {
            if (appState.name().equals(name)) {
                return appState;
            }
        }
        return FlinkAppState.OTHER;
    }
}
