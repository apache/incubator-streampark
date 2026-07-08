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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class PipelineSnapshot {

    private String appName;
    private PipelineTypeEnum pipeType;
    private PipelineStatusEnum pipeStatus;
    private int curStep;
    private int allSteps;
    private Map<Integer, Map.Entry<PipelineStepStatusEnum, Long>> stepStatus;
    private PipeError error;
    private long emitTime;

    public double percent() {
        return Utils.calPercent(curStep, allSteps);
    }

    public Map<Integer, PipelineStepStatusEnum> pureStepStatusAsJava() {
        Map<Integer, PipelineStepStatusEnum> map = new HashMap<>();
        stepStatus.forEach((k, v) -> map.put(k, v.getKey()));
        return map;
    }

    public Map<Integer, Long> stepStatusTimestampAsJava() {
        Map<Integer, Long> map = new HashMap<>();
        stepStatus.forEach((k, v) -> map.put(k, v.getValue()));
        return map;
    }
}
