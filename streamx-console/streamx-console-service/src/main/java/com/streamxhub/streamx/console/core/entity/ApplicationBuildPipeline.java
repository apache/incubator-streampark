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
package com.streamxhub.streamx.console.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.streamxhub.streamx.console.base.util.JsonUtils;
import com.streamxhub.streamx.flink.packer.pipeline.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Building pipeline state for Application.
 * Each Application instance will have only one corresponding ApplicationBuildPipeline record.
 *
 * @author Al-assad
 */
@TableName("t_app_build_pipe")
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ApplicationBuildPipeline {

    @TableId(value = "app_id")
    private Long appId;

    @TableField(value = "pipe_type")
    private String pipeTypeName;

    @TableField(value = "pipe_status")
    private Integer pipeStatusCode;

    @TableField(value = "cur_step")
    private Integer curStep;

    @TableField(value = "total_step")
    private Integer totalStep;

    @TableField(value = "steps_status")
    private String stepStatusJson;

    @TableField(value = "error")
    private String errorJson;

    @TableField(value = "build_result")
    private String buildResultJson;

    @TableField(value = "update_time")
    private Date updateTime;

    @Nonnull
    @JsonIgnore
    public PipeType getPipeType() {
        return PipeType.ofName(pipeTypeName);
    }

    @JsonIgnore
    public ApplicationBuildPipeline setPipeType(@Nonnull PipeType pipeType) {
        this.pipeTypeName = pipeType.name();
        return this;
    }

    @Nonnull
    @JsonIgnore
    public PipeStatus getPipeStatus() {
        return PipeStatus.of(pipeStatusCode);
    }

    @JsonIgnore
    public ApplicationBuildPipeline setPipeStatus(@Nonnull PipeStatus pipeStatus) {
        this.pipeStatusCode = pipeStatus.getCode();
        return this;
    }

    @Nonnull
    @JsonIgnore
    public Map<Integer, PipeStepStatus> getStepStatus() {
        if (StringUtils.isBlank(stepStatusJson)) {
            return Maps.newHashMap();
        }
        try {
            return JsonUtils.MAPPER.readValue(stepStatusJson, new TypeReference<HashMap<Integer, PipeStepStatus>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("[streamx] json parse error on ApplicationBuildPipeline, stepStatusJson={}", stepStatusJson, e);
            return Maps.newHashMap();
        }
    }

    @JsonIgnore
    public ApplicationBuildPipeline setStepStatus(@Nonnull Map<Integer, PipeStepStatus> stepStatus) {
        try {
            this.stepStatusJson = JsonUtils.MAPPER.writeValueAsString(stepStatus);
        } catch (JsonProcessingException e) {
            log.error("[streamx] json parse error on ApplicationBuildPipeline, stepStatusMap=({})",
                stepStatus.entrySet().stream().map(et -> et.getKey() + "->" + et.getValue()).collect(Collectors.joining(",")), e);
        }
        return this;
    }

    @Nonnull
    @JsonIgnore
    public PipeErr getError() {
        if (StringUtils.isBlank(errorJson)) {
            return PipeErr.empty();
        }
        try {
            return JsonUtils.MAPPER.readValue(errorJson, PipeErr.class);
        } catch (JsonProcessingException e) {
            log.error("[streamx] json parse error on ApplicationBuildPipeline, errorJson={}", errorJson, e);
            return PipeErr.empty();
        }
    }


    @JsonIgnore
    public ApplicationBuildPipeline setError(@Nonnull PipeErr error) {
        try {
            this.errorJson = JsonUtils.MAPPER.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            log.error("[streamx] json parse error on ApplicationBuildPipeline, error={}", error, e);
        }
        return this;
    }

    @JsonIgnore
    public <R extends BuildResult> ApplicationBuildPipeline setBuildResult(@Nonnull R result) {
        try {
            this.buildResultJson = JsonUtils.MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("[streamx] json parse error on ApplicationBuildPipeline, buildResult={}", result, e);
        }
        return this;
    }

    /**
     * Only return null when getPipeType() = UNKNOWN or json covert error,
     * The return class type depend on PipeType.ResultType.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    @JsonIgnore
    public <R extends BuildResult> R getBuildResult() {
        PipeType pipeType = getPipeType();
        if (pipeType.isUnknown()) {
            return null;
        }
        try {
            return (R) JsonUtils.MAPPER.readValue(buildResultJson, pipeType.getResultType());
        } catch (JsonProcessingException e) {
            log.error("[streamx] json parse error on ApplicationBuildPipeline, buildResultJson={}", buildResultJson, e);
            return null;
        }
    }


    public static ApplicationBuildPipeline initFromPipeline(@Nonnull BuildPipeline pipeline) {
        return new ApplicationBuildPipeline()
            .setPipeType(pipeline.pipeType())
            .setPipeStatus(pipeline.pipeStatus())
            .setTotalStep(pipeline.allSteps())
            .setCurStep(pipeline.curStep())
            .setStepStatus(pipeline.getStepsStatusAsJava())
            .setUpdateTime(new Date());
    }

    public static ApplicationBuildPipeline fromPipeSnapshot(@Nonnull PipeSnapshot snapshot) {
        return new ApplicationBuildPipeline()
            .setPipeType(snapshot.pipeType())
            .setPipeStatus(snapshot.pipeStatus())
            .setTotalStep(snapshot.allSteps())
            .setCurStep(snapshot.curStep())
            .setStepStatus(snapshot.stepStatusAsJava())
            .setError(snapshot.error())
            .setUpdateTime(new Date(snapshot.emitTime()));
    }
}
