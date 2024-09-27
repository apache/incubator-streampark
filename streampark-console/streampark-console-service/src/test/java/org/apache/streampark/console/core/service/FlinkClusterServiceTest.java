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
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.YarnQueue;
import org.apache.streampark.console.core.service.impl.FlinkClusterServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** The unit test class for {@link FlinkClusterService}. */
class FlinkClusterServiceTest extends SpringUnitTestBase {

    @Autowired
    private FlinkClusterService flinkClusterService;

    @Autowired
    private YarnQueueService yarnQueueService;

    @AfterEach
    void cleanTestRecordsInDatabase() {
        flinkClusterService.remove(new QueryWrapper<>());
        yarnQueueService.remove(new QueryWrapper<>());
    }

    @Test
    void testCheckQueueValidationIfNeeded() {

        FlinkClusterServiceImpl clusterServiceImpl = (FlinkClusterServiceImpl) flinkClusterService;

        // ------- Test it for the create operation. -------

        final String queueLabel1 = "queue1@label1";
        final String queueLabel2 = "queue1@label2";
        final Long teamId1 = 1L;
        final Long teamId2 = 2L;

        // Test cluster with available queue
        YarnQueue yarnQueue = mockYarnQueue(teamId1, queueLabel1);
        yarnQueueService.save(yarnQueue);

        YarnQueue yarnQueue2 = mockYarnQueue(teamId2, queueLabel2);
        yarnQueueService.save(yarnQueue2);

        FlinkCluster cluster = mockYarnSessionFlinkCluster("cluster1", queueLabel1, 1L);
        assertThat(clusterServiceImpl.validateQueueIfNeeded(cluster)).isTrue();

        cluster.setYarnQueue(queueLabel2);
        assertThat(clusterServiceImpl.validateQueueIfNeeded(cluster)).isTrue();

        // Test cluster without available queue
        cluster.setYarnQueue("non-exited-queue");
        assertThat(clusterServiceImpl.validateQueueIfNeeded(cluster)).isFalse();

        // ------- Test it for the update operation. -------
        final String queue1Label1 = "queue1@label1";
        final String queue1Label2 = "queue1@label2";
        final String nonExistedQueue = "nonExistedQueue";
        final String clusterName = "cluster1";
        final Long targetVersion = 1L;
        final Long teamId3 = 3L;
        final Long teamId4 = 4L;

        // Test update for both versions in yarn-session with same yarn queue
        FlinkCluster cluster1 = mockYarnSessionFlinkCluster(clusterName, queue1Label1, targetVersion);
        FlinkCluster cluster2 = mockYarnSessionFlinkCluster(clusterName, queue1Label1, targetVersion);
        assertThat(clusterServiceImpl.validateQueueIfNeeded(cluster1, cluster2)).isTrue();

        // Test available queue
        YarnQueue yarnQueueLabel1 = mockYarnQueue(teamId3, queue1Label1);
        yarnQueueService.save(yarnQueueLabel1);
        YarnQueue yarnQueueLabel2 = mockYarnQueue(teamId4, queue1Label2);
        yarnQueueService.save(yarnQueueLabel2);
        cluster2.setYarnQueue(queue1Label2);
        assertThat(clusterServiceImpl.validateQueueIfNeeded(cluster1, cluster2)).isTrue();

        // Test non-existed queue
        cluster1.setDeployMode(FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION.getMode());
        cluster2.setYarnQueue(nonExistedQueue);
        assertThat(clusterServiceImpl.validateQueueIfNeeded(cluster1, cluster2)).isFalse();
    }
}
