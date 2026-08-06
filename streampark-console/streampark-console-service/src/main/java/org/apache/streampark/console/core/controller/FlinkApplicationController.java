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
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.annotation.AppChangeEvent;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.assembler.FlinkApplicationAssembler;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkApplicationBackup;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;
import org.apache.streampark.console.core.request.flink.FlinkAppCancelRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppCheckNameRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppCheckSavepointPathRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppConfigRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppCopyRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppCreateRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppGetMainRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppIdRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppK8sLogRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppListQueryRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppMappingRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppStartRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppUpdateRequest;
import org.apache.streampark.console.core.response.flink.FlinkAppResponse;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.application.ApplicationLogService;
import org.apache.streampark.console.core.service.application.FlinkApplicationActionService;
import org.apache.streampark.console.core.service.application.FlinkApplicationBackupService;
import org.apache.streampark.console.core.service.application.FlinkApplicationInfoService;
import org.apache.streampark.console.core.service.application.FlinkApplicationManageService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/app")
public class FlinkApplicationController {

    @Autowired
    private FlinkApplicationManageService applicationManageService;

    @Autowired
    private FlinkApplicationActionService applicationActionService;

    @Autowired
    private FlinkApplicationInfoService applicationInfoService;

    @Autowired
    private FlinkApplicationBackupService backUpService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private ResourceService resourceService;

    @PostMapping("get")
    @Permission(app = "#request.id")
    @RequiresPermissions("app:detail")
    public RestResponse get(FlinkAppIdRequest request) {
        FlinkApplication application = applicationManageService.getApp(request.getId());
        FlinkAppResponse response = FlinkApplicationAssembler.toResponse(application);
        return RestResponse.success(response);
    }

    @Permission(team = "#request.teamId")
    @PostMapping("create")
    @RequiresPermissions("app:create")
    public RestResponse create(FlinkAppCreateRequest request) throws IOException {
        FlinkApplication app = FlinkApplicationAssembler.toEntity(request);
        boolean saved = applicationManageService.create(app);
        return RestResponse.success(saved);
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("copy")
    @RequiresPermissions("app:copy")
    public RestResponse copy(FlinkAppCopyRequest request) throws IOException {
        applicationManageService.copy(FlinkApplicationAssembler.toEntity(request));
        return RestResponse.success();
    }

    @AppChangeEvent
    @Permission(app = "#request.id")
    @PostMapping("update")
    @RequiresPermissions("app:update")
    public RestResponse update(FlinkAppUpdateRequest request) {
        applicationManageService.update(FlinkApplicationAssembler.toEntity(request));
        return RestResponse.success(true);
    }

    @PostMapping("dashboard")
    @Permission(team = "#request.teamId")
    public RestResponse dashboard(FlinkAppIdRequest request) {
        Map<String, Serializable> dashboardMap = applicationInfoService.getDashboardDataMap(request.getTeamId());
        return RestResponse.success(FlinkApplicationAssembler.toDashboardResponse(dashboardMap));
    }

    @PostMapping("list")
    @Permission(team = "#query.teamId")
    @RequiresPermissions("app:view")
    public RestResponse list(FlinkAppListQueryRequest query, RestRequest request) {
        FlinkApplication appParam = FlinkApplicationAssembler.toEntity(query);
        IPage<FlinkApplication> applicationList = applicationManageService.page(appParam, request);
        return RestResponse.success(FlinkApplicationAssembler.toPageResponse(applicationList));
    }

    @AppChangeEvent
    @PostMapping("mapping")
    @Permission(app = "#request.id")
    @RequiresPermissions("app:mapping")
    public RestResponse mapping(FlinkAppMappingRequest request) {
        boolean flag = applicationManageService.mapping(FlinkApplicationAssembler.toEntity(request));
        return RestResponse.success(flag);
    }

    @AppChangeEvent
    @Permission(app = "#request.id")
    @PostMapping("revoke")
    @RequiresPermissions("app:release")
    public RestResponse revoke(FlinkAppIdRequest request) {
        applicationActionService.revoke(request.getId());
        return RestResponse.success();
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("check/start")
    @RequiresPermissions("app:start")
    public RestResponse checkStart(FlinkAppIdRequest request) {
        AppExistsStateEnum stateEnum = applicationInfoService.checkStart(request.getId());
        return RestResponse.success(stateEnum.get());
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("start")
    @RequiresPermissions("app:start")
    public RestResponse start(FlinkAppStartRequest request) throws Exception {
        applicationActionService.start(FlinkApplicationAssembler.toEntity(request), false);
        return RestResponse.success(true);
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public RestResponse cancel(FlinkAppCancelRequest request) throws Exception {
        applicationActionService.cancel(FlinkApplicationAssembler.toEntity(request));
        return RestResponse.success();
    }

    /** force stop(stop normal start or in progress) */
    @Permission(app = "#request.id")
    @PostMapping("abort")
    @RequiresPermissions("app:cancel")
    public RestResponse abort(FlinkAppIdRequest request) {
        applicationActionService.abort(request.getId());
        return RestResponse.success();
    }

    @PostMapping("yarn")
    public RestResponse yarn() {
        return RestResponse.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    @Permission(app = "#request.id", team = "#request.teamId")
    public RestResponse yarnName(FlinkAppConfigRequest request) {
        String yarnName = applicationInfoService.getYarnName(request.getConfig());
        return RestResponse.success(yarnName);
    }

    @PostMapping("check/name")
    @Permission(app = "#request.id", team = "#request.teamId")
    public RestResponse checkName(FlinkAppCheckNameRequest request) {
        AppExistsStateEnum exists = applicationInfoService.checkExists(FlinkApplicationAssembler.toEntity(request));
        return RestResponse.success(exists.get());
    }

    @PostMapping("read_conf")
    public RestResponse readConf(FlinkAppConfigRequest request) throws IOException {
        String config = applicationInfoService.readConf(request.getConfig());
        return RestResponse.success(config);
    }

    @PostMapping("main")
    @Permission(app = "#request.id", team = "#request.teamId")
    public RestResponse getMain(FlinkAppGetMainRequest request) {
        String mainClass = applicationInfoService.getMain(FlinkApplicationAssembler.toEntity(request));
        return RestResponse.success(mainClass);
    }

    @PostMapping("backups")
    @Permission(app = "#backUp.appId", team = "#backUp.teamId")
    public RestResponse backups(FlinkApplicationBackup backUp, RestRequest request) {
        IPage<FlinkApplicationBackup> backups = backUpService.getPage(backUp, request);
        return RestResponse.success(backups);
    }

    @PostMapping("opt_log")
    @Permission(app = "#applicationLog.appId", team = "#applicationLog.teamId")
    public RestResponse log(ApplicationLog applicationLog, RestRequest request) {
        IPage<ApplicationLog> applicationList = applicationLogService.getPage(applicationLog, request);
        return RestResponse.success(applicationList);
    }

    @Permission(app = "#applicationLog.appId", team = "#applicationLog.teamId")
    @PostMapping("delete/opt_log")
    @RequiresPermissions("app:delete")
    public RestResponse deleteLog(ApplicationLog applicationLog) {
        Boolean deleted = applicationLogService.delete(applicationLog);
        return RestResponse.success(deleted);
    }

    @Permission(app = "#request.id", team = "#request.teamId")
    @PostMapping("delete")
    @RequiresPermissions("app:delete")
    public RestResponse delete(FlinkAppIdRequest request) throws InternalException {
        Boolean deleted = applicationManageService.remove(request.getId());
        return RestResponse.success(deleted);
    }

    @Permission(app = "#backUp.appId")
    @PostMapping("delete/backup")
    public RestResponse deleteBackup(FlinkApplicationBackup backUp) throws InternalException {
        Boolean deleted = backUpService.removeById(backUp.getId());
        return RestResponse.success(deleted);
    }

    @PostMapping("check/jar")
    public RestResponse checkJar(String jar) throws IOException {
        Utils.requireCheckJarFile(new File(jar).toURI().toURL());
        return RestResponse.success(true);
    }

    @PostMapping("verify_schema")
    public RestResponse verifySchema(String path) {
        final URI uri = URI.create(path);
        final String scheme = uri.getScheme();
        final String pathPart = uri.getPath();
        RestResponse restResponse = RestResponse.success(true);
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
            restResponse = RestResponse.success(false).message(error);
        }
        return restResponse;
    }

    @PostMapping("check/savepoint_path")
    @Permission(app = "#request.id", team = "#request.teamId")
    public RestResponse checkSavepointPath(FlinkAppCheckSavepointPathRequest request) throws Exception {
        String error = applicationInfoService.checkSavepointPath(FlinkApplicationAssembler.toEntity(request));
        if (error == null) {
            return RestResponse.success(true);
        }
        return RestResponse.success(false).message(error);
    }

    @Permission(app = "#request.id")
    @PostMapping("k8s_log")
    public RestResponse k8sStartLog(FlinkAppK8sLogRequest request) throws Exception {
        String resp = applicationInfoService.k8sStartLog(request.getId(), request.getOffset(), request.getLimit());
        return RestResponse.success(resp);
    }
}
