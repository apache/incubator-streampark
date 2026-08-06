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

import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.request.spark.SparkAppCancelRequest;
import org.apache.streampark.console.core.request.spark.SparkAppCheckNameRequest;
import org.apache.streampark.console.core.request.spark.SparkAppConfigRequest;
import org.apache.streampark.console.core.request.spark.SparkAppCopyRequest;
import org.apache.streampark.console.core.request.spark.SparkAppCreateRequest;
import org.apache.streampark.console.core.request.spark.SparkAppIdRequest;
import org.apache.streampark.console.core.request.spark.SparkAppListQueryRequest;
import org.apache.streampark.console.core.request.spark.SparkAppMappingRequest;
import org.apache.streampark.console.core.request.spark.SparkAppStartRequest;
import org.apache.streampark.console.core.request.spark.SparkAppUpdateRequest;
import org.apache.streampark.console.core.response.spark.SparkAppDashboardResponse;
import org.apache.streampark.console.core.response.spark.SparkAppResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * Converts between Spark application entities and API request/response contracts.
 */
public final class SparkApplicationAssembler {

    private SparkApplicationAssembler() {
    }

    public static SparkApplication toEntity(SparkAppCreateRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        BeanUtils.copyProperties(request, app);
        return app;
    }

    public static SparkApplication toEntity(SparkAppUpdateRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = toEntity((SparkAppCreateRequest) request);
        app.setId(request.getId());
        return app;
    }

    public static SparkApplication toEntity(SparkAppIdRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        return app;
    }

    public static SparkApplication toEntity(SparkAppStartRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        return app;
    }

    public static SparkApplication toEntity(SparkAppCancelRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        return app;
    }

    public static SparkApplication toEntity(SparkAppCopyRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setAppName(request.getAppName());
        app.setAppArgs(request.getAppArgs());
        return app;
    }

    public static SparkApplication toEntity(SparkAppMappingRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        app.setId(request.getId());
        app.setClusterId(request.getClusterId());
        return app;
    }

    public static SparkApplication toEntity(SparkAppListQueryRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        BeanUtils.copyProperties(request, app);
        return app;
    }

    public static SparkApplication toEntity(SparkAppCheckNameRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setAppName(request.getAppName());
        return app;
    }

    public static SparkApplication toEntity(SparkAppConfigRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        app.setId(request.getId());
        app.setTeamId(request.getTeamId());
        app.setConfig(request.getConfig());
        return app;
    }

    public static SparkApplication toCleanEntity(SparkAppIdRequest request) {
        if (request == null) {
            return null;
        }
        SparkApplication app = new SparkApplication();
        app.setId(request.getId());
        app.setRelease(ReleaseStateEnum.DONE.get());
        return app;
    }

    public static SparkAppResponse toResponse(SparkApplication app) {
        return DtoAssembler.toDto(app, SparkAppResponse.class);
    }

    public static IPage<SparkAppResponse> toPageResponse(IPage<SparkApplication> page) {
        return DtoAssembler.toPage(page, SparkApplicationAssembler::toResponse);
    }

    public static SparkAppDashboardResponse toDashboardResponse(Map<String, Serializable> dashboardMap) {
        if (dashboardMap == null) {
            return null;
        }
        SparkAppDashboardResponse response = new SparkAppDashboardResponse();
        response.setRunningApplication((Integer) dashboardMap.get("runningApplication"));
        response.setNumTasks((Long) dashboardMap.get("numTasks"));
        response.setNumCompletedTasks((Long) dashboardMap.get("numCompletedTasks"));
        response.setNumStages((Long) dashboardMap.get("numStages"));
        response.setNumCompletedStages((Long) dashboardMap.get("numCompletedStages"));
        response.setUsedMemory((Long) dashboardMap.get("usedMemory"));
        response.setUsedVCores((Long) dashboardMap.get("usedVCores"));
        return response;
    }
}
