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

import org.apache.streampark.common.enums.SparkJobType;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.SimpleBuildResponse;
import org.apache.streampark.flink.packer.pipeline.SparkYarnBuildRequest;

/** Building pipeline for spark yarn application mode */
public class SparkYarnBuildPipeline extends BuildPipeline {

    private final SparkYarnBuildRequest request;

    public SparkYarnBuildPipeline(SparkYarnBuildRequest request) {
        this.request = request;
    }

    @Override
    public PipelineTypeEnum pipeType() {
        return PipelineTypeEnum.SPARK_CLUSTER;
    }

    @Override
    public SparkYarnBuildRequest offerBuildParam() {
        return request;
    }

    @Override
    public SimpleBuildResponse buildProcess() {
        runYarnSqlBuildSteps(
            request.localWorkspace(),
            request.yarnProvidedPath(),
            request.jobType() == SparkJobType.SPARK_SQL,
            request.dependencyInfo());
        return new SimpleBuildResponse();
    }

    public static SparkYarnBuildPipeline of(SparkYarnBuildRequest request) {
        return new SparkYarnBuildPipeline(request);
    }
}
