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
    UNKNOWN(0, "", ImmutableMap.of(), null),

    /**
     * flink native kubernetes session mode
     */
    FLINK_NATIVE_K8S_SESSION(
        1,
        "flink native kubernetes session mode task building pipeline",
        ImmutableMap.<Integer, String>builder()
            .put(1, "Create building workspace")
            .put(2, "Build shaded flink app jar")
            .build(),
        FlinkK8sSessionBuildResponse.class
    ),

    /**
     * flink native kubernetes application mode
     */
    FLINK_NATIVE_K8S_APPLICATION(
        2,
        "flink native kubernetes session mode task building pipeline",
        ImmutableMap.<Integer, String>builder()
            .put(1, "Create building workspace")
            .put(2, "Export kubernetes pod template")
            .put(3, "Build shaded flink app jar")
            .put(4, "Export flink app dockerfile")
            .put(5, "Pull flink app base docker image")
            .put(6, "Build flink app docker image")
            .put(7, "Push flink app docker image")
            .build(),
        FlinkK8sApplicationBuildResponse.class
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
    /**
     * type of result
     */
    private final Class<? extends BuildResult> resultType;

    PipeType(Integer code, String desc, Map<Integer, String> steps, Class<? extends BuildResult> resultType) {
        this.code = code;
        this.desc = desc;
        this.steps = steps;
        this.resultType = resultType;
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

    public Class<? extends BuildResult> getResultType() {
        return resultType;
    }

    public boolean isUnknown() {
        return this == UNKNOWN;
    }

}

