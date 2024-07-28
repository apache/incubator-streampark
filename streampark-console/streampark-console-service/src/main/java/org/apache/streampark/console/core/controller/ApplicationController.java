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
import org.apache.streampark.console.base.domain.Result;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.annotation.AppUpdated;
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
import org.springframework.web.multipart.MultipartFile;

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
    public Result<Application> get(Application app) {
        Application application = applicationManageService.getApp(app.getId());
        return Result.success(application);
    }

    @Permission(team = "#app.teamId")
    @PostMapping("create")
    @RequiresPermissions("app:create")
    public Result<Boolean> create(Application app) throws IOException {
        return Result.success(applicationManageService.create(app));
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("copy")
    @RequiresPermissions("app:copy")
    public Result<Void> copy(Application app) throws IOException {
        applicationManageService.copy(app);
        return Result.success();
    }

    @AppUpdated
    @Permission(app = "#app.id")
    @PostMapping("update")
    @RequiresPermissions("app:update")
    public Result<Void> update(Application app) {
        applicationManageService.update(app);
        return Result.success();
    }

    @PostMapping("dashboard")
    @Permission(team = "#teamId")
    public Result<Map<String, Serializable>> dashboard(Long teamId) {
        Map<String, Serializable> dashboardMap = applicationInfoService.getDashboardDataMap(teamId);
        return Result.success(dashboardMap);
    }

    @PostMapping("list")
    @Permission(team = "#app.teamId")
    @RequiresPermissions("app:view")
    public Result<IPage<Application>> list(Application app, RestRequest request) {
        return Result.success(applicationManageService.page(app, request));
    }

    @AppUpdated
    @PostMapping("mapping")
    @Permission(app = "#app.id")
    @RequiresPermissions("app:mapping")
    public Result<Boolean> mapping(Application app) {
        boolean flag = applicationManageService.mapping(app);
        return Result.success(flag);
    }

    @AppUpdated
    @Permission(app = "#app.id")
    @PostMapping("revoke")
    @RequiresPermissions("app:release")
    public Result<Void> revoke(Application app) {
        applicationActionService.revoke(app.getId());
        return Result.success();
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("check/start")
    @RequiresPermissions("app:start")
    public Result<Integer> checkStart(Application app) {
        AppExistsStateEnum stateEnum = applicationInfoService.checkStart(app.getId());
        return Result.success(stateEnum.get());
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("start")
    @RequiresPermissions("app:start")
    public Result<Boolean> start(Application app) throws Exception {
        applicationActionService.start(app, false);
        return Result.success(true);
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public Result<Void> cancel(Application app) throws Exception {
        applicationActionService.cancel(app);
        return Result.success();
    }

    /** force stop(stop normal start or in progress) */
    @Permission(app = "#app.id")
    @PostMapping("abort")
    @RequiresPermissions("app:cancel")
    public Result<Void> abort(Application app) {
        applicationActionService.abort(app.getId());
        return Result.success();
    }

    @PostMapping("yarn")
    public Result<String> yarn() {
        return Result.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    @Permission(app = "#app.id", team = "#app.teamId")
    public Result<String> yarnName(Application app) {
        String yarnName = applicationInfoService.getYarnName(app.getConfig());
        return Result.success(yarnName);
    }

    @PostMapping("check/name")
    @Permission(app = "#app.id", team = "#app.teamId")
    public Result<Integer> checkName(Application app) {
        AppExistsStateEnum exists = applicationInfoService.checkExists(app);
        return Result.success(exists.get());
    }

    @PostMapping("read_conf")
    public Result<String> readConf(Application app) throws IOException {
        String config = applicationInfoService.readConf(app.getConfig());
        return Result.success(config);
    }

    @PostMapping("main")
    @Permission(app = "#app.id", team = "#app.teamId")
    public Result<String> getMain(Application application) {
        String mainClass = applicationInfoService.getMain(application);
        return Result.success(mainClass);
    }

    @PostMapping("backups")
    @Permission(app = "#backUp.appId", team = "#backUp.teamId")
    public Result<IPage<ApplicationBackUp>> backups(ApplicationBackUp backUp, RestRequest request) {
        IPage<ApplicationBackUp> backups = backUpService.getPage(backUp, request);
        return Result.success(backups);
    }

    @PostMapping("opt_log")
    @Permission(app = "#log.appId", team = "#log.teamId")
    public Result<IPage<ApplicationLog>> log(ApplicationLog applicationLog, RestRequest request) {
        IPage<ApplicationLog> applicationList = applicationLogService.getPage(applicationLog, request);
        return Result.success(applicationList);
    }

    @Permission(app = "#log.appId", team = "#log.teamId")
    @PostMapping("delete/opt_log")
    @RequiresPermissions("app:delete")
    public Result<Boolean> deleteLog(Long id) {
        Boolean deleted = applicationLogService.removeById(id);
        return Result.success(deleted);
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("delete")
    @RequiresPermissions("app:delete")
    public Result<Boolean> delete(Application app) throws InternalException {
        Boolean deleted = applicationManageService.remove(app.getId());
        return Result.success(deleted);
    }

    @Permission(app = "#backUp.appId")
    @PostMapping("delete/backup")
    public Result<Boolean> deleteBackup(ApplicationBackUp backUp) throws InternalException {
        Boolean deleted = backUpService.removeById(backUp.getId());
        return Result.success(deleted);
    }

    @PostMapping("check/jar")
    public Result<Boolean> checkjar(String jar) throws IOException {
        Utils.requireCheckJarFile(new File(jar).toURI().toURL());
        return Result.success(true);
    }

    @PostMapping("upload")
    @RequiresPermissions("app:create")
    public Result<String> upload(MultipartFile file) throws Exception {
        String uploadPath = resourceService.upload(file);
        return Result.success(uploadPath);
    }

    @PostMapping("verify_schema")
    public Result<Boolean> verifySchema(String path) {
        final URI uri = URI.create(path);
        final String scheme = uri.getScheme();
        final String pathPart = uri.getPath();
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
            return Result.success(false, error);
        }
        return Result.success(true);
    }

    @PostMapping("check/savepoint_path")
    @Permission(app = "#app.id", team = "#app.teamId")
    public Result<Boolean> checkSavepointPath(Application app) throws Exception {
        String error = applicationInfoService.checkSavepointPath(app);
        if (error == null) {
            return Result.success(true);
        }
        return Result.success(false, error);
    }

    @Permission(app = "#id")
    @PostMapping("k8s_log")
    public Result<String> k8sStartLog(Long id, Integer offset, Integer limit) throws Exception {
        String resp = applicationInfoService.k8sStartLog(id, offset, limit);
        return Result.success(resp);
    }
}
