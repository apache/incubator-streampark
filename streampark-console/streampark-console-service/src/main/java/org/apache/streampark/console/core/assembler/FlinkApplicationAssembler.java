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
import org.apache.streampark.console.core.request.flink.FlinkAppCancelRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppCheckNameRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppCheckSavepointPathRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppConfigRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppCopyRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppCreateRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppGetMainRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppIdRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppListQueryRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppMappingRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppStartRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppUpdateRequest;
import org.apache.streampark.console.core.response.flink.FlinkAppDashboardResponse;
import org.apache.streampark.console.core.response.flink.FlinkAppResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts between Flink application entities and API request/response contracts.
 */
public final class FlinkApplicationAssembler {

    private FlinkApplicationAssembler() {
    }

    public static FlinkApplication toEntity(FlinkAppCreateRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        BeanUtils.copyProperties(request, app);
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppUpdateRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = toEntity((FlinkAppCreateRequest) request);
        app.setId(request.getId());
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppIdRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppStartRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setRestoreOrTriggerSavepoint(request.getRestoreOrTriggerSavepoint());
        app.setSavepointPath(request.getSavepointPath());
        app.setAllowNonRestored(request.getAllowNonRestored());
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppCancelRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setRestoreOrTriggerSavepoint(request.getRestoreOrTriggerSavepoint());
        app.setDrain(request.getDrain());
        app.setNativeFormat(request.getNativeFormat());
        app.setSavepointPath(request.getSavepointPath());
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppCopyRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setJobName(request.getJobName());
        app.setArgs(request.getArgs());
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppMappingRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        app.setId(request.getId());
        app.setClusterId(request.getClusterId());
        app.setJobId(request.getJobId());
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppListQueryRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        BeanUtils.copyProperties(request, app);
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppCheckNameRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setJobName(request.getJobName());
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppConfigRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setConfig(request.getConfig());
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppCheckSavepointPathRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setSavepointPath(request.getSavepointPath());
        return app;
    }

    public static FlinkApplication toEntity(FlinkAppGetMainRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplication app = new FlinkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setProjectId(request.getProjectId());
        app.setJar(request.getJar());
        app.setModule(request.getModule());
        return app;
    }

    public static FlinkAppResponse toResponse(FlinkApplication app) {
        if (app == null) {
            return null;
        }
        FlinkAppResponse response = new FlinkAppResponse();
        BeanUtils.copyProperties(app, response);
        return response;
    }

    public static IPage<FlinkAppResponse> toPageResponse(IPage<FlinkApplication> page) {
        if (page == null) {
            return null;
        }
        Page<FlinkAppResponse> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<FlinkAppResponse> records =
            page.getRecords().stream().map(FlinkApplicationAssembler::toResponse).collect(Collectors.toList());
        result.setRecords(records);
        return result;
    }

    public static FlinkAppDashboardResponse toDashboardResponse(Map<String, Serializable> dashboardMap) {
        if (dashboardMap == null) {
            return null;
        }
        FlinkAppDashboardResponse response = new FlinkAppDashboardResponse();
        response.setTask((JobsOverview.Task) dashboardMap.get("task"));
        response.setJmMemory((Integer) dashboardMap.get("jmMemory"));
        response.setTmMemory((Integer) dashboardMap.get("tmMemory"));
        response.setTotalTM((Integer) dashboardMap.get("totalTM"));
        response.setAvailableSlot((Integer) dashboardMap.get("availableSlot"));
        response.setTotalSlot((Integer) dashboardMap.get("totalSlot"));
        response.setRunningJob((Integer) dashboardMap.get("runningJob"));
        return response;
    }
}
