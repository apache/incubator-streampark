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

package org.apache.streampark.flink.packer.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 *
 * <pre>
 * Status of per step of building pipeline.
 * state machine:
 * ┌──────────────────────────────────┐
 * │                     ┌─► success  │
 * │ pending ─► running ─┤            │
 * │              │      └─► failure  │
 * │              │             │     │
 * │              │             ▼     │
 * │              └────────► skipped  │
 * └──────────────────────────────────┘
 * </pre>
 */
public enum PipelineStepStatusEnum {

    /** Unknown step status. */
    unknown(0),
    /** Step is waiting to run. */
    waiting(1),
    /** Step is running. */
    running(2),
    /** Step finished successfully. */
    success(3),
    /** Step finished with failure. */
    failure(4),
    /** Step was skipped. */
    skipped(5);

    private final Integer code;

    PipelineStepStatusEnum(Integer code) {
        this.code = code;
    }

    @JsonCreator
    public static PipelineStepStatusEnum of(Integer code) {
        for (PipelineStepStatusEnum status : PipelineStepStatusEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return unknown;
    }

    @JsonValue
    public Integer getCode() {
        return code;
    }

    public boolean isUnknown() {
        return this == unknown;
    }
}
