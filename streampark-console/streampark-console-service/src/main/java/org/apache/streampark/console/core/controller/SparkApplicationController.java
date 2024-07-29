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
import org.apache.streampark.console.base.bean.PageRequest;
import org.apache.streampark.console.base.bean.Response;
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
    public Response<SparkApplication> get(SparkApplication app) {
        SparkApplication application = applicationManageService.getApp(app.getId());
        return Response.success(application);
    }

    @PostMapping("create")
    @RequiresPermissions("app:create")
    public Response<Boolean> create(SparkApplication app) throws IOException {
        boolean saved = applicationManageService.create(app);
        return Response.success(saved);
    }

    @PostMapping("copy")
    @RequiresPermissions("app:copy")
    public Response<Void> copy(SparkApplication app) throws IOException {
        applicationManageService.copy(app);
        return Response.success();
    }

    @AppUpdated
    @PostMapping("update")
    @RequiresPermissions("app:update")
    public Response<Boolean> update(SparkApplication app) {
        applicationManageService.update(app);
        return Response.success(true);
    }

    @PostMapping("dashboard")
    public Response<Map<String, Serializable>> dashboard(Long teamId) {
        Map<String, Serializable> dashboardMap = applicationInfoService.getDashboardDataMap(teamId);
        return Response.success(dashboardMap);
    }

    @PostMapping("list")
    @RequiresPermissions("app:view")
    public Response<IPage<SparkApplication>> list(SparkApplication app, PageRequest request) {
        IPage<SparkApplication> applicationList = applicationManageService.page(app, request);
        return Response.success(applicationList);
    }

    @AppUpdated
    @PostMapping("mapping")
    @RequiresPermissions("app:mapping")
    public Response<Boolean> mapping(SparkApplication app) {
        boolean flag = applicationManageService.mapping(app);
        return Response.success(flag);
    }

    @AppUpdated
    @PostMapping("revoke")
    @RequiresPermissions("app:release")
    public Response<Void> revoke(SparkApplication app) {
        applicationActionService.revoke(app.getId());
        return Response.success();
    }

    @PostMapping("check/start")
    @RequiresPermissions("app:start")
    public Response<Integer> checkStart(SparkApplication app) {
        AppExistsStateEnum stateEnum = applicationInfoService.checkStart(app.getId());
        return Response.success(stateEnum.get());
    }

    @PostMapping("start")
    @RequiresPermissions("app:start")
    public Response<Boolean> start(SparkApplication app) {
        try {
            applicationActionService.start(app, false);
            return Response.success(true);
        } catch (Exception e) {
            return Response.success(false, e.getMessage());
        }
    }

    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public Response<Void> stop(SparkApplication app) throws Exception {
        applicationActionService.stop(app);
        return Response.success();
    }

    @AppUpdated
    @PostMapping("clean")
    @RequiresPermissions("app:clean")
    public Response<Boolean> clean(SparkApplication app) {
        applicationManageService.clean(app);
        return Response.success(true);
    }

    @PostMapping("forcedStop")
    @RequiresPermissions("app:cancel")
    public Response<Void> forcedStop(SparkApplication app) {
        applicationActionService.forcedStop(app.getId());
        return Response.success();
    }

    @PostMapping("yarn")
    public Response<String> yarn() {
        return Response.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    public Response<String> yarnName(SparkApplication app) {
        String yarnName = applicationInfoService.getYarnName(app.getConfig());
        return Response.success(yarnName);
    }

    @PostMapping("check/name")
    public Response<Integer> checkName(SparkApplication app) {
        AppExistsStateEnum exists = applicationInfoService.checkExists(app);
        return Response.success(exists.get());
    }

    @PostMapping("read_conf")
    public Response<String> readConf(SparkApplication app) throws IOException {
        String config = applicationInfoService.readConf(app.getConfig());
        return Response.success(config);
    }

    @PostMapping("main")
    public Response<String> getMain(SparkApplication application) {
        String mainClass = applicationInfoService.getMain(application);
        return Response.success(mainClass);
    }

    @PostMapping("upload")
    @RequiresPermissions("app:create")
    public Response<String> upload(MultipartFile file) throws Exception {
        String uploadPath = resourceService.upload(file);
        return Response.success(uploadPath);
    }

}
