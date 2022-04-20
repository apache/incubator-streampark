/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.controller;

import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.base.domain.ApiDocConstant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.core.annotation.ApiAccess;
import com.streamxhub.streamx.console.core.entity.AppControl;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.ApplicationBackUp;
import com.streamxhub.streamx.console.core.entity.ApplicationLog;
import com.streamxhub.streamx.console.core.enums.AppExistsState;
import com.streamxhub.streamx.console.core.service.AppBuildPipeService;
import com.streamxhub.streamx.console.core.service.ApplicationBackUpService;
import com.streamxhub.streamx.console.core.service.ApplicationLogService;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.flink.packer.pipeline.PipelineStatus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("flink/app")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private AppBuildPipeService appBuildPipeService;

    @ApiAccess
    @PostMapping("get")
    @RequiresPermissions("app:detail")
    public RestResponse get(Application app) {
        Application application = applicationService.getApp(app);
        return RestResponse.create().data(application);
    }

    @ApiAccess
    @PostMapping("create")
    @RequiresPermissions("app:create")
    public RestResponse create(Application app) throws IOException {
        boolean saved = applicationService.create(app);
        return RestResponse.create().data(saved);
    }

    @PostMapping("update")
    @RequiresPermissions("app:update")
    public RestResponse update(Application app) {
        applicationService.update(app);
        return RestResponse.create().data(true);
    }

    @PostMapping("dashboard")
    public RestResponse dashboard() {
        Map<String, Serializable> map = applicationService.dashboard();
        return RestResponse.create().data(map);
    }

    @ApiAccess
    @PostMapping("list")
    @RequiresPermissions("app:view")
    public RestResponse list(Application app, RestRequest request) {
        IPage<Application> applicationList = applicationService.page(app, request);

        List<Application> appRecords = applicationList.getRecords();
        List<Long> appIds = appRecords.stream().map(Application::getId).collect(Collectors.toList());
        Map<Long, PipelineStatus> pipeStates = appBuildPipeService.listPipelineStatus(appIds);

        // add building pipeline status info and app control info
        appRecords = appRecords.stream().peek(e -> {
            if (pipeStates.containsKey(e.getId())) {
                e.setBuildStatus(pipeStates.get(e.getId()).getCode());
            }
        }).peek(e -> e.setAppControl(new AppControl().setAllowBuild(e.getBuildStatus() == null || !PipelineStatus.running.getCode().equals(e.getBuildStatus())).setAllowStart(PipelineStatus.success.getCode().equals(e.getBuildStatus()) && !e.shouldBeTrack()).setAllowStop(e.isRunning()))).collect(Collectors.toList());

        applicationList.setRecords(appRecords);
        return RestResponse.create().data(applicationList);
    }

    @PostMapping("mapping")
    @RequiresPermissions("app:mapping")
    public RestResponse mapping(Application app) {
        boolean flag = applicationService.mapping(app);
        return RestResponse.create().data(flag);
    }

    @PostMapping("revoke")
    @RequiresPermissions("app:launch")
    public RestResponse revoke(Application app) throws Exception {
        applicationService.revoke(app);
        return RestResponse.create();
    }

    @ApiAccess
    @ApiOperation(value = "App Start", notes = "App Start", tags = ApiDocConstant.FLINK_APP_OP_TAG, consumes = "x-www-form-urlencoded")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", value = "app Id", required = true, paramType = "form", dataType = "Long"),
        @ApiImplicitParam(name = "savePointed", value = "从savepoint或最新checkpoint恢复app", required = true, paramType = "form", dataType = "Boolean", defaultValue = "false"),
        @ApiImplicitParam(name = "savePoint", value = "手动填写savepoint或最新checkpoint", required = true, paramType = "form", dataType = "String", defaultValue = ""),
        @ApiImplicitParam(name = "flameGraph", value = "flame Graph support", required = true, paramType = "form", dataType = "Boolean", defaultValue = "false"),
        @ApiImplicitParam(name = "allowNonRestored", value = "ignore savepoint then cannot be restored", required = true, paramType = "form", dataType = "Boolean", defaultValue = "false")})
    @PostMapping("start")
    @RequiresPermissions("app:start")
    public RestResponse start(@ApiIgnore Application app) {
        try {
            applicationService.checkEnv(app);
            applicationService.starting(app);
            applicationService.start(app, false);
            return RestResponse.create().data(true);
        } catch (Exception e) {
            return RestResponse.create().data(false).message(e.getMessage());
        }
    }

    @ApiAccess
    @PostMapping("clean")
    @RequiresPermissions("app:clean")
    public RestResponse clean(Application app) {
        applicationService.clean(app);
        return RestResponse.create().data(true);
    }

    @ApiAccess
    @ApiOperation(value = "App Cancel", notes = "App Cancel", tags = ApiDocConstant.FLINK_APP_OP_TAG, consumes = "x-www-form-urlencoded")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", value = "app Id", required = true, paramType = "form", dataType = "Long"),
        @ApiImplicitParam(name = "savePointed", value = "trigger savePoint before taking stoping", required = true, paramType = "form", dataType = "Boolean", defaultValue = "false"),
        @ApiImplicitParam(name = "savePoint", value = "savepoint path", required = false, paramType = "form", dataType = "String", defaultValue = "hdfs:///tm/xxx"),
        @ApiImplicitParam(name = "drain", value = "取消前发送最大 watermark", required = true, paramType = "form", dataType = "Boolean", defaultValue = "false")})
    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public RestResponse cancel(@ApiIgnore Application app) {
        applicationService.cancel(app);
        return RestResponse.create();
    }

    @PostMapping("yarn")
    public RestResponse yarn() {
        return RestResponse.create().data(HadoopUtils.getRMWebAppURL(false));
    }

    @PostMapping("name")
    public RestResponse yarnName(Application app) {
        String yarnName = applicationService.getYarnName(app);
        return RestResponse.create().data(yarnName);
    }

    @PostMapping("checkName")
    public RestResponse checkName(Application app) {
        AppExistsState exists = applicationService.checkExists(app);
        return RestResponse.create().data(exists.get());
    }

    @PostMapping("readConf")
    public RestResponse readConf(Application app) throws IOException {
        String config = applicationService.readConf(app);
        return RestResponse.create().data(config);
    }

    @PostMapping("main")
    public RestResponse getMain(Application application) {
        String mainClass = applicationService.getMain(application);
        return RestResponse.create().data(mainClass);
    }

    @PostMapping("backups")
    public RestResponse backups(ApplicationBackUp backUp, RestRequest request) {
        IPage<ApplicationBackUp> backups = backUpService.page(backUp, request);
        return RestResponse.create().data(backups);
    }

    @PostMapping("rollback")
    public RestResponse rollback(ApplicationBackUp backUp) {
        //TODO: next version implementation
        //backUpService.rollback(backUp);
        return RestResponse.create();
    }

    @PostMapping("optionlog")
    public RestResponse optionlog(ApplicationLog applicationLog, RestRequest request) {
        IPage<ApplicationLog> applicationList = applicationLogService.page(applicationLog, request);
        return RestResponse.create().data(applicationList);
    }

    @PostMapping("delete")
    public RestResponse delete(Application app) throws ServiceException {
        Boolean deleted = applicationService.delete(app);
        return RestResponse.create().data(deleted);
    }

    @PostMapping("deletebak")
    public RestResponse deleteBak(ApplicationBackUp backUp) throws ServiceException {
        Boolean deleted = backUpService.delete(backUp.getId());
        return RestResponse.create().data(deleted);
    }

    @PostMapping("checkjar")
    public RestResponse checkjar(String jar) {
        File file = new File(jar);
        try {
            Utils.checkJarFile(file.toURI().toURL());
            return RestResponse.create().data(true);
        } catch (IOException e) {
            return RestResponse.create().data(file).message(e.getLocalizedMessage());
        }
    }

    @PostMapping("upload")
    @RequiresPermissions("app:create")
    public RestResponse upload(MultipartFile file) throws Exception {
        String uploadPath = applicationService.upload(file);
        return RestResponse.create().data(uploadPath);
    }

    @PostMapping("downlog")
    public RestResponse downlog(Long id) {
        applicationService.tailMvnDownloading(id);
        return RestResponse.create();
    }

    @PostMapping("verifySchema")
    public RestResponse verifySchema(String path) {
        final URI uri = URI.create(path);
        final String scheme = uri.getScheme();
        final String pathPart = uri.getPath();
        RestResponse restResponse = RestResponse.create().data(true);
        String error;
        if (scheme == null) {
            error = "The scheme (hdfs://, file://, etc) is null. Please specify the file system scheme explicitly in the URI.";
            restResponse.data(false).message(error);
        }
        if (pathPart == null) {
            error = "The path to store the checkpoint data in is null. Please specify a directory path for the checkpoint data.";
            restResponse.data(false).message(error);
        }
        if (pathPart.length() == 0 || pathPart.equals("/")) {
            error = "Cannot use the root directory for checkpoints.";
            restResponse.data(false).message(error);
        }
        return restResponse;
    }

}
