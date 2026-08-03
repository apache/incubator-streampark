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

package org.apache.streampark.flink.packer.pipeline.impl;

import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.FlinkYarnApplicationBuildRequest;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.SimpleBuildResponse;

/** Building pipeline for flink yarn application mode */
public class FlinkYarnApplicationBuildPipeline extends BuildPipeline {

    private final FlinkYarnApplicationBuildRequest request;

    public FlinkYarnApplicationBuildPipeline(FlinkYarnApplicationBuildRequest request) {
        this.request = request;
    }

    @Override
    public PipelineTypeEnum pipeType() {
        return PipelineTypeEnum.FLINK_YARN_APPLICATION;
    }

    @Override
    public FlinkYarnApplicationBuildRequest offerBuildParam() {
        return request;
    }

    @Override
    public SimpleBuildResponse buildProcess() {
        boolean sqlMode =
            request.flinkJobType() == FlinkJobType.FLINK_SQL
                || request.flinkJobType() == FlinkJobType.PYFLINK;
        runYarnSqlBuildSteps(
            request.localWorkspace(),
            request.yarnProvidedPath(),
            sqlMode,
            request.dependencyInfo());
        return new SimpleBuildResponse();
    }

    public static FlinkYarnApplicationBuildPipeline of(FlinkYarnApplicationBuildRequest request) {
        return new FlinkYarnApplicationBuildPipeline(request);
    }
}
