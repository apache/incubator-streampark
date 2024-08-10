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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.common.enums.SparkExecutionMode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.enums.YarnMessageStatus;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.YarnQueue;
import org.apache.streampark.console.core.mapper.YarnQueueMapper;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.YarnQueueService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.streampark.console.base.enums.CommonStatus.APPLICATION;
import static org.apache.streampark.console.base.enums.CommonStatus.FLINK_CLUSTERS;
import static org.apache.streampark.console.base.enums.UserMessageStatus.SYSTEM_TEAM_ID_NULL_ERROR;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_ID_NULL;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_LABEL_AVAILABLE;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_LABEL_EXIST;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_LABEL_FORMAT;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_LABEL_NULL;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_NOT_EXIST;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_NULL;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_QUERY_PARAMS_NULL;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_QUERY_PARAMS_TEAM_ID_NULL;
import static org.apache.streampark.console.base.enums.YarnMessageStatus.YARN_QUEUE_USED_FORMAT;
import static org.apache.streampark.console.core.util.YarnQueueLabelExpression.isValid;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class YarnQueueServiceImpl extends ServiceImpl<YarnQueueMapper, YarnQueue>
    implements
        YarnQueueService {

    public static final String DEFAULT_QUEUE = "default";

    @Autowired
    private ApplicationManageService applicationManageService;
    @Autowired
    private FlinkClusterService flinkClusterService;

    @Override
    public IPage<YarnQueue> getPage(YarnQueue yarnQueue, RestRequest request) {
        ApiAlertException.throwIfNull(yarnQueue, YARN_QUEUE_QUERY_PARAMS_NULL);
        ApiAlertException.throwIfNull(yarnQueue.getTeamId(), YARN_QUEUE_QUERY_PARAMS_TEAM_ID_NULL);
        Page<YarnQueue> page = MybatisPager.getPage(request);
        return this.baseMapper.selectPage(page, yarnQueue);
    }

    /**
     * Check for the yarn queue if exists in the team or the queue name and label format is invalid.
     * status msg 0 Success 1 The queue already existed in the team, 2 The queue name and label format
     * is invalid, 3 The queue name and label is empty.
     */
    @Override
    public ResponseResult<String> checkYarnQueue(YarnQueue yarnQueue) {

        ApiAlertException.throwIfNull(yarnQueue, YARN_QUEUE_NULL);
        ApiAlertException.throwIfNull(yarnQueue.getTeamId(), SYSTEM_TEAM_ID_NULL_ERROR);

        ResponseResult<String> responseResult = new ResponseResult<>();

        if (StringUtils.isBlank(yarnQueue.getQueueLabel())) {
            responseResult.setStatus(3);
            responseResult.setMsg(YARN_QUEUE_LABEL_NULL.getMessage());
            return responseResult;
        }

        boolean valid = isValid(yarnQueue.getQueueLabel());
        if (!valid) {
            responseResult.setStatus(2);
            responseResult.setMsg(YARN_QUEUE_LABEL_FORMAT.getMessage());
            return responseResult;
        }

        boolean existed = this.baseMapper.existsByQueueLabel(yarnQueue);

        if (existed) {
            responseResult.setStatus(1);
            responseResult.setMsg(YARN_QUEUE_LABEL_EXIST.getMessage());
            return responseResult;
        }
        responseResult.setStatus(0);
        responseResult.setMsg(YARN_QUEUE_LABEL_AVAILABLE.getMessage());
        return responseResult;
    }

    @Override
    public boolean createYarnQueue(YarnQueue yarnQueue) {
        ResponseResult<String> checkResponse = checkYarnQueue(yarnQueue);

        YarnMessageStatus status = null;
        switch (checkResponse.getStatus()) {
            case 1:
                status = YARN_QUEUE_LABEL_EXIST;
                break;
            case 2:
                status = YARN_QUEUE_LABEL_FORMAT;
                break;
            case 3:
                status = YARN_QUEUE_LABEL_NULL;
                break;
            default:
                status = YARN_QUEUE_LABEL_AVAILABLE;
        }
        ApiAlertException.throwIfFalse(checkResponse.getStatus() == 0, status);
        return save(yarnQueue);
    }

    @Override
    public void updateYarnQueue(YarnQueue yarnQueue) {

        YarnQueue queueFromDB = getYarnQueueByIdWithPreconditions(yarnQueue);

        // 1) no data to update
        if (StringUtils.equals(yarnQueue.getQueueLabel(), queueFromDB.getQueueLabel())
            && StringUtils.equals(yarnQueue.getDescription(), queueFromDB.getDescription())) {
            return;
        }

        // 2 update description
        if (StringUtils.equals(yarnQueue.getQueueLabel(), queueFromDB.getQueueLabel())) {
            queueFromDB.setDescription(yarnQueue.getDescription());
            updateById(queueFromDB);
            return;
        }

        // 3 update yarnQueue
        ApiAlertException.throwIfFalse(isValid(yarnQueue.getQueueLabel()), YARN_QUEUE_LABEL_FORMAT);

        checkNotReferencedByApplications(
            queueFromDB.getTeamId(), queueFromDB.getQueueLabel(), "updating");

        checkNotReferencedByFlinkClusters(queueFromDB.getQueueLabel(), "updating");

        queueFromDB.setDescription(yarnQueue.getDescription());
        queueFromDB.setQueueLabel(yarnQueue.getQueueLabel());
        updateById(queueFromDB);
    }

    @Override
    public void remove(YarnQueue yarnQueue) {
        YarnQueue queueFromDB = getYarnQueueByIdWithPreconditions(yarnQueue);

        checkNotReferencedByApplications(
            queueFromDB.getTeamId(), queueFromDB.getQueueLabel(), "deleting");

        checkNotReferencedByFlinkClusters(queueFromDB.getQueueLabel(), "deleting");

        removeById(yarnQueue.getId());
    }

    /**
     * Only check the validation of queue-labelExpression when using yarn application or yarn-session
     * mode or yarn-perjob mode.
     *
     * @param executionModeEnum execution mode.
     * @param queueLabel        queueLabel expression.
     */
    @Override
    public void checkQueueLabel(FlinkExecutionMode executionModeEnum, String queueLabel) {
        if (FlinkExecutionMode.isYarnMode(executionModeEnum)) {
            ApiAlertException.throwIfFalse(isValid(queueLabel, true), YARN_QUEUE_LABEL_FORMAT);
        }
    }

    @Override
    public void checkQueueLabel(SparkExecutionMode executionModeEnum, String queueLabel) {
        if (SparkExecutionMode.isYarnMode(executionModeEnum)) {
            ApiAlertException.throwIfFalse(isValid(queueLabel, true), YARN_QUEUE_LABEL_FORMAT);
        }
    }

    @Override
    public boolean isDefaultQueue(String queueLabel) {
        return StringUtils.equals(DEFAULT_QUEUE, queueLabel) || StringUtils.isBlank(queueLabel);
    }

    @Override
    public boolean existByQueueLabel(String queueLabel) {
        return getBaseMapper()
            .exists(new LambdaQueryWrapper<YarnQueue>().eq(YarnQueue::getQueueLabel, queueLabel));
    }

    @Override
    public boolean existByTeamIdQueueLabel(Long teamId, String queueLabel) {
        return getBaseMapper()
            .exists(
                new LambdaQueryWrapper<YarnQueue>()
                    .eq(YarnQueue::getTeamId, teamId)
                    .eq(YarnQueue::getQueueLabel, queueLabel));
    }

    // --------- private methods------------

    @VisibleForTesting
    public YarnQueue getYarnQueueByIdWithPreconditions(YarnQueue yarnQueue) {
        ApiAlertException.throwIfNull(yarnQueue, YARN_QUEUE_NULL);
        ApiAlertException.throwIfNull(yarnQueue.getId(), YARN_QUEUE_ID_NULL);
        YarnQueue queueFromDB = getById(yarnQueue.getId());
        ApiAlertException.throwIfNull(queueFromDB, YARN_QUEUE_NOT_EXIST);
        return queueFromDB;
    }

    @VisibleForTesting
    public void checkNotReferencedByFlinkClusters(
                                                  @Nonnull String queueLabel, @Nonnull String operation) {
        List<FlinkCluster> clustersReferenceYarnQueueLabel = flinkClusterService
            .listByExecutionModes(Sets.newHashSet(FlinkExecutionMode.YARN_SESSION))
            .stream()
            .filter(flinkCluster -> StringUtils.equals(flinkCluster.getYarnQueue(), queueLabel))
            .collect(Collectors.toList());
        ApiAlertException.throwIfFalse(
            CollectionUtils.isEmpty(clustersReferenceYarnQueueLabel),
            YARN_QUEUE_USED_FORMAT, FLINK_CLUSTERS, operation);
    }

    @VisibleForTesting
    public void checkNotReferencedByApplications(
                                                 @Nonnull Long teamId, @Nonnull String queueLabel,
                                                 @Nonnull String operation) {
        List<Application> appsReferenceQueueLabel = applicationManageService
            .listByTeamIdAndExecutionModes(
                teamId,
                Sets.newHashSet(
                    FlinkExecutionMode.YARN_APPLICATION,
                    FlinkExecutionMode.YARN_PER_JOB))
            .stream()
            .filter(
                application -> {
                    application.setYarnQueueByHotParams();
                    return StringUtils.equals(application.getYarnQueue(), queueLabel);
                })
            .collect(Collectors.toList());
        ApiAlertException.throwIfFalse(
            CollectionUtils.isEmpty(appsReferenceQueueLabel),
            YARN_QUEUE_USED_FORMAT, APPLICATION, operation);
    }
}
