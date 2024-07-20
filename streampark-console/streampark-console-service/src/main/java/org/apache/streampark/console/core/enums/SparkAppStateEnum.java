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

package org.apache.streampark.console.core.enums;

import lombok.Getter;

/** Describe the status of Spark Application */
@Getter
public enum SparkAppStateEnum {

    /** Added new job to database. */
    ADDED(0),

    /** (From Yarn)Application which was just created. */
    NEW(1),

    /** (From Yarn)Application which is being saved. */
    NEW_SAVING(2),

    /** Application which is currently running. */
    STARTING(3),

    /** (From Yarn)Application which has been submitted. */
    SUBMITTED(4),

    /** (From Yarn)Application has been accepted by the scheduler. */
    ACCEPTED(5),

    /** The job has failed and is currently waiting for the cleanup to complete. */
    RUNNING(6),

    /** (From Yarn)Application which finished successfully. */
    FINISHED(7),

    /** (From Yarn)Application which failed. */
    FAILED(8),

    /** Loss of mapping. */
    LOST(9),

    /** Mapping. */
    MAPPING(10),

    /** Other statuses. */
    OTHER(11),

    /** Has rollback. */
    REVOKED(12),

    /** Spark job has being cancelling(killing) by streampark */
    STOPPING(13),

    /** Job SUCCEEDED on yarn. */
    SUCCEEDED(14),

    /** Has killed in Yarn. */
    KILLED(-9);

    private final int value;

    SparkAppStateEnum(int value) {
        this.value = value;
    }

    public static SparkAppStateEnum of(Integer state) {
        for (SparkAppStateEnum appState : values()) {
            if (appState.value == state) {
                return appState;
            }
        }
        return SparkAppStateEnum.OTHER;
    }

    public static SparkAppStateEnum of(String name) {
        for (SparkAppStateEnum appState : values()) {
            if (appState.name().equals(name)) {
                return appState;
            }
        }
        return SparkAppStateEnum.OTHER;
    }

    public static boolean isEndState(Integer appState) {
        SparkAppStateEnum sparkAppStateEnum = SparkAppStateEnum.of(appState);
        return SparkAppStateEnum.FAILED == sparkAppStateEnum
            || SparkAppStateEnum.KILLED == sparkAppStateEnum
            || SparkAppStateEnum.FINISHED == sparkAppStateEnum
            || SparkAppStateEnum.SUCCEEDED == sparkAppStateEnum
            || SparkAppStateEnum.LOST == sparkAppStateEnum;
    }

    public static boolean isLost(Integer appState) {
        SparkAppStateEnum sparkAppStateEnum = SparkAppStateEnum.of(appState);
        return SparkAppStateEnum.LOST == sparkAppStateEnum;
    }
}
