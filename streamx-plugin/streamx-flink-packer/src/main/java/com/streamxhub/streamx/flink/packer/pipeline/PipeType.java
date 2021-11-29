/*
 * Copyright (c) 2021 The StreamX Project
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
package com.streamxhub.streamx.flink.packer.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Building pipeline type.
 *
 * @author Al-assad
 */
public enum PipeType {

    /**
     * unknown type
     */
    UNKNOWN(0, "", ImmutableMap.of()),

    /**
     * flink native kubernetes session mode
     */
    FLINK_NATIVE_K8S_SESSION(
        1,
        "flink native kubernetes session mode task building pipeline",
        ImmutableMap.<Integer, String>builder()
            .put(1, "create building workspace")
            .put(2, "build shaded flink app jar")
            .build()
    ),

    /**
     * flink native kubernetes application mode
     */
    FLINK_NATIVE_K8S_APPLICATION(
        2,
        "flink native kubernetes session mode task building pipeline",
        ImmutableMap.<Integer, String>builder()
            .put(1, "create building workspace")
            .put(2, "export kubernetes pod template")
            .put(3, "build shaded flink app jar")
            .put(4, "export flink app dockerfile")
            .put(5, "pull flink app base docker image")
            .put(6, "build flink app docker image")
            .put(7, "push flink app docker image")
            .build()
    ),

    // todo FLINK_YARN_APPLICATION(),
    // todo FLINK_YARN_SESSION(),
    // todo FLINK_STANDALONE(),
    ;


    private final Integer code;
    /**
     * short description of pipeline type.
     */
    private final String desc;
    /**
     * building steps of pipeline, element => [sorted seq -> step desc].
     */
    private final Map<Integer, String> steps;

    PipeType(Integer code, String desc, Map<Integer, String> steps) {
        this.code = code;
        this.desc = desc;
        this.steps = steps;
    }

    @JsonCreator
    public static PipeType of(Integer code) {
        for (PipeType type : PipeType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public static PipeType ofName(String name) {
        PipeType r = PipeType.valueOf(name);
        return r == null ? UNKNOWN : r;
    }

    @JsonValue
    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public Map<Integer, String> getSteps() {
        return steps;
    }

    public boolean isUnknown() {
        return this == UNKNOWN;
    }

}

