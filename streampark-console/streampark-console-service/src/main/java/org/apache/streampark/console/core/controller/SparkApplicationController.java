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
import org.apache.streampark.console.core.annotation.AppUpdated;
import org.apache.streampark.console.core.entity.ApplicationBackUp;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkApplicationLog;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.SparkApplicationLogService;
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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
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
    private ApplicationBackUpService backUpService;

    @Autowired
    private SparkApplicationLogService applicationLogService;

    @Autowired
    private ResourceService resourceService;

    @PostMapping("get")
    @RequiresPermissions("app:detail")
    public RestResponse get(SparkApplication app) {
        SparkApplication application = applicationManageService.getApp(app.getId());
        return RestResponse.success(application);
    }

    @PostMapping("create")
    @RequiresPermissions("app:create")
    public RestResponse create(SparkApplication app) throws IOException {
        boolean saved = applicationManageService.create(app);
        return RestResponse.success(saved);
    }

    @PostMapping("copy")
    @RequiresPermissions("app:copy")
    public RestResponse copy(SparkApplication app) throws IOException {
        applicationManageService.copy(app);
        return RestResponse.success();
    }

    @AppUpdated
    @PostMapping("update")
    @RequiresPermissions("app:update")
    public RestResponse update(SparkApplication app) {
        applicationManageService.update(app);
        return RestResponse.success(true);
    }

    @PostMapping("dashboard")
    public RestResponse dashboard(Long teamId) {
        Map<String, Serializable> dashboardMap = applicationInfoService.getDashboardDataMap(teamId);
        return RestResponse.success(dashboardMap);
    }

    @PostMapping("list")
    @RequiresPermissions("app:view")
    public RestResponse list(SparkApplication app, RestRequest request) {
        IPage<SparkApplication> applicationList = applicationManageService.page(app, request);
        return RestResponse.success(applicationList);
    }

    @AppUpdated
    @PostMapping("mapping")
    @RequiresPermissions("app:mapping")
    public RestResponse mapping(SparkApplication app) {
        boolean flag = applicationManageService.mapping(app);
        return RestResponse.success(flag);
    }

    @AppUpdated
    @PostMapping("revoke")
    @RequiresPermissions("app:release")
    public RestResponse revoke(SparkApplication app) {
        applicationActionService.revoke(app.getId());
        return RestResponse.success();
    }

    @PostMapping("check/start")
    @RequiresPermissions("app:start")
    public RestResponse checkStart(SparkApplication app) {
        AppExistsStateEnum stateEnum = applicationInfoService.checkStart(app.getId());
        return RestResponse.success(stateEnum.get());
    }

    @PostMapping("start")
    @RequiresPermissions("app:start")
    public RestResponse start(SparkApplication app) {
        try {
            applicationActionService.start(app, false);
            return RestResponse.success(true);
        } catch (Exception e) {
            return RestResponse.success(e.getMessage(), false);
        }
    }

    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public RestResponse stop(SparkApplication app) throws Exception {
        applicationActionService.stop(app);
        return RestResponse.success();
    }

    @AppUpdated
    @PostMapping("clean")
    @RequiresPermissions("app:clean")
    public RestResponse clean(SparkApplication app) {
        applicationManageService.clean(app);
        return RestResponse.success(true);
    }

    @PostMapping("forcedStop")
    @RequiresPermissions("app:cancel")
    public RestResponse forcedStop(SparkApplication app) {
        applicationActionService.forcedStop(app.getId());
        return RestResponse.success();
    }

    @PostMapping("yarn")
    public RestResponse yarn() {
        return RestResponse.success(YarnUtils.getRMWebAppProxyURL());
    }

    @PostMapping("name")
    public RestResponse yarnName(SparkApplication app) {
        String yarnName = applicationInfoService.getYarnName(app.getConfig());
        return RestResponse.success(yarnName);
    }

    @PostMapping("check/name")
    public RestResponse checkName(SparkApplication app) {
        AppExistsStateEnum exists = applicationInfoService.checkExists(app);
        return RestResponse.success(exists.get());
    }

    @PostMapping("read_conf")
    public RestResponse readConf(SparkApplication app) throws IOException {
        String config = applicationInfoService.readConf(app.getConfig());
        return RestResponse.success(config);
    }

    @PostMapping("main")
    public RestResponse getMain(SparkApplication application) {
        String mainClass = applicationInfoService.getMain(application);
        return RestResponse.success(mainClass);
    }

    @PostMapping("backups")
    public RestResponse backups(ApplicationBackUp backUp, RestRequest request) {
        IPage<ApplicationBackUp> backups = backUpService.getPage(backUp, request);
        return RestResponse.success(backups);
    }

    @PostMapping("opt_log")
    public RestResponse optionlog(SparkApplicationLog applicationLog, RestRequest request) {
        IPage<SparkApplicationLog> applicationList = applicationLogService.getPage(applicationLog, request);
        return RestResponse.success(applicationList);
    }

    @PostMapping("delete/opt_log")
    @RequiresPermissions("app:delete")
    public RestResponse deleteOperationLog(Long id) {
        Boolean deleted = applicationLogService.removeById(id);
        return RestResponse.success(deleted);
    }

    @PostMapping("delete")
    @RequiresPermissions("app:delete")
    public RestResponse delete(SparkApplication app) throws InternalException {
        Boolean deleted = applicationManageService.remove(app.getId());
        return RestResponse.success(deleted);
    }

    @PostMapping("delete/bak")
    public RestResponse deleteBak(ApplicationBackUp backUp) throws InternalException {
        Boolean deleted = backUpService.removeById(backUp.getId());
        return RestResponse.success(deleted);
    }

    @PostMapping("check/jar")
    public RestResponse checkjar(String jar) {
        File file = new File(jar);
        try {
            Utils.requireCheckJarFile(file.toURI().toURL());
            return RestResponse.success(true);
        } catch (IOException e) {
            return RestResponse.success(e.getLocalizedMessage());
        }
    }

    @PostMapping("upload")
    @RequiresPermissions("app:create")
    public RestResponse upload(MultipartFile file) throws Exception {
        String uploadPath = resourceService.upload(file);
        return RestResponse.success(uploadPath);
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
            restResponse = RestResponse.success(error, false);
        }
        return restResponse;
    }
}
