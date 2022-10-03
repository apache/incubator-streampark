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

package org.apache.streampark.console.core.service;

import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.flink.packer.pipeline.DockerResolvedSnapshot;
import org.apache.streampark.flink.packer.pipeline.PipelineStatus;

import com.baomidou.mybatisplus.extension.service.IService;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AppBuildPipeService extends IService<AppBuildPipeline> {

    /**
     * Build application.
     * This is an async call method.
     */
    boolean buildApplication(@Nonnull Application app) throws Exception;

    /**
     * Get current build pipeline instance of specified application
     *
     * @param appId application id
     * @return ApplicationBuildPipeline instance
     */
    Optional<AppBuildPipeline> getCurrentBuildPipeline(@Nonnull Long appId);

    /**
     * Get Docker resolved snapshot of specified application.
     */
    DockerResolvedSnapshot getDockerProgressDetailSnapshot(@Nonnull Long appId);

    /**
     * Whether the application can currently start a new building progress
     */
    boolean allowToBuildNow(@Nonnull Long appId);

    /**
     * list pipeline status on application id list
     */
    Map<Long, PipelineStatus> listPipelineStatus(List<Long> appIds);

    /**
     * delete appBuildPipeline By application
     * @param appId
     */
    void removeApp(Long appId);
}
