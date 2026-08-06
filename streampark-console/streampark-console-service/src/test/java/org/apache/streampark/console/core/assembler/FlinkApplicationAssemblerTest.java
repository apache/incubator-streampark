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

package org.apache.streampark.console.core.assembler;

import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.metrics.flink.JobsOverview;
import org.apache.streampark.console.core.request.flink.FlinkAppCreateRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppStartRequest;
import org.apache.streampark.console.core.response.flink.FlinkAppDashboardResponse;
import org.apache.streampark.console.core.response.flink.FlinkAppResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class FlinkApplicationAssemblerTest {

    @Test
    void shouldConvertCreateRequestToEntity() {
        FlinkAppCreateRequest request = new FlinkAppCreateRequest();
        request.setTeamId(100001L);
        request.setJobName("demo");
        request.setJobType(2);
        request.setDeployMode(4);

        FlinkApplication app = FlinkApplicationAssembler.toEntity(request);

        Assertions.assertNotNull(app);
        Assertions.assertEquals(100001L, app.getTeamId());
        Assertions.assertEquals("demo", app.getJobName());
        Assertions.assertEquals(2, app.getJobType());
        Assertions.assertEquals(4, app.getDeployMode());
    }

    @Test
    void shouldConvertStartRequestToEntity() {
        FlinkAppStartRequest request = new FlinkAppStartRequest();
        request.setId(1L);
        request.setRestoreOrTriggerSavepoint(true);
        request.setSavepointPath("/tmp/sp");
        request.setAllowNonRestored(true);

        FlinkApplication app = FlinkApplicationAssembler.toEntity(request);

        Assertions.assertEquals(1L, app.getId());
        Assertions.assertTrue(app.getRestoreOrTriggerSavepoint());
        Assertions.assertEquals("/tmp/sp", app.getSavepointPath());
        Assertions.assertTrue(app.getAllowNonRestored());
    }

    @Test
    void shouldConvertEntityToResponse() {
        FlinkApplication app = new FlinkApplication();
        app.setId(10L);
        app.setJobName("sql-job");
        app.setState(0);

        FlinkAppResponse response = FlinkApplicationAssembler.toResponse(app);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(10L, response.getId());
        Assertions.assertEquals("sql-job", response.getJobName());
        Assertions.assertEquals(0, response.getState());
    }

    @Test
    void shouldConvertDashboardMapToResponse() {
        JobsOverview.Task task = new JobsOverview.Task();
        task.setTotal(5);
        task.setRunning(2);

        Map<String, Serializable> dashboardMap = new HashMap<>();
        dashboardMap.put("task", task);
        dashboardMap.put("jmMemory", 1024);
        dashboardMap.put("tmMemory", 2048);
        dashboardMap.put("totalTM", 3);
        dashboardMap.put("availableSlot", 8);
        dashboardMap.put("totalSlot", 12);
        dashboardMap.put("runningJob", 2);

        FlinkAppDashboardResponse response = FlinkApplicationAssembler.toDashboardResponse(dashboardMap);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(5, response.getTask().getTotal());
        Assertions.assertEquals(1024, response.getJmMemory());
        Assertions.assertEquals(2, response.getRunningJob());
    }
}
