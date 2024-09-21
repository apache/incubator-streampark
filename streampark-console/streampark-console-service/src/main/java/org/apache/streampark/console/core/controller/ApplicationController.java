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
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationBackUp;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.application.ApplicationActionService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;

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
public class ApplicationController {

    @Autowired
    private ApplicationManageService applicationManageService;

    @Autowired
    private ApplicationActionService applicationActionService;

    @Autowired
    private ApplicationInfoService applicationInfoService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private ResourceService resourceService;

    @PostMapping("get")
    @Permission(app = "#app.id")
    @RequiresPermissions("app:detail")
    public RestResponse get(Application app) {
        Application application = applicationManageService.getApp(app.getId());
        return RestResponse.success(application);
    }

    @Permission(team = "#app.teamId")
    @PostMapping("create")
    @RequiresPermissions("app:create")
    public RestResponse create(Application app) throws IOException {
        boolean saved = applicationManageService.create(app);
        return RestResponse.success(saved);
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("copy")
    @RequiresPermissions("app:copy")
    public RestResponse copy(Application app) throws IOException {
        applicationManageService.copy(app);
        return RestResponse.success();
    }

    @AppChangeEvent
    @Permission(app = "#app.id")
    @PostMapping("update")
    @RequiresPermissions("app:update")
    public RestResponse update(Application app) {
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
    public RestResponse list(Application app, RestRequest request) {
        IPage<Application> applicationList = applicationManageService.page(app, request);
        return RestResponse.success(applicationList);
    }

    @AppChangeEvent
    @PostMapping("mapping")
    @Permission(app = "#app.id")
    @RequiresPermissions("app:mapping")
    public RestResponse mapping(Application app) {
        boolean flag = applicationManageService.mapping(app);
        return RestResponse.success(flag);
    }

    @AppChangeEvent
    @Permission(app = "#app.id")
    @PostMapping("revoke")
    @RequiresPermissions("app:release")
    public RestResponse revoke(Application app) {
        applicationActionService.revoke(app.getId());
        return RestResponse.success();
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("check/start")
    @RequiresPermissions("app:start")
    public RestResponse checkStart(Application app) {
        AppExistsStateEnum stateEnum = applicationInfoService.checkStart(app.getId());
        return RestResponse.success(stateEnum.get());
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("start")
    @RequiresPermissions("app:start")
    public RestResponse start(Application app) throws Exception {
        applicationActionService.start(app, false);
        return RestResponse.success(true);
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public RestResponse cancel(Application app) throws Exception {
        applicationActionService.cancel(app);
        return RestResponse.success();
    }

    /** force stop(stop normal start or in progress) */
    @Permission(app = "#app.id")
    @PostMapping("abort")
    @RequiresPermissions("app:cancel")
    public RestResponse abort(Application app) {
        applicationActionService.abort(app.getId());
        return RestResponse.success();
    }

    @PostMapping("yarn")
    public RestResponse yarn() {
        return RestResponse.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    @Permission(app = "#app.id", team = "#app.teamId")
    public RestResponse yarnName(Application app) {
        String yarnName = applicationInfoService.getYarnName(app.getConfig());
        return RestResponse.success(yarnName);
    }

    @PostMapping("check/name")
    @Permission(app = "#app.id", team = "#app.teamId")
    public RestResponse checkName(Application app) {
        AppExistsStateEnum exists = applicationInfoService.checkExists(app);
        return RestResponse.success(exists.get());
    }

    @PostMapping("read_conf")
    public RestResponse readConf(Application app) throws IOException {
        String config = applicationInfoService.readConf(app.getConfig());
        return RestResponse.success(config);
    }

    @PostMapping("main")
    @Permission(app = "#app.id", team = "#app.teamId")
    public RestResponse getMain(Application app) {
        String mainClass = applicationInfoService.getMain(app);
        return RestResponse.success(mainClass);
    }

    @PostMapping("backups")
    @Permission(app = "#backUp.appId", team = "#backUp.teamId")
    public RestResponse backups(ApplicationBackUp backUp, RestRequest request) {
        IPage<ApplicationBackUp> backups = backUpService.getPage(backUp, request);
        return RestResponse.success(backups);
    }

    @PostMapping("opt_log")
    @Permission(app = "#applicationLog.appId")
    public RestResponse log(ApplicationLog applicationLog, RestRequest request) {
        IPage<ApplicationLog> applicationList = applicationLogService.getPage(applicationLog, request);
        return RestResponse.success(applicationList);
    }

    @Permission(app = "#appId")
    @PostMapping("delete/opt_log")
    @RequiresPermissions("app:delete")
    public RestResponse deleteLog(Long id, Long appId) {
        Boolean deleted = applicationLogService.removeById(id);
        return RestResponse.success(deleted);
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("delete")
    @RequiresPermissions("app:delete")
    public RestResponse delete(Application app) throws InternalException {
        Boolean deleted = applicationManageService.remove(app.getId());
        return RestResponse.success(deleted);
    }

    @Permission(app = "#backUp.appId")
    @PostMapping("delete/backup")
    public RestResponse deleteBackup(ApplicationBackUp backUp) throws InternalException {
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
    public RestResponse checkSavepointPath(Application app) throws Exception {
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
