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

import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.common.util.YarnUtils;
import com.streamxhub.streamx.console.base.domain.ApiDocConstant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.ApplicationException;
import com.streamxhub.streamx.console.base.exception.InternalException;
import com.streamxhub.streamx.console.base.util.MoreFutures;
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
import com.streamxhub.streamx.console.core.service.impl.LoggerServiceImpl;
import com.streamxhub.streamx.flink.packer.pipeline.PipelineStatus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Autowired
    private LoggerServiceImpl loggerService;
    @ApiAccess
    @PostMapping("get")
    @RequiresPermissions("app:detail")
    public RestResponse get(Application app) {
        Application application = applicationService.getApp(app);
        return RestResponse.success(application);
    }

    @ApiAccess
    @PostMapping("create")
    @RequiresPermissions("app:create")
    public RestResponse create(Application app) throws IOException {
        if (app.getTeamId() == null || app.getTeamId() <= 0L) {
            return RestResponse.success(false).message("请选择团队");
        }
        boolean saved = applicationService.create(app);
        return RestResponse.success(saved);
    }

    @PostMapping("update")
    @RequiresPermissions("app:update")
    public RestResponse update(Application app) {
        applicationService.update(app);
        return RestResponse.success(true);
    }

    @PostMapping("dashboard")
    public RestResponse dashboard() {
        Map<String, Serializable> map = applicationService.dashboard();
        return RestResponse.success(map);
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
        return RestResponse.success(applicationList);
    }

    @PostMapping("mapping")
    @RequiresPermissions("app:mapping")
    public RestResponse mapping(Application app) {
        boolean flag = applicationService.mapping(app);
        return RestResponse.success(flag);
    }

    @PostMapping("revoke")
    @RequiresPermissions("app:launch")
    public RestResponse revoke(Application app) throws Exception {
        applicationService.revoke(app);
        return RestResponse.success();
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
            return RestResponse.success(true);
        } catch (Exception e) {
            return RestResponse.success(false).message(e.getMessage());
        }
    }

    @ApiAccess
    @ApiOperation(value = "App Cancel", notes = "App Cancel", tags = ApiDocConstant.FLINK_APP_OP_TAG, consumes = "x-www-form-urlencoded")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", value = "app Id", required = true, paramType = "form", dataType = "Long"),
        @ApiImplicitParam(name = "savePointed", value = "trigger savePoint before taking stoping", required = true, paramType = "form", dataType = "Boolean", defaultValue = "false"),
        @ApiImplicitParam(name = "savePoint", value = "savepoint path", paramType = "form", dataType = "String", defaultValue = "hdfs:///tm/xxx"),
        @ApiImplicitParam(name = "drain", value = "取消前发送最大 watermark", required = true, paramType = "form", dataType = "Boolean", defaultValue = "false")})
    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public RestResponse cancel(@ApiIgnore Application app) throws Exception {
        applicationService.cancel(app);
        return RestResponse.success();
    }

    @ApiAccess
    @PostMapping("clean")
    @RequiresPermissions("app:clean")
    public RestResponse clean(Application app) {
        applicationService.clean(app);
        return RestResponse.success(true);
    }

    /**
     * 强制停止.(正常启动或者停止一直在进行中)
     * @param app
     * @return
     */
    @PostMapping("forcedStop")
    @RequiresPermissions("app:cancel")
    public RestResponse forcedStop(Application app) {
        applicationService.forcedStop(app);
        return RestResponse.success();
    }

    @PostMapping("yarn")
    public RestResponse yarn() {
        return RestResponse.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    public RestResponse yarnName(Application app) {
        String yarnName = applicationService.getYarnName(app);
        return RestResponse.success(yarnName);
    }

    @PostMapping("checkName")
    public RestResponse checkName(Application app) {
        AppExistsState exists = applicationService.checkExists(app);
        return RestResponse.success(exists.get());
    }

    @PostMapping("readConf")
    public RestResponse readConf(Application app) throws IOException {
        String config = applicationService.readConf(app);
        return RestResponse.success(config);
    }

    @PostMapping("main")
    public RestResponse getMain(Application application) {
        String mainClass = applicationService.getMain(application);
        return RestResponse.success(mainClass);
    }

    @PostMapping("backups")
    public RestResponse backups(ApplicationBackUp backUp, RestRequest request) {
        IPage<ApplicationBackUp> backups = backUpService.page(backUp, request);
        return RestResponse.success(backups);
    }

    @PostMapping("rollback")
    public RestResponse rollback(ApplicationBackUp backUp) {
        //TODO: next version implementation
        //backUpService.rollback(backUp);
        return RestResponse.success();
    }

    @PostMapping("optionlog")
    public RestResponse optionlog(ApplicationLog applicationLog, RestRequest request) {
        IPage<ApplicationLog> applicationList = applicationLogService.page(applicationLog, request);
        return RestResponse.success(applicationList);
    }

    @PostMapping("delete")
    public RestResponse delete(Application app) throws InternalException {
        Boolean deleted = applicationService.delete(app);
        return RestResponse.success(deleted);
    }

    @PostMapping("deletebak")
    public RestResponse deleteBak(ApplicationBackUp backUp) throws InternalException {
        Boolean deleted = backUpService.delete(backUp.getId());
        return RestResponse.success(deleted);
    }

    @PostMapping("checkjar")
    public RestResponse checkjar(String jar) {
        File file = new File(jar);
        try {
            Utils.checkJarFile(file.toURI().toURL());
            return RestResponse.success(true);
        } catch (IOException e) {
            return RestResponse.success(file).message(e.getLocalizedMessage());
        }
    }

    @PostMapping("upload")
    @RequiresPermissions("app:create")
    public RestResponse upload(MultipartFile file) throws ApplicationException {
        String uploadPath = applicationService.upload(file);
        return RestResponse.success(uploadPath);
    }

    @PostMapping("downlog")
    public RestResponse downlog(Long id) {
        applicationService.tailMvnDownloading(id);
        return RestResponse.success();
    }

    @PostMapping("verifySchema")
    public RestResponse verifySchema(String path) {
        final URI uri = URI.create(path);
        final String scheme = uri.getScheme();
        final String pathPart = uri.getPath();
        RestResponse restResponse = RestResponse.success(true);
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

    @PostMapping("checkSavepointPath")
    public RestResponse checkSavepointPath(Application app) throws Exception {
        String error = applicationService.checkSavepointPath(app);
        if (error == null) {
            return RestResponse.success(true);
        } else {
            return RestResponse.success(false).message(error);
        }
    }

    @ApiOperation(value = "APP detail")
    @PostMapping(value = "/detail")
    public RestResponse detail(@ApiParam("K8s name spaces") @RequestParam(value = "namespac", required = false) String namespac,
                               @ApiParam("Job name") @RequestParam(value = "jobName", required = false) String jobName,
                               @ApiParam("Number of log lines skipped loading") @RequestParam(value = "skipLineNum", required = false) Integer skipLineNum,
                               @ApiParam("Number of log lines loaded at once") @RequestParam(value = "limit", required = false) Integer limit) {
        return RestResponse.success(MoreFutures.derefUsingDefaultTimeout(loggerService.queryLog(namespac, jobName, skipLineNum, limit)));
    }
}
