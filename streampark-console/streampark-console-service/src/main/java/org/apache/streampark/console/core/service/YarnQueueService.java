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

import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.SparkDeployMode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.YarnQueue;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface YarnQueueService extends IService<YarnQueue> {

    /**
     * Retrieves a page of {@link YarnQueue} objects based on the provided parameters.
     *
     * @param yarnQueue The {@link YarnQueue} object containing the search criteria.
     * @param restRequest The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link YarnQueue} objects.
     */
    IPage<YarnQueue> getPage(YarnQueue yarnQueue, RestRequest restRequest);

    /**
     * Check the correctness of yarnQueue
     *
     * @param yarnQueue YarnQueue
     * @return ResponseResult
     */
    ResponseResult<String> checkYarnQueue(YarnQueue yarnQueue);

    /**
     * Create a YarnQueue by entering parameters
     *
     * @param yarnQueue YarnQueue
     * @return
     */
    boolean createYarnQueue(YarnQueue yarnQueue);

    /**
     * Update YarnQueue based on input parameters
     *
     * @param yarnQueue YarnQueue to be updated
     */
    void updateYarnQueue(YarnQueue yarnQueue);

    /**
     * Remove YarnQueue based on input parameters
     *
     * @param yarnQueue YarnQueue to be removed
     */
    void remove(YarnQueue yarnQueue);

    /**
     * Check queue label by given parameters
     *
     * @param deployModeEnum FlinkDeployMode
     * @param queueLabel queue Label
     */
    void checkQueueLabel(FlinkDeployMode deployModeEnum, String queueLabel);

    void checkQueueLabel(SparkDeployMode deployModeEnum, String queueLabel);

    /**
     * Determine whether it is the default queue by the given queue label
     *
     * @param queueLabel Queue label
     * @return Whether the returned result is true
     */
    boolean isDefaultQueue(String queueLabel);

    /**
     * Check if queue exists by given queue label
     *
     * @param queueLabel queue label
     * @return Whether the returned result is true
     */
    boolean existByQueueLabel(String queueLabel);

    /**
     * Check if queue exists by given queue label and team id
     *
     * @param teamId team id
     * @param queueLabel queue label
     * @return Whether the returned result is true
     */
    boolean existByTeamIdQueueLabel(Long teamId, String queueLabel);
}
