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

package org.apache.streampark.console.core.service.application.impl;

import org.apache.streampark.console.core.entity.ApplicationBuildPipeline;
import org.apache.streampark.console.core.mapper.ApplicationBuildPipelineMapper;
import org.apache.streampark.flink.packer.pipeline.PipelineStatusEnum;

import org.apache.commons.collections.CollectionUtils;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractApplicationBuildPipelineService
    extends
        ServiceImpl<ApplicationBuildPipelineMapper, ApplicationBuildPipeline> {

    public Optional<ApplicationBuildPipeline> getCurrentBuildPipeline(@Nonnull Long appId) {
        return Optional.ofNullable(getById(appId));
    }

    public boolean allowToBuildNow(@Nonnull Long appId) {
        return getCurrentBuildPipeline(appId)
            .map(pipeline -> PipelineStatusEnum.RUNNING != pipeline.getPipelineStatus())
            .orElse(true);
    }

    public Map<Long, PipelineStatusEnum> listAppIdPipelineStatusMap(List<Long> appIds) {
        if (CollectionUtils.isEmpty(appIds)) {
            return new HashMap<>();
        }
        List<ApplicationBuildPipeline> appBuildPipelines =
            this.lambdaQuery().in(ApplicationBuildPipeline::getAppId, appIds).list();
        if (CollectionUtils.isEmpty(appBuildPipelines)) {
            return new HashMap<>();
        }
        return appBuildPipelines.stream()
            .collect(Collectors.toMap(ApplicationBuildPipeline::getAppId, ApplicationBuildPipeline::getPipelineStatus));
    }

    public void removeByAppId(Long appId) {
        this.lambdaUpdate().eq(ApplicationBuildPipeline::getAppId, appId).remove();
    }

    public boolean saveEntity(ApplicationBuildPipeline pipe) {
        ApplicationBuildPipeline old = getById(pipe.getAppId());
        if (old == null) {
            return save(pipe);
        }
        return updateById(pipe);
    }
}
