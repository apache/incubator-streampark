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

import org.apache.streampark.common.util.Utils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Snapshot for a BuildPipeline instance.
 *
 * @param emitTime snapshot interception time
 * @param stepStatus StepSeq -> (PipeStepStatus -> status update timestamp)
 */
public class PipelineSnapshot {

    private final String appName;
    private final PipelineTypeEnum pipeType;
    private final PipelineStatusEnum pipeStatus;
    private final int curStep;
    private final int allSteps;
    private final Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> stepStatus;
    private final PipeError error;
    private final long emitTime;

    public PipelineSnapshot(
                            String appName,
                            PipelineTypeEnum pipeType,
                            PipelineStatusEnum pipeStatus,
                            int curStep,
                            int allSteps,
                            Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> stepStatus,
                            PipeError error,
                            long emitTime) {
        this.appName = appName;
        this.pipeType = pipeType;
        this.pipeStatus = pipeStatus;
        this.curStep = curStep;
        this.allSteps = allSteps;
        this.stepStatus = stepStatus;
        this.error = error;
        this.emitTime = emitTime;
    }

    public String appName() {
        return appName;
    }

    public PipelineTypeEnum pipeType() {
        return pipeType;
    }

    public PipelineStatusEnum pipeStatus() {
        return pipeStatus;
    }

    public int curStep() {
        return curStep;
    }

    public int allSteps() {
        return allSteps;
    }

    public Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> stepStatus() {
        return stepStatus;
    }

    public PipeError error() {
        return error;
    }

    public long emitTime() {
        return emitTime;
    }

    public double percent() {
        return Utils.calPercent(curStep, allSteps);
    }

    public Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> stepStatusAsJava() {
        Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> result = new HashMap<>();
        stepStatus.forEach(
            (key, value) -> result.put(
                key,
                new AbstractMap.SimpleEntry<>(value.getKey(), value.getValue())));
        return result;
    }

    public Map<Integer, PipelineStepStatusEnum> pureStepStatusAsJava() {
        return stepStatus.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getKey()));
    }

    public Map<Integer, Long> stepStatusTimestampAsJava() {
        return stepStatus.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue()));
    }
}
