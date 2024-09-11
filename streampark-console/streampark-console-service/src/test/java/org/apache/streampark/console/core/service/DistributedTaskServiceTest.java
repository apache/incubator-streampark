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

import org.apache.streampark.console.core.bean.FlinkTaskItem;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.DistributedTask;
import org.apache.streampark.console.core.enums.DistributedTaskEnum;
import org.apache.streampark.console.core.service.impl.DistributedTaskServiceImpl;

import com.fasterxml.jackson.core.JacksonException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class DistributedTaskServiceTest {

    private final DistributedTaskServiceImpl distributionTaskService = new DistributedTaskServiceImpl();

    private final String serverName = "testServer";

    // the number of virtual nodes for each server
    private final int numberOfReplicas = 2 << 16;

    @Test
    void testInit() {
        distributionTaskService.init(serverName);
        assert (distributionTaskService.getConsistentHashSize() == numberOfReplicas);
    }

    @Test
    void testIsLocalProcessing() {
        distributionTaskService.init(serverName);
        for (long i = 0; i < numberOfReplicas; i++) {
            assert (distributionTaskService.isLocalProcessing(i));
        }
    }

    @Test
    void testGetTaskAndApp() {
        Application application = new Application();
        application.setId(0L);
        try {
            DistributedTask DistributedTask =
                distributionTaskService.getDistributedTaskByApp(application, false, DistributedTaskEnum.START);
            FlinkTaskItem flinkTaskItem = distributionTaskService.getFlinkTaskItem(DistributedTask);
            Application newApplication = distributionTaskService.getAppByFlinkTaskItem(flinkTaskItem);
            assert (application.equals(newApplication));
        } catch (JacksonException e) {
            log.error("testGetTaskAndApp failed:", e);
        }
    }

}
