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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.PipeError;
import org.apache.streampark.flink.packer.pipeline.PipelineSnapshot;
import org.apache.streampark.flink.packer.pipeline.PipelineStatusEnum;
import org.apache.streampark.flink.packer.pipeline.PipelineStepStatusEnum;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Building pipeline state for Application. Each Application instance will have only one
 * corresponding ApplicationBuildPipeline record.
 */
@TableName("t_app_build_pipe")
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ApplicationBuildPipeline {

    @TableId(type = IdType.INPUT)
    private Long appId;

    @TableField(value = "pipe_type")
    private Integer pipeTypeCode;

    @TableField(value = "pipe_status")
    private Integer pipeStatusCode;

    private Integer curStep;

    private Integer totalStep;

    // step status map: (stepSeq -> stepStatus)
    @TableField(value = "steps_status")
    private String stepStatusJson;

    // step status update timestamp map: (stepSeq -> update timestamp)
    @TableField(value = "steps_status_ts")
    private String stepStatusTimestampJson;

    @TableField(value = "error")
    private String errorJson;

    @TableField(value = "build_result")
    private String buildResultJson;

    private Date modifyTime;

    @Nonnull
    @JsonIgnore
    public PipelineTypeEnum getPipeType() {
        return PipelineTypeEnum.of(pipeTypeCode);
    }

    @JsonIgnore
    public ApplicationBuildPipeline setPipeType(@Nonnull PipelineTypeEnum pipeType) {
        this.pipeTypeCode = pipeType.getCode();
        return this;
    }

    @Nonnull
    @JsonIgnore
    public PipelineStatusEnum getPipelineStatus() {
        return PipelineStatusEnum.of(pipeStatusCode);
    }

    @JsonIgnore
    public ApplicationBuildPipeline setPipeStatus(@Nonnull PipelineStatusEnum pipeStatus) {
        this.pipeStatusCode = pipeStatus.getCode();
        return this;
    }

    @Nonnull
    @JsonIgnore
    public Map<Integer, PipelineStepStatusEnum> getStepStatus() {
        if (StringUtils.isBlank(stepStatusJson)) {
            return new HashMap<>();
        }
        try {
            return JacksonUtils.read(
                stepStatusJson, new TypeReference<HashMap<Integer, PipelineStepStatusEnum>>() {
                });
        } catch (JsonProcessingException e) {
            log.error(
                "json parse error on ApplicationBuildPipeline, stepStatusJson={}", stepStatusJson, e);
            return new HashMap<>();
        }
    }

    @JsonIgnore
    public ApplicationBuildPipeline setStepStatus(@Nonnull Map<Integer, PipelineStepStatusEnum> stepStatus) {
        try {
            this.stepStatusJson = JacksonUtils.write(stepStatus);
        } catch (JsonProcessingException e) {
            log.error(
                "json parse error on ApplicationBuildPipeline, stepStatusMap=({})",
                stepStatus.entrySet().stream()
                    .map(et -> et.getKey() + "->" + et.getValue())
                    .collect(Collectors.joining(",")),
                e);
        }
        return this;
    }

    @Nonnull
    @JsonIgnore
    public Map<Integer, Long> getStepStatusTimestamp() {
        if (StringUtils.isBlank(stepStatusTimestampJson)) {
            return new HashMap<>();
        }
        try {
            return JacksonUtils.read(
                stepStatusTimestampJson, new TypeReference<HashMap<Integer, Long>>() {
                });
        } catch (JsonProcessingException e) {
            log.error(
                "json parse error on ApplicationBuildPipeline, stepStatusJson={}",
                stepStatusTimestampJson,
                e);
            return new HashMap<>();
        }
    }

    @JsonIgnore
    public ApplicationBuildPipeline setStepStatusTimestamp(@Nonnull Map<Integer, Long> stepStatusSt) {
        try {
            this.stepStatusTimestampJson = JacksonUtils.write(stepStatusSt);
        } catch (JsonProcessingException e) {
            log.error(
                "json parse error on ApplicationBuildPipeline, stepStatusSt=({})",
                stepStatusSt.entrySet().stream()
                    .map(et -> et.getKey() + "->" + et.getValue())
                    .collect(Collectors.joining(",")),
                e);
        }
        return this;
    }

    @Nonnull
    @JsonIgnore
    public PipeError getError() {
        if (StringUtils.isBlank(errorJson)) {
            return PipeError.empty();
        }
        try {
            return JacksonUtils.read(errorJson, PipeError.class);
        } catch (JsonProcessingException e) {
            log.error("json parse error on ApplicationBuildPipeline, errorJson={}", errorJson, e);
            return PipeError.empty();
        }
    }

    @JsonIgnore
    public ApplicationBuildPipeline setError(@Nonnull PipeError error) {
        try {
            this.errorJson = JacksonUtils.write(error);
        } catch (JsonProcessingException e) {
            log.error("json parse error on ApplicationBuildPipeline, error={}", error, e);
        }
        return this;
    }

    @JsonIgnore
    public <R extends BuildResult> ApplicationBuildPipeline setBuildResult(@Nonnull R result) {
        try {
            this.buildResultJson = JacksonUtils.write(result);
        } catch (JsonProcessingException e) {
            log.error("json parse error on ApplicationBuildPipeline, buildResult={}", result, e);
        }
        return this;
    }

    public long calCostSecond() {
        // max timestamp - min timestamp in stepStatusTimestamp
        Map<Integer, Long> st = getStepStatusTimestamp();
        if (st.isEmpty()) {
            return 0;
        }
        long max = st.values().stream().max(Long::compareTo).orElse(0L);
        long min = st.values().stream().min(Long::compareTo).orElse(0L);
        return (max - min) / 1000;
    }

    /**
     * Only return null when getPipeType() = UNKNOWN or json covert error, The return class type
     * depend on PipeType.ResultType.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    @JsonIgnore
    public <R extends BuildResult> R getBuildResult() {
        PipelineTypeEnum pipeType = getPipeType();
        if (pipeType.isUnknown() || buildResultJson == null) {
            return null;
        }
        try {
            return (R) JacksonUtils.read(buildResultJson, pipeType.getResultType());
        } catch (JsonProcessingException e) {
            log.error(
                "json parse error on ApplicationBuildPipeline, buildResultJson={}", buildResultJson, e);
            return null;
        }
    }

    /** Initialize from BuildPipeline */
    public static ApplicationBuildPipeline initFromPipeline(@Nonnull BuildPipeline pipeline) {
        return fromPipeSnapshot(pipeline.snapshot());
    }

    /** Create object from PipeSnapshot */
    public static ApplicationBuildPipeline fromPipeSnapshot(@Nonnull PipelineSnapshot snapshot) {
        return new ApplicationBuildPipeline()
            .setPipeType(snapshot.pipeType())
            .setPipeStatus(snapshot.pipeStatus())
            .setTotalStep(snapshot.allSteps())
            .setCurStep(snapshot.curStep())
            .setStepStatus(snapshot.pureStepStatusAsJava())
            .setStepStatusTimestamp(snapshot.stepStatusTimestampAsJava())
            .setError(snapshot.error())
            .setModifyTime(new Date(snapshot.emitTime()));
    }

    /** Covert to view object */
    public View toView() {
        return View.of(this);
    }

    /** View object of AppBuildPipeline */
    @Getter
    @Setter
    @Accessors(chain = true)
    @NoArgsConstructor
    public static class View {

        private Long appId;
        private Integer pipeType;
        private Integer pipeStatus;
        private Integer curStep;
        private Integer totalStep;
        private Double percent;
        private Long costSec;
        private List<Step> steps;
        private Boolean hasError;
        private String errorSummary;
        private String errorStack;
        private Date updateTime;

        public static View of(@Nonnull ApplicationBuildPipeline pipe) {
            // combine step info
            Map<Integer, String> stepDesc = pipe.getPipeType().getSteps();
            Map<Integer, PipelineStepStatusEnum> stepStatus = pipe.getStepStatus();
            Map<Integer, Long> stepTs = pipe.getStepStatusTimestamp();
            List<Step> steps = new ArrayList<>(stepDesc.size());
            for (int i = 1; i <= pipe.getPipeType().getSteps().size(); i++) {
                Step step = new Step()
                    .setSeq(i)
                    .setDesc(stepDesc.getOrDefault(i, "unknown step"))
                    .setStatus(stepStatus.getOrDefault(i, PipelineStepStatusEnum.unknown).getCode());
                Long st = stepTs.get(i);
                if (st != null) {
                    step.setTs(new Date(st));
                }
                steps.add(step);
            }

            return new View()
                .setAppId(pipe.getAppId())
                .setPipeType(pipe.getPipeTypeCode())
                .setPipeStatus(pipe.getPipeStatusCode())
                .setCurStep(pipe.getCurStep())
                .setTotalStep(pipe.getTotalStep())
                .setPercent(
                    Utils.calPercent(
                        pipe.getBuildResult() == null ? pipe.getCurStep() - 1
                            : pipe.getCurStep(),
                        pipe.getTotalStep()))
                .setCostSec(pipe.calCostSecond())
                .setSteps(steps)
                .setHasError(pipe.getError().nonEmpty())
                .setErrorSummary(pipe.getError().summary())
                .setErrorStack(pipe.getError().exceptionStack())
                .setUpdateTime(pipe.getModifyTime());
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @NoArgsConstructor
    public static class Step {

        private Integer seq;
        private String desc;
        private Integer status;
        private Date ts;
    }
}
