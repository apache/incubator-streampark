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

import java.io.Serializable;

/**
 * @author benjobs
 */
@Getter
public enum FlinkAppState implements Serializable {

    /**
     * added new job to database
     */
    ADDED(0),
    /**
     * Application which is currently deploying.
     */
    DEPLOYING(1),
    /**
     * Application which is currently deploying.
     */
    DEPLOYED(2),
    /**
     * The job has been received by the Dispatcher, and is waiting for the job manager to be
     * created.
     */
    INITIALIZING(3),
    /**
     * Job is newly created, no task has started to run.
     */
    CREATED(4),
    /**
     * Application which is currently running.
     */
    STARTING(5),

    /**
     * Application which is currently running.
     */
    RESTARTING(6),
    /**
     * Some tasks are scheduled or running, some may be pending, some may be finished.
     */
    RUNNING(7),

    /**
     * The job has failed and is currently waiting for the cleanup to complete.
     */
    FAILING(8),

    /**
     * The job has failed with a non-recoverable task failure.
     */
    FAILED(9),

    /**
     * Job is being cancelled.
     */
    CANCELLING(10),

    /**
     * Job has been cancelled.
     */
    CANCELED(11),

    /**
     * All of the job's tasks have successfully finished.
     */
    SUCCEEDED(12),
    /**
     * The job has been suspended which means that it has been stopped but not been removed from a
     * potential HA job store.
     */
    SUSPENDED(13),
    /**
     * The job is currently reconciling and waits for task execution report to recover state.
     */
    RECONCILING(14),
    /**
     * 失联
     */
    LOST(15),

    /**
     * 射影中...
     */
    MAPPING(16),

    /**
     * 其他不关心的状态...
     */
    OTHER(17),

    //已回滚
    REVOKED(18),

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
