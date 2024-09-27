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
import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.YarnQueue;
import org.apache.streampark.console.core.service.application.FlinkApplicationManageService;
import org.apache.streampark.console.core.service.impl.YarnQueueServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Collectors;

import static org.apache.streampark.console.core.service.impl.YarnQueueServiceImpl.QUEUE_EMPTY_HINT;
import static org.apache.streampark.console.core.service.impl.YarnQueueServiceImpl.QUEUE_USED_FORMAT;
import static org.apache.streampark.console.core.util.YarnQueueLabelExpression.ERR_FORMAT_HINTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

/**
 * Test for {@link YarnQueueService}. We use {@link Execution} to specify the same thread mode to
 * avoid noisy data form h2 database.
 */
@Execution(SAME_THREAD)
class YarnQueueServiceTest extends SpringUnitTestBase {

    @Autowired
    private FlinkClusterService flinkClusterService;

    @Autowired
    private FlinkApplicationManageService applicationManageService;

    @Autowired
    private YarnQueueService yarnQueueService;

    @AfterEach
    void cleanTestRecordsInDatabase() {
        flinkClusterService.remove(new QueryWrapper<>());
        applicationManageService.remove(new QueryWrapper<>());
        yarnQueueService.remove(new QueryWrapper<>());
    }

    // For public methods.

    @Test
    void testFindYarnQueues() {
        final Long targetTeamId = 1L;
        String q1AtL1 = "q1@l1";
        String q2AtL1 = "q2@l1";
        String q3AtL1 = "q3@l1";
        String q3AtL3 = "q3@l3";
        yarnQueueService.save(mockYarnQueue(targetTeamId, q1AtL1));
        yarnQueueService.save(mockYarnQueue(targetTeamId, q2AtL1));
        yarnQueueService.save(mockYarnQueue(targetTeamId, q3AtL1));
        yarnQueueService.save(mockYarnQueue(targetTeamId, q3AtL3));
        yarnQueueService.save(mockYarnQueue(2L, q3AtL1));

        // Test for 1st page, size = 2, order by create time desc
        YarnQueue queryParams = new YarnQueue();
        queryParams.setTeamId(targetTeamId);

        queryParams.setTeamId(targetTeamId);
        RestRequest request = new RestRequest();
        request.setPageSize(5);
        request.setPageNum(1);
        request.setSortField("create_time");
        request.setSortOrder("desc");
        IPage<YarnQueue> yarnQueues = yarnQueueService.getPage(queryParams, request);
        assertThat(
            yarnQueues.getRecords().stream()
                .map(YarnQueue::getQueueLabel)
                .collect(Collectors.toList()))
                    .containsExactlyInAnyOrder(q3AtL3, q3AtL1, q2AtL1, q1AtL1);

        // Test for 1st page, size = 2, order by create time with queue_label
        queryParams.setQueueLabel("q3");
        IPage<YarnQueue> yarnQueuesWithQueueLabelLikeQuery = yarnQueueService.getPage(queryParams, request);
        assertThat(
            yarnQueuesWithQueueLabelLikeQuery.getRecords().stream()
                .map(YarnQueue::getQueueLabel)
                .collect(Collectors.toList()))
                    .containsExactlyInAnyOrder(q3AtL3, q3AtL1);
    }

    @Test
    void testCheckYarnQueue() {

        // Test for error format with non-empty.
        YarnQueue yarnQueue = mockYarnQueue(1L, "queue@");
        ResponseResult<String> result = yarnQueueService.checkYarnQueue(yarnQueue);
        assertThat(result.getStatus()).isEqualTo(2);
        assertThat(result.getMsg()).isEqualTo(ERR_FORMAT_HINTS);

        // Test for error format with empty.
        yarnQueue.setQueueLabel("");
        result = yarnQueueService.checkYarnQueue(yarnQueue);
        assertThat(result.getStatus()).isEqualTo(3);
        assertThat(result.getMsg()).isEqualTo(QUEUE_EMPTY_HINT);

        // Test for existed
        yarnQueue.setQueueLabel("queue1@label1");
        yarnQueueService.save(yarnQueue);

        // QueueLabel not updated
        yarnQueue.setQueueLabel("queue1@label1");
        result = yarnQueueService.checkYarnQueue(yarnQueue);
        assertThat(result.getStatus()).isEqualTo(0);

        // QueueLabel updated
        yarnQueue.setQueueLabel("queue2@label1");
        result = yarnQueueService.checkYarnQueue(yarnQueue);
        assertThat(result.getStatus()).isEqualTo(0);

        // new record but same QueueLabel
        yarnQueue.setId(null);
        yarnQueue.setQueueLabel("queue1@label1");
        result = yarnQueueService.checkYarnQueue(yarnQueue);
        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getMsg()).isEqualTo(YarnQueueServiceImpl.QUEUE_EXISTED_IN_TEAM_HINT);

        // Test for normal cases.
        yarnQueue.setQueueLabel("q1");
        result = yarnQueueService.checkYarnQueue(yarnQueue);
        assertThat(result.getStatus()).isEqualTo(0);
        assertThat(result.getMsg()).isEqualTo(YarnQueueServiceImpl.QUEUE_AVAILABLE_HINT);
    }

    /**
     * Ignored test for the queue in using. It was tested in the <code>
     * checkNotReferencedByFlinkClusters</code> & <code>checkNotReferencedByApplications</code> test
     * cases.
     */
    @Test
    void testUpdateYarnQueue() {
        final Long queueId = 1L;
        String newQueueAtNewLabel1 = "newQueue@newLable1";
        String newQueue = "newQueue";
        String mockedDesc = "mocked desc";
        // Test for same information
        YarnQueue yarnQueue = mockYarnQueue(1L, "queue1");
        yarnQueue.setId(queueId);
        yarnQueueService.save(yarnQueue);

        // Test for only change description
        yarnQueue.setDescription(mockedDesc);
        yarnQueueService.updateYarnQueue(yarnQueue);
        assertThat(yarnQueueService.getById(queueId).getDescription()).isEqualTo(mockedDesc);

        // Test for error queue label format
        yarnQueue.setQueueLabel("q1@");
        assertThatThrownBy(() -> yarnQueueService.updateYarnQueue(yarnQueue))
            .isInstanceOf(ApiAlertException.class)
            .hasMessage(ERR_FORMAT_HINTS);

        // Test for formal cases.
        yarnQueue.setQueueLabel(newQueue);
        yarnQueue.setDescription(newQueueAtNewLabel1);
        yarnQueueService.updateYarnQueue(yarnQueue);
        YarnQueue queueFromDB = yarnQueueService.getById(queueId);
        assertThat(queueFromDB.getQueueLabel()).isEqualTo(newQueue);
        assertThat(queueFromDB.getDescription()).isEqualTo(newQueueAtNewLabel1);
    }

    /**
     * Ignored due to main logic in {@link YarnQueueService#checkYarnQueue(YarnQueue)}, which has been
     * tested in corresponding cases.
     */
    @Disabled
    @Test
    void testCreateYarnQueue() {
        // Do nothing.
    }

    // For private methods

    @Test
    void testGetYarnQueueByIdWithPreconditions() {
        final String queueLabel = "queue1@label1";
        final Long targetTeamId = 1L;
        YarnQueueServiceImpl yarnQueueServiceImpl = (YarnQueueServiceImpl) yarnQueueService;

        // Test for null yarn queue
        assertThatThrownBy(() -> yarnQueueServiceImpl.getYarnQueueByIdWithPreconditions(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Yarn queue mustn't be null.");

        // Test for null yarn queue id
        YarnQueue yarnQueue = new YarnQueue();
        yarnQueue.setId(null);
        assertThatThrownBy(() -> yarnQueueServiceImpl.getYarnQueueByIdWithPreconditions(yarnQueue))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Yarn queue id mustn't be null.");

        // Test for yarn queue non-existed in database.
        yarnQueue.setId(1L);
        assertThatThrownBy(() -> yarnQueueServiceImpl.getYarnQueueByIdWithPreconditions(yarnQueue))
            .isInstanceOf(ApiAlertException.class)
            .hasMessage("The queue doesn't exist.");

        // Test for expected condition.
        yarnQueue.setQueueLabel(queueLabel);
        yarnQueue.setTeamId(targetTeamId);
        yarnQueueService.save(yarnQueue);

        yarnQueueServiceImpl.getYarnQueueByIdWithPreconditions(yarnQueue);
    }

    @Test
    void testCheckNotReferencedByFlinkClusters() {
        final String queueLabel = "queue1@label1";
        final String operation = "testing";
        YarnQueueServiceImpl yarnQueueServiceImpl = (YarnQueueServiceImpl) yarnQueueService;

        // Test for non-existed clusters.
        yarnQueueServiceImpl.checkNotReferencedByFlinkClusters(queueLabel, operation);

        // Test for existed clusters without specified yarn queue.
        flinkClusterService.save(mockYarnSessionFlinkCluster("fc2", null, 2L));

        yarnQueueServiceImpl.checkNotReferencedByFlinkClusters(queueLabel, operation);

        // Test for existed clusters with specified yarn queue.
        flinkClusterService.save(mockYarnSessionFlinkCluster("fc1", queueLabel, 1L));
        assertThatThrownBy(
            () -> yarnQueueServiceImpl.checkNotReferencedByFlinkClusters(queueLabel, operation))
                .isInstanceOf(ApiAlertException.class)
                .hasMessage(String.format(QUEUE_USED_FORMAT, "flink clusters", operation));
    }

    @Test
    void testCheckNotReferencedByApplications() {
        final String queueLabel = "queue1@label1";
        final String operation = "testing";
        final Long targetTeamId = 1L;
        YarnQueueServiceImpl yarnQueueServiceImpl = (YarnQueueServiceImpl) yarnQueueService;

        // Test for non-existed applications.
        yarnQueueServiceImpl.checkNotReferencedByApplications(targetTeamId, queueLabel, operation);

        // Test for existed applications that don't belong to the same team, not in yarn mode.
        applicationManageService.save(mockYarnModeJobApp(2L, "app1", null, FlinkDeployMode.REMOTE));
        yarnQueueServiceImpl.checkNotReferencedByApplications(targetTeamId, queueLabel, operation);

        // Test for existed applications that don't belong to the same team, in yarn mode
        applicationManageService.save(
            mockYarnModeJobApp(2L, "app2", null, FlinkDeployMode.YARN_APPLICATION));
        yarnQueueServiceImpl.checkNotReferencedByApplications(targetTeamId, queueLabel, operation);

        // Test for existed applications that belong to the same team, but not in yarn mode.
        applicationManageService.save(
            mockYarnModeJobApp(targetTeamId, "app3", null, FlinkDeployMode.REMOTE));
        yarnQueueServiceImpl.checkNotReferencedByApplications(targetTeamId, queueLabel, operation);

        // Test for existed applications that belong to the same team, but without yarn queue value.
        applicationManageService.save(
            mockYarnModeJobApp(targetTeamId, "app4", null, FlinkDeployMode.YARN_PER_JOB));
        yarnQueueServiceImpl.checkNotReferencedByApplications(targetTeamId, queueLabel, operation);

        // Test for existed applications, some apps belong to the same team, but others don't belong to.
        applicationManageService.save(
            mockYarnModeJobApp(targetTeamId, "app5", queueLabel, FlinkDeployMode.YARN_PER_JOB));
        assertThatThrownBy(
            () -> yarnQueueServiceImpl.checkNotReferencedByApplications(
                targetTeamId, queueLabel, operation))
                    .isInstanceOf(ApiAlertException.class)
                    .hasMessage(String.format(QUEUE_USED_FORMAT, "applications",
                        operation));
    }
}
