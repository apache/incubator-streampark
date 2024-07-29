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
import org.apache.streampark.console.base.bean.PageRequest;
import org.apache.streampark.console.base.bean.Response;
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
    public Response<Application> get(Application app) {
        Application application = applicationManageService.getApp(app.getId());
        return Response.success(application);
    }

    @Permission(team = "#app.teamId")
    @PostMapping("create")
    @RequiresPermissions("app:create")
    public Response<Boolean> create(Application app) throws IOException {
        return Response.success(applicationManageService.create(app));
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("copy")
    @RequiresPermissions("app:copy")
    public Response<Void> copy(Application app) throws IOException {
        applicationManageService.copy(app);
        return Response.success();
    }

    @AppUpdated
    @Permission(app = "#app.id")
    @PostMapping("update")
    @RequiresPermissions("app:update")
    public Response<Void> update(Application app) {
        applicationManageService.update(app);
        return Response.success();
    }

    @PostMapping("dashboard")
    @Permission(team = "#teamId")
    public Response<?> dashboard(Long teamId) {
        Map<String, Object> dashboardMap = applicationInfoService.getDashboardDataMap(teamId);
        return Response.success(dashboardMap);
    }

    @PostMapping("list")
    @Permission(team = "#app.teamId")
    @RequiresPermissions("app:view")
    public Response<IPage<Application>> list(Application app, PageRequest request) {
        return Response.success(applicationManageService.page(app, request));
    }

    @AppUpdated
    @PostMapping("mapping")
    @Permission(app = "#app.id")
    @RequiresPermissions("app:mapping")
    public Response<Boolean> mapping(Application app) {
        boolean flag = applicationManageService.mapping(app);
        return Response.success(flag);
    }

    @AppUpdated
    @Permission(app = "#app.id")
    @PostMapping("revoke")
    @RequiresPermissions("app:release")
    public Response<Void> revoke(Application app) {
        applicationActionService.revoke(app.getId());
        return Response.success();
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("check/start")
    @RequiresPermissions("app:start")
    public Response<Integer> checkStart(Application app) {
        AppExistsStateEnum stateEnum = applicationInfoService.checkStart(app.getId());
        return Response.success(stateEnum.get());
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("start")
    @RequiresPermissions("app:start")
    public Response<Boolean> start(Application app) throws Exception {
        applicationActionService.start(app, false);
        return Response.success(true);
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public Response<Void> cancel(Application app) throws Exception {
        applicationActionService.cancel(app);
        return Response.success();
    }

    /** force stop(stop normal start or in progress) */
    @Permission(app = "#app.id")
    @PostMapping("abort")
    @RequiresPermissions("app:cancel")
    public Response<Void> abort(Application app) {
        applicationActionService.abort(app.getId());
        return Response.success();
    }

    @PostMapping("yarn")
    public Response<String> yarn() {
        return Response.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    @Permission(app = "#app.id", team = "#app.teamId")
    public Response<String> yarnName(Application app) {
        String yarnName = applicationInfoService.getYarnName(app.getConfig());
        return Response.success(yarnName);
    }

    @PostMapping("check/name")
    @Permission(app = "#app.id", team = "#app.teamId")
    public Response<Integer> checkName(Application app) {
        AppExistsStateEnum exists = applicationInfoService.checkExists(app);
        return Response.success(exists.get());
    }

    @PostMapping("read_conf")
    public Response<String> readConf(Application app) throws IOException {
        String config = applicationInfoService.readConf(app.getConfig());
        return Response.success(config);
    }

    @PostMapping("main")
    @Permission(app = "#app.id", team = "#app.teamId")
    public Response<String> getMain(Application application) {
        String mainClass = applicationInfoService.getMain(application);
        return Response.success(mainClass);
    }

    @PostMapping("backups")
    @Permission(app = "#backUp.appId", team = "#backUp.teamId")
    public Response<IPage<ApplicationBackUp>> backups(ApplicationBackUp backUp, PageRequest request) {
        IPage<ApplicationBackUp> backups = backUpService.getPage(backUp, request);
        return Response.success(backups);
    }

    @PostMapping("opt_log")
    @Permission(app = "#log.appId", team = "#log.teamId")
    public Response<IPage<ApplicationLog>> log(ApplicationLog applicationLog, PageRequest request) {
        IPage<ApplicationLog> applicationList = applicationLogService.getPage(applicationLog, request);
        return Response.success(applicationList);
    }

    @Permission(app = "#log.appId", team = "#log.teamId")
    @PostMapping("delete/opt_log")
    @RequiresPermissions("app:delete")
    public Response<Boolean> deleteLog(Long id) {
        Boolean deleted = applicationLogService.removeById(id);
        return Response.success(deleted);
    }

    @Permission(app = "#app.id", team = "#app.teamId")
    @PostMapping("delete")
    @RequiresPermissions("app:delete")
    public Response<Boolean> delete(Application app) throws InternalException {
        Boolean deleted = applicationManageService.remove(app.getId());
        return Response.success(deleted);
    }

    @Permission(app = "#backUp.appId")
    @PostMapping("delete/backup")
    public Response<Boolean> deleteBackup(ApplicationBackUp backUp) throws InternalException {
        Boolean deleted = backUpService.removeById(backUp.getId());
        return Response.success(deleted);
    }

    @PostMapping("check/jar")
    public Response<Boolean> checkjar(String jar) throws IOException {
        Utils.requireCheckJarFile(new File(jar).toURI().toURL());
        return Response.success(true);
    }

    @PostMapping("upload")
    @RequiresPermissions("app:create")
    public Response<String> upload(MultipartFile file) throws Exception {
        String uploadPath = resourceService.upload(file);
        return Response.success(uploadPath);
    }

    @PostMapping("verify_schema")
    public Response<Boolean> verifySchema(String path) {
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
            return Response.success(false, error);
        }
        return Response.success(true);
    }

    @PostMapping("check/savepoint_path")
    @Permission(app = "#app.id", team = "#app.teamId")
    public Response<Boolean> checkSavepointPath(Application app) throws Exception {
        String error = applicationInfoService.checkSavepointPath(app);
        if (error == null) {
            return Response.success(true);
        }
        return Response.success(false, error);
    }

    @Permission(app = "#id")
    @PostMapping("k8s_log")
    public Response<String> k8sStartLog(Long id, Integer offset, Integer limit) throws Exception {
        String resp = applicationInfoService.k8sStartLog(id, offset, limit);
        return Response.success(resp);
    }
}
