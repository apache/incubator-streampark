/*
 *  Copyright (c) 2019 The StreamX Project
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.console.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.ApplicationBackUp;
import com.streamxhub.streamx.console.core.entity.ApplicationLog;
import com.streamxhub.streamx.console.core.enums.AppExistsState;
import com.streamxhub.streamx.console.core.service.ApplicationBackUpService;
import com.streamxhub.streamx.console.core.service.ApplicationLogService;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

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

    @PostMapping("get")
    @RequiresPermissions("app:detail")
    public RestResponse get(Application app) {
        Application application = applicationService.getApp(app);
        return RestResponse.create().data(application);
    }

    @PostMapping("create")
    @RequiresPermissions("app:create")
    public RestResponse create(Application app) throws IOException {
        boolean saved = applicationService.create(app);
        return RestResponse.create().data(saved);
    }

    @PostMapping("update")
    @RequiresPermissions("app:update")
    public RestResponse update(Application app) {
        try {
            applicationService.update(app);
            return RestResponse.create().data(true);
        } catch (Exception e) {
            return RestResponse.create().data(false);
        }
    }

    @PostMapping("dashboard")
    public RestResponse dashboard() {
        Map<String, Serializable> map = applicationService.dashboard();
        return RestResponse.create().data(map);
    }

    @PostMapping("list")
    @RequiresPermissions("app:view")
    public RestResponse list(Application app, RestRequest request) {
        IPage<Application> applicationList = applicationService.page(app, request);
        return RestResponse.create().data(applicationList);
    }

    @PostMapping("mapping")
    @RequiresPermissions("app:mapping")
    public RestResponse mapping(Application app) {
        boolean flag = applicationService.mapping(app);
        return RestResponse.create().data(flag);
    }

    @PostMapping("deploy")
    @RequiresPermissions("app:deploy")
    public RestResponse deploy(Application app) {
        Application application = applicationService.getById(app.getId());
        assert application != null;
        application.setBackUp(true);
        application.setBackUpDescription(app.getBackUpDescription());
        applicationService.deploy(application);
        return RestResponse.create();
    }

    @PostMapping("revoke")
    @RequiresPermissions("app:deploy")
    public RestResponse revoke(Application app) throws Exception {
        applicationService.revoke(app);
        return RestResponse.create();
    }

    @PostMapping("start")
    @RequiresPermissions("app:start")
    public RestResponse start(Application app) throws Exception {
        boolean success = applicationService.checkStart(app);
        if (success) {
            applicationService.starting(app);
            boolean started = applicationService.start(app, false);
            return RestResponse.create().data(started ? 1 : 0);
        } else {
            return RestResponse.create().data(-1);
        }
    }

    @PostMapping("clean")
    @RequiresPermissions("app:clean")
    public RestResponse clean(Application app) {
        applicationService.clean(app);
        return RestResponse.create().data(true);
    }

    @PostMapping("cancel")
    @RequiresPermissions("app:cancel")
    public RestResponse cancel(Application app) {
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

    @PostMapping("exists")
    public RestResponse exists(Application app) {
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
        backUpService.rollback(backUp);
        return RestResponse.create();
    }

    @PostMapping("startlog")
    public RestResponse startlog(ApplicationLog applicationLog, RestRequest request) {
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
    public RestResponse upload(MultipartFile file) throws IOException {
        boolean upload = applicationService.upload(file);
        return RestResponse.create().data(upload);
    }


}
