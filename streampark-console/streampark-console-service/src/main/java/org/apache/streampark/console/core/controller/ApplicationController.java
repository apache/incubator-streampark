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
import org.apache.streampark.console.base.domain.ApiDocConstant;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.base.util.MoreFutures;
import org.apache.streampark.console.core.annotation.ApiAccess;
import org.apache.streampark.console.core.bean.AppControl;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationBackUp;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.enums.AppExistsState;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.LoggerService;
import org.apache.streampark.flink.packer.pipeline.PipelineStatus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = {"FLINK_APPLICATION_TAG"})
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
    private LoggerService logService;

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
        boolean saved = applicationService.create(app);
        return RestResponse.success(saved);
    }

    @ApiAccess
    @ApiOperation(value = "Copy application from the exist app", tags = ApiDocConstant.FLINK_APP_OP_TAG, consumes = "application/x-www-form-urlencoded")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", value = "copy target app id", required = true, paramType = "query", dataTypeClass = Long.class),
        @ApiImplicitParam(name = "jobName", value = "name of the copied application", required = true, paramType = "query", dataTypeClass = String.class, defaultValue = ""),
        @ApiImplicitParam(name = "args", value = "commit parameters after copying", required = false, paramType = "query", dataTypeClass = String.class, defaultValue = "")})
    @PostMapping(value = "copy", consumes = "application/x-www-form-urlencoded")
    @RequiresPermissions("app:copy")
    public RestResponse copy(@ApiIgnore Application app) throws IOException {
        Long id = applicationService.copy(app);
        Map<String, String> data = new HashMap<>();
        data.put("id", Long.toString(id));
        return id.equals(0) ? RestResponse.success(false).data(data) : RestResponse.success(true).data(data);
    }

    @PostMapping("update")
    @RequiresPermissions("app:update")
    public RestResponse update(Application app) {
        applicationService.update(app);
        return RestResponse.success(true);
    }

    @PostMapping("dashboard")
    public RestResponse dashboard(Long teamId) {
        Map<String, Serializable> map = applicationService.dashboard(teamId);
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
        }).peek(e -> {
            AppControl appControl = new AppControl()
                .setAllowBuild(e.getBuildStatus() == null || !PipelineStatus.running.getCode().equals(e.getBuildStatus()))
                .setAllowStart(!e.shouldBeTrack() && PipelineStatus.success.getCode().equals(e.getBuildStatus()))
                .setAllowStop(e.isRunning());
            e.setAppControl(appControl);
        }).collect(Collectors.toList());
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
    public RestResponse revoke(Application app) {
        applicationService.revoke(app);
        return RestResponse.success();
    }

    @ApiAccess
    @ApiOperation(value = "Start application", tags = ApiDocConstant.FLINK_APP_OP_TAG, consumes = "application/x-www-form-urlencoded")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", value = "app Id", required = true, paramType = "query", dataTypeClass = Long.class),
        @ApiImplicitParam(name = "savePointed", value = "restored app from the savepoint or latest checkpoint", required = true, paramType = "query", dataTypeClass = Boolean.class, defaultValue = "false"),
        @ApiImplicitParam(name = "savePoint", value = "savepoint or checkpoint path", required = true, paramType = "query", dataTypeClass = String.class, defaultValue = ""),
        @ApiImplicitParam(name = "flameGraph", value = "whether the flame graph support", required = true, paramType = "query", dataTypeClass = Boolean.class, defaultValue = "false"),
        @ApiImplicitParam(name = "allowNonRestored", value = "ignore savepoint then cannot be restored", required = true, paramType = "query", dataTypeClass = Boolean.class, defaultValue = "false")})
    @PostMapping(value = "start", consumes = "application/x-www-form-urlencoded")
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
    @ApiOperation(value = "Cancel application", tags = ApiDocConstant.FLINK_APP_OP_TAG, consumes = "application/x-www-form-urlencoded")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", value = "app id", required = true, paramType = "query", dataTypeClass = Long.class),
        @ApiImplicitParam(name = "savePointed", value = "whether trigger savepoint before taking stopping", required = true, paramType = "query", dataTypeClass = Boolean.class, defaultValue = "false"),
        @ApiImplicitParam(name = "savePoint", value = "savepoint path", paramType = "query", dataTypeClass = String.class, defaultValue = "hdfs:///tm/xxx"),
        @ApiImplicitParam(name = "drain", value = "send max watermark before canceling", required = true, paramType = "query", dataTypeClass = Boolean.class, defaultValue = "false")})
    @PostMapping(value = "cancel", consumes = "application/x-www-form-urlencoded")
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
     * force stop(stop normal start or in progress)
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

    @ApiOperation(value = "Read flink on k8s deploy log")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "namespace", value = "ks8 namespace"),
        @ApiImplicitParam(name = "jobName", value = "job name"),
        @ApiImplicitParam(name = "jobId", value = "job id"),
        @ApiImplicitParam(name = "skipLineNum", value = "number of log lines skipped loading"),
        @ApiImplicitParam(name = "limit", value = "number of log lines loaded at once")
    })
    @PostMapping(value = "/detail")
    public RestResponse detail(@RequestParam(value = "namespace", required = false) String namespace,
                               @RequestParam(value = "jobName", required = false) String jobName,
                               @RequestParam(value = "jobId", required = false) String jobId,
                               @RequestParam(value = "skipLineNum", required = false) Integer skipLineNum,
                               @RequestParam(value = "limit", required = false) Integer limit) {
        return RestResponse.success(MoreFutures.derefUsingDefaultTimeout(logService.queryLog(namespace, jobName, jobId, skipLineNum, limit)));
    }

}
