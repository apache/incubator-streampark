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
package com.streamxhub.flink.monitor.core.enums;

import lombok.Getter;

@Getter
public enum FlinkAppState {

    /**
     * Application which is currently deploying.
     */
    CREATED(0),

    /**
     * Application which is currently deploying.
     */
    DEPLOYING(1),

    /**
     * Application which was just created.
     */
    NEW(2),

    /**
     * Application which is being saved.
     */
    NEW_SAVING(3),

    /**
     * Application which has been submitted.
     */
    SUBMITTED(4),

    /**
     * Application has been accepted by the scheduler
     */
    ACCEPTED(5),

    /**
     * Application which is currently running.
     */
    RUNNING(6),

    CANCELLING(7),
    /**
     * Application which was terminated by a user or admin.
     */
    CANCELED(8),

    /**
     * Application which finished successfully.
     */
    FINISHED(9),

    /**
     * Application which failed.
     */
    FAILED(10),
    
    /**
     * 失联
     */
    LOST(11);

    int value;

    FlinkAppState(int value) {
        this.value = value;
    }


}
