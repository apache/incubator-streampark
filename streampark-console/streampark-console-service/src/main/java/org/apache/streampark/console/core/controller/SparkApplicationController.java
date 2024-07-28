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

import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.Result;
import org.apache.streampark.console.core.annotation.AppUpdated;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;
import org.apache.streampark.console.core.service.ResourceService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
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
    private ResourceService resourceService;

    @PostMapping("get")
    @RequiresPermissions("app:detail")
    public Result<SparkApplication> get(SparkApplication app) {
        SparkApplication application = applicationManageService.getApp(app.getId());
        return Result.success(application);
    }

    @PostMapping("create")
    @RequiresPermissions("app:create")
    public Result<Boolean> create(SparkApplication app) throws IOException {
        boolean saved = applicationManageService.create(app);
        return Result.success(saved);
    }

    @PostMapping("copy")
    @RequiresPermissions("app:copy")
    public Result<Void> copy(SparkApplication app) throws IOException {
        applicationManageService.copy(app);
        return Result.success();
    }

    @AppUpdated
    @PostMapping("update")
    @RequiresPermissions("app:update")
    public Result<Boolean> update(SparkApplication app) {
        applicationManageService.update(app);
        return Result.success(true);
    }

    @PostMapping("dashboard")
    public Result<Map<String, Serializable>> dashboard(Long teamId) {
        Map<String, Serializable> dashboardMap = applicationInfoService.getDashboardDataMap(teamId);
        return Result.success(dashboardMap);
    }

    @PostMapping("list")
    @RequiresPermissions("app:view")
    public Result<IPage<SparkApplication>> list(SparkApplication app, RestRequest request) {
        IPage<SparkApplication> applicationList = applicationManageService.page(app, request);
        return Result.success(applicationList);
    }

    @AppUpdated
    @PostMapping("mapping")
    @RequiresPermissions("app:mapping")
    public Result<Boolean> mapping(SparkApplication app) {
        boolean flag = applicationManageService.mapping(app);
        return Result.success(flag);
    }

    @AppUpdated
    @PostMapping("revoke")
    @RequiresPermissions("app:release")
    public Result<Void> revoke(SparkApplication app) {
        applicationActionService.revoke(app.getId());
        return Result.success();
    }

    @PostMapping("check/start")
    @RequiresPermissions("app:start")
    public Result<Integer> checkStart(SparkApplication app) {
        AppExistsStateEnum stateEnum = applicationInfoService.checkStart(app.getId());
        return Result.success(stateEnum.get());
    }

    @PostMapping("start")
    @RequiresPermissions("app:start")
    public Result<Boolean> start(SparkApplication app) {
        try {
            applicationActionService.start(app, false);
            return Result.success(true);
        } catch (Exception e) {
            return Result.success(false, e.getMessage());
        }
    }

    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public Result<Void> stop(SparkApplication app) throws Exception {
        applicationActionService.stop(app);
        return Result.success();
    }

    @AppUpdated
    @PostMapping("clean")
    @RequiresPermissions("app:clean")
    public Result<Boolean> clean(SparkApplication app) {
        applicationManageService.clean(app);
        return Result.success(true);
    }

    @PostMapping("forcedStop")
    @RequiresPermissions("app:cancel")
    public Result<Void> forcedStop(SparkApplication app) {
        applicationActionService.forcedStop(app.getId());
        return Result.success();
    }

    @PostMapping("yarn")
    public Result<String> yarn() {
        return Result.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    public Result<String> yarnName(SparkApplication app) {
        String yarnName = applicationInfoService.getYarnName(app.getConfig());
        return Result.success(yarnName);
    }

    @PostMapping("check/name")
    public Result<Integer> checkName(SparkApplication app) {
        AppExistsStateEnum exists = applicationInfoService.checkExists(app);
        return Result.success(exists.get());
    }

    @PostMapping("read_conf")
    public Result<String> readConf(SparkApplication app) throws IOException {
        String config = applicationInfoService.readConf(app.getConfig());
        return Result.success(config);
    }

    @PostMapping("main")
    public Result<String> getMain(SparkApplication application) {
        String mainClass = applicationInfoService.getMain(application);
        return Result.success(mainClass);
    }

    @PostMapping("upload")
    @RequiresPermissions("app:create")
    public Result<String> upload(MultipartFile file) throws Exception {
        String uploadPath = resourceService.upload(file);
        return Result.success(uploadPath);
    }

}
