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

package org.apache.streampark.console.core.controller;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.core.annotation.AppChangeEvent;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.assembler.AppLogAssembler;
import org.apache.streampark.console.core.assembler.SparkApplicationAssembler;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;
import org.apache.streampark.console.core.request.app.AppBackupDeleteRequest;
import org.apache.streampark.console.core.request.app.AppBackupQueryRequest;
import org.apache.streampark.console.core.request.app.AppOptLogQueryRequest;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.common.TeamIdRequest;
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
import org.apache.streampark.console.core.response.app.AppBackupResponse;
import org.apache.streampark.console.core.response.app.AppOptLogResponse;
import org.apache.streampark.console.core.response.spark.SparkAppDashboardResponse;
import org.apache.streampark.console.core.response.spark.SparkAppResponse;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.application.ApplicationLogService;
import org.apache.streampark.console.core.service.application.FlinkApplicationBackupService;
import org.apache.streampark.console.core.service.application.SparkApplicationActionService;
import org.apache.streampark.console.core.service.application.SparkApplicationInfoService;
import org.apache.streampark.console.core.service.application.SparkApplicationManageService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("spark/app")
public class SparkApplicationController {

    @Autowired
    private SparkApplicationManageService applicationManageService;

    @Autowired
    private SparkApplicationActionService applicationActionService;

    @Autowired
    private SparkApplicationInfoService applicationInfoService;

    @Autowired
    private FlinkApplicationBackupService backUpService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private ResourceService resourceService;

    @PostMapping("get")
    @Permission(app = "#request.id")
    @RequiresPermissions("app:detail")
    public RestResponseBody<SparkAppResponse> get(@Valid SparkAppIdRequest request) {
        SparkApplication application = applicationManageService.getApp(request.getId());
        SparkAppResponse response = SparkApplicationAssembler.toResponse(application);
        return RestResponseBody.success(response);
    }

    @Permission(team = "#request.teamId")
    @PostMapping("create")
    @RequiresPermissions("app:create")
    public RestResponseBody<Boolean> create(@Valid @FormOrJson SparkAppCreateRequest request) throws IOException {
        SparkApplication app = SparkApplicationAssembler.toEntity(request);
        boolean saved = applicationManageService.create(app);
        return RestResponseBody.success(saved);
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("copy")
    @RequiresPermissions("app:copy")
    public RestResponseBody<Void> copy(@Valid @FormOrJson SparkAppCopyRequest request) throws IOException {
        applicationManageService.copy(SparkApplicationAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @AppChangeEvent
    @Permission(app = "#request.id")
    @PostMapping("update")
    @RequiresPermissions("app:update")
    public RestResponseBody<Boolean> update(@Valid @FormOrJson SparkAppUpdateRequest request) {
        applicationManageService.update(SparkApplicationAssembler.toEntity(request));
        return RestResponseBody.success(true);
    }

    @PostMapping("dashboard")
    @Permission(team = "#request.teamId")
    public RestResponseBody<SparkAppDashboardResponse> dashboard(@Valid TeamIdRequest request) {
        Map<String, Serializable> dashboardMap = applicationInfoService.getDashboardDataMap(request.getTeamId());
        return RestResponseBody.success(SparkApplicationAssembler.toDashboardResponse(dashboardMap));
    }

    @PostMapping("list")
    @Permission(team = "#query.teamId")
    @RequiresPermissions("app:view")
    public RestResponseBody<IPage<SparkAppResponse>> list(@Valid SparkAppListQueryRequest query, RestRequest request) {
        SparkApplication appParam = SparkApplicationAssembler.toEntity(query);
        IPage<SparkApplication> applicationList = applicationManageService.page(appParam, request);
        return RestResponseBody.success(SparkApplicationAssembler.toPageResponse(applicationList));
    }

    @AppChangeEvent
    @PostMapping("mapping")
    @Permission(app = "#request.id")
    @RequiresPermissions("app:mapping")
    public RestResponseBody<Boolean> mapping(@Valid @FormOrJson SparkAppMappingRequest request) {
        boolean flag = applicationManageService.mapping(SparkApplicationAssembler.toEntity(request));
        return RestResponseBody.success(flag);
    }

    @AppChangeEvent
    @Permission(app = "#request.id")
    @PostMapping("revoke")
    @RequiresPermissions("app:release")
    public RestResponseBody<Void> revoke(@Valid @FormOrJson SparkAppIdRequest request) {
        applicationActionService.revoke(request.getId());
        return RestResponseBody.success();
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("check/start")
    @RequiresPermissions("app:start")
    public RestResponseBody<Integer> checkStart(@Valid SparkAppIdRequest request) {
        AppExistsStateEnum stateEnum = applicationInfoService.checkStart(request.getId());
        return RestResponseBody.success(stateEnum.get());
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("start")
    @RequiresPermissions("app:start")
    public RestResponseBody<Boolean> start(@Valid @FormOrJson SparkAppStartRequest request) {
        try {
            applicationActionService.start(SparkApplicationAssembler.toEntity(request), false);
            return RestResponseBody.success(true);
        } catch (Exception e) {
            return RestResponseBody.success(false).message(e.getMessage());
        }
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public RestResponseBody<Void> cancel(@Valid @FormOrJson SparkAppCancelRequest request) throws Exception {
        applicationActionService.cancel(SparkApplicationAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @AppChangeEvent
    @Permission(app = "#request.id")
    @PostMapping("clean")
    @RequiresPermissions("app:clean")
    public RestResponseBody<Boolean> clean(@Valid @FormOrJson SparkAppIdRequest request) {
        applicationManageService.clean(SparkApplicationAssembler.toCleanEntity(request));
        return RestResponseBody.success(true);
    }

    @Permission(app = "#request.id")
    @PostMapping("forcedStop")
    @RequiresPermissions("app:cancel")
    public RestResponseBody<Void> forcedStop(@Valid @FormOrJson SparkAppIdRequest request) {
        applicationActionService.forcedStop(request.getId());
        return RestResponseBody.success();
    }

    @PostMapping("yarn")
    public RestResponseBody<String> yarn() {
        return RestResponseBody.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    @Permission(app = "#request.id", team = "#request.teamId")
    public RestResponseBody<String> yarnName(SparkAppConfigRequest request) {
        String yarnName = applicationInfoService.getYarnName(request.getConfig());
        return RestResponseBody.success(yarnName);
    }

    @PostMapping("check/name")
    @Permission(app = "#request.id", team = "#request.teamId")
    public RestResponseBody<Integer> checkName(@Valid SparkAppCheckNameRequest request) {
        AppExistsStateEnum exists = applicationInfoService.checkExists(SparkApplicationAssembler.toEntity(request));
        return RestResponseBody.success(exists.get());
    }

    @PostMapping("read_conf")
    public RestResponseBody<String> readConf(SparkAppConfigRequest request) throws IOException {
        String config = applicationInfoService.readConf(request.getConfig());
        return RestResponseBody.success(config);
    }

    @PostMapping("backups")
    @Permission(app = "#query.appId", team = "#query.teamId")
    public RestResponseBody<IPage<AppBackupResponse>> backups(AppBackupQueryRequest query, RestRequest request) {
        return RestResponseBody.success(
            AppLogAssembler.toBackupPage(backUpService.getPage(AppLogAssembler.toEntity(query), request)));
    }

    @PostMapping("opt_log")
    @Permission(app = "#query.appId", team = "#query.teamId")
    public RestResponseBody<IPage<AppOptLogResponse>> optionlog(AppOptLogQueryRequest query, RestRequest request) {
        return RestResponseBody.success(
            AppLogAssembler.toOptLogPage(applicationLogService.getPage(AppLogAssembler.toEntity(query), request)));
    }

    @PostMapping("delete/opt_log")
    @RequiresPermissions("app:delete")
    public RestResponseBody<Boolean> deleteOperationLog(@Valid @FormOrJson IdRequest request) {
        Boolean deleted = applicationLogService.removeById(request.getId());
        return RestResponseBody.success(deleted);
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("delete")
    @RequiresPermissions("app:delete")
    public RestResponseBody<Boolean> delete(@Valid @FormOrJson SparkAppIdRequest request) throws InternalException {
        Boolean deleted = applicationManageService.remove(request.getId());
        return RestResponseBody.success(deleted);
    }

    @Permission(app = "#request.appId")
    @PostMapping("delete/bak")
    public RestResponseBody<Boolean> deleteBak(@Valid @FormOrJson AppBackupDeleteRequest request) throws InternalException {
        Boolean deleted = backUpService.removeById(request.getId());
        return RestResponseBody.success(deleted);
    }

    @PostMapping("check/jar")
    public RestResponseBody<Boolean> checkjar(String jar) {
        File file = new File(jar);
        try {
            Utils.requireCheckJarFile(file.toURI().toURL());
            return RestResponseBody.success(true);
        } catch (IOException e) {
            return RestResponseBody.success(false).message(e.getLocalizedMessage());
        }
    }

    @PostMapping("verify_schema")
    public RestResponseBody<Boolean> verifySchema(String path) {
        final URI uri = URI.create(path);
        final String scheme = uri.getScheme();
        final String pathPart = uri.getPath();
        RestResponseBody<Boolean> restResponse = RestResponseBody.success(true);
        String error = null;
        if (scheme == null) {
            error =
                "The scheme (hdfs://, file://, etc) is null. Please specify the file system scheme explicitly in the URI.";
        } else if (pathPart == null) {
            error =
                "The path to store the checkpoint data in is null. Please specify a directory path for the checkpoint data.";
        } else if (pathPart.isEmpty() || "/".equals(pathPart)) {
            error = "Cannot use the root directory for checkpoints.";
        }
        if (error != null) {
            restResponse = RestResponseBody.success(false).message(error);
        }
        return restResponse;
    }
}
