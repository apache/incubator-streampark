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
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkApplicationBackUp;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.application.ApplicationLogService;
import org.apache.streampark.console.core.service.application.FlinkApplicationActionService;
import org.apache.streampark.console.core.service.application.FlinkApplicationBackUpService;
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
    private FlinkApplicationBackUpService backUpService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private ResourceService resourceService;

    @PostMapping("get")
    @Permission(app = "#app.id")
    @RequiresPermissions("app:detail")
    public RestResponse get(FlinkApplication app) {
        FlinkApplication application = applicationManageService.getApp(app.getId());
        return RestResponse.success(application);
    }

    @Permission(team = "#app.teamId")
    @PostMapping("create")
    @RequiresPermissions("app:create")
    public RestResponse create(FlinkApplication app) throws IOException {
        boolean saved = applicationManageService.create(app);
        return RestResponse.success(saved);
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("copy")
    @RequiresPermissions("app:copy")
    public RestResponse copy(FlinkApplication app) throws IOException {
        applicationManageService.copy(app);
        return RestResponse.success();
    }

    @AppChangeEvent
    @Permission(app = "#app.id")
    @PostMapping("update")
    @RequiresPermissions("app:update")
    public RestResponse update(FlinkApplication app) {
        applicationManageService.update(app);
        return RestResponse.success(true);
    }

    @PostMapping("dashboard")
    @Permission(team = "#teamId")
    public RestResponse dashboard(Long teamId) {
        Map<String, Serializable> dashboardMap = applicationInfoService.getDashboardDataMap(teamId);
        return RestResponse.success(dashboardMap);
    }

    @PostMapping("list")
    @Permission(team = "#app.teamId")
    @RequiresPermissions("app:view")
    public RestResponse list(FlinkApplication app, RestRequest request) {
        IPage<FlinkApplication> applicationList = applicationManageService.page(app, request);
        return RestResponse.success(applicationList);
    }

    @AppChangeEvent
    @PostMapping("mapping")
    @Permission(app = "#app.id")
    @RequiresPermissions("app:mapping")
    public RestResponse mapping(FlinkApplication app) {
        boolean flag = applicationManageService.mapping(app);
        return RestResponse.success(flag);
    }

    @AppChangeEvent
    @Permission(app = "#app.id")
    @PostMapping("revoke")
    @RequiresPermissions("app:release")
    public RestResponse revoke(FlinkApplication app) {
        applicationActionService.revoke(app.getId());
        return RestResponse.success();
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("check/start")
    @RequiresPermissions("app:start")
    public RestResponse checkStart(FlinkApplication app) {
        AppExistsStateEnum stateEnum = applicationInfoService.checkStart(app.getId());
        return RestResponse.success(stateEnum.get());
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("start")
    @RequiresPermissions("app:start")
    public RestResponse start(FlinkApplication app) throws Exception {
        applicationActionService.start(app, false);
        return RestResponse.success(true);
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public RestResponse cancel(FlinkApplication app) throws Exception {
        applicationActionService.cancel(app);
        return RestResponse.success();
    }

    /** force stop(stop normal start or in progress) */
    @Permission(app = "#app.id")
    @PostMapping("abort")
    @RequiresPermissions("app:cancel")
    public RestResponse abort(FlinkApplication app) {
        applicationActionService.abort(app.getId());
        return RestResponse.success();
    }

    @PostMapping("yarn")
    public RestResponse yarn() {
        return RestResponse.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    @Permission(app = "#app.id", team = "#app.teamId")
    public RestResponse yarnName(FlinkApplication app) {
        String yarnName = applicationInfoService.getYarnName(app.getConfig());
        return RestResponse.success(yarnName);
    }

    @PostMapping("check/name")
    @Permission(app = "#app.id", team = "#app.teamId")
    public RestResponse checkName(FlinkApplication app) {
        AppExistsStateEnum exists = applicationInfoService.checkExists(app);
        return RestResponse.success(exists.get());
    }

    @PostMapping("read_conf")
    public RestResponse readConf(FlinkApplication app) throws IOException {
        String config = applicationInfoService.readConf(app.getConfig());
        return RestResponse.success(config);
    }

    @PostMapping("main")
    @Permission(app = "#app.id", team = "#app.teamId")
    public RestResponse getMain(FlinkApplication application) {
        String mainClass = applicationInfoService.getMain(application);
        return RestResponse.success(mainClass);
    }

    @PostMapping("backups")
    @Permission(app = "#backUp.appId", team = "#backUp.teamId")
    public RestResponse backups(FlinkApplicationBackUp backUp, RestRequest request) {
        IPage<FlinkApplicationBackUp> backups = backUpService.getPage(backUp, request);
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

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("delete")
    @RequiresPermissions("app:delete")
    public RestResponse delete(FlinkApplication app) throws InternalException {
        Boolean deleted = applicationManageService.remove(app.getId());
        return RestResponse.success(deleted);
    }

    @Permission(app = "#backUp.appId")
    @PostMapping("delete/backup")
    public RestResponse deleteBackup(FlinkApplicationBackUp backUp) throws InternalException {
        Boolean deleted = backUpService.removeById(backUp.getId());
        return RestResponse.success(deleted);
    }

    @PostMapping("check/jar")
    public RestResponse checkjar(String jar) throws IOException {
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
    @Permission(app = "#app.id", team = "#app.teamId")
    public RestResponse checkSavepointPath(FlinkApplication app) throws Exception {
        String error = applicationInfoService.checkSavepointPath(app);
        if (error == null) {
            return RestResponse.success(true);
        }
        return RestResponse.success(false).message(error);
    }

    @Permission(app = "#id")
    @PostMapping("k8s_log")
    public RestResponse k8sStartLog(Long id, Integer offset, Integer limit) throws Exception {
        String resp = applicationInfoService.k8sStartLog(id, offset, limit);
        return RestResponse.success(resp);
    }
}
