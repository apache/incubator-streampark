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

import com.streamxhub.streamx.flink.kubernetes.enums.FlinkJobState;
import lombok.Getter;
import scala.Enumeration;

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
    FINISHED(12),

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
     * Lost track of flink job temporarily.
     * A complete loss of flink job tracking translates into LOST state.
     */
    SILENT(19),

    /**
     * Flink job has terminated vaguely, maybe FINISHED, CACNELED or FAILED
     */
    TERMINATED(20),

    /**
     * Flink job has terminated vaguely, maybe FINISHED, CACNELED or FAILED
     */
    POS_TERMINATED(21),

    /**
     * job SUCCEEDED on yarn
     */
    SUCCEEDED(22),
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

    /**
     * type conversion bridging
     */
    public static class Bridge {
        /**
         * covert from com.streamxhub.streamx.flink.k8s.enums.FlinkJobState
         */
        public static FlinkAppState fromK8sFlinkJobState(Enumeration.Value flinkJobState) {
            if (FlinkJobState.K8S_INITIALIZING().equals(flinkJobState)) {
                return INITIALIZING;
            } else {
                return of(flinkJobState.toString());
            }
        }

        /**
         * covert to com.streamxhub.streamx.flink.k8s.enums.FlinkJobState
         */
        public static Enumeration.Value toK8sFlinkJobState(FlinkAppState flinkAppState) {
            return FlinkJobState.of(flinkAppState.name());
        }

    }

}
