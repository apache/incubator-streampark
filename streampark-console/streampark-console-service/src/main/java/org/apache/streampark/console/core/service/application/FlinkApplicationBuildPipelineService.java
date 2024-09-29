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

package org.apache.streampark.console.core.service.application;

import org.apache.streampark.console.core.entity.ApplicationBuildPipeline;
import org.apache.streampark.flink.packer.pipeline.DockerResolvedSnapshot;
import org.apache.streampark.flink.packer.pipeline.PipelineStatusEnum;

import com.baomidou.mybatisplus.extension.service.IService;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Applications can be built asynchronously, can manage pipeline and get info */
public interface FlinkApplicationBuildPipelineService extends IService<ApplicationBuildPipeline> {

    /**
     * Build application. This is an async call method.
     *
     * @param appId application id
     * @param forceBuild forced start pipeline or not
     * @return Whether the pipeline was successfully started
     */
    boolean buildApplication(@Nonnull Long appId, boolean forceBuild) throws Exception;

    /**
     * Get current build pipeline instance of specified application
     *
     * @param appId application id
     * @return ApplicationBuildPipeline instance
     */
    Optional<ApplicationBuildPipeline> getCurrentBuildPipeline(@Nonnull Long appId);

    /**
     * Get Docker resolved snapshot of specified application.
     *
     * @param appId application id
     * @return DockerResolvedSnapshot instance
     */
    DockerResolvedSnapshot getDockerProgressDetailSnapshot(@Nonnull Long appId);

    /**
     * Whether the application can currently start a new building progress
     *
     * @param appId application id
     * @return Whether construction can be started at this time
     */
    boolean allowToBuildNow(@Nonnull Long appId);

    /**
     * List pipeline status on application id list
     *
     * @param appIds list of application ids
     * @return Map structure, key is application id, value is for the pipeline state
     */
    Map<Long, PipelineStatusEnum> listAppIdPipelineStatusMap(List<Long> appIds);

    /**
     * Delete appBuildPipeline By application id
     *
     * @param appId
     */
    void removeByAppId(Long appId);
}
