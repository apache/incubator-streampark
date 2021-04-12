/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.core.runner;

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.util.HdfsUtils;
import com.streamxhub.streamx.console.base.utils.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Objects;

/**
 * @author benjobs
 */
@Order
@Slf4j
@Component
public class EnvInitializeRunner implements ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    private String PROD_ENV_NAME = "prod";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String profiles = context.getEnvironment().getActiveProfiles()[0];
        if (profiles.equals(PROD_ENV_NAME)) {
            String flinkLocalHome = System.getenv("FLINK_HOME");
            if (flinkLocalHome == null) {
                throw new ExceptionInInitializerError("[StreamX] FLINK_HOME is undefined,Make sure that Flink is installed.");
            }

            String appFlink = ConfigConst.APP_FLINK();
            if (!HdfsUtils.exists(appFlink)) {
                log.info("mkdir {} starting ...", appFlink);
                HdfsUtils.mkdirs(appFlink);
            }

            String appUploads = ConfigConst.APP_UPLOADS();
            if (!HdfsUtils.exists(appUploads)) {
                log.info("mkdir {} starting ...", appUploads);
                HdfsUtils.mkdirs(appUploads);
            }

            String appWorkspace = ConfigConst.APP_WORKSPACE();
            if (!HdfsUtils.exists(appWorkspace)) {
                log.info("mkdir {} starting ...", appWorkspace);
                HdfsUtils.mkdirs(appWorkspace);
            }

            String appBackups = ConfigConst.APP_BACKUPS();
            if (!HdfsUtils.exists(appBackups)) {
                log.info("mkdir {} starting ...", appBackups);
                HdfsUtils.mkdirs(appBackups);
            }

            String appPlugins = ConfigConst.APP_PLUGINS();
            if (!HdfsUtils.exists(appPlugins)) {
                log.info("mkdir {} starting ...", appPlugins);
                HdfsUtils.mkdirs(appPlugins);
            }

            String appSavepoints = ConfigConst.APP_SAVEPOINTS();
            if (!HdfsUtils.exists(appSavepoints)) {
                log.info("mkdir {} starting ...", appSavepoints);
                HdfsUtils.mkdirs(appSavepoints);
            }

            String appJars = ConfigConst.APP_JARS();
            if (!HdfsUtils.exists(appJars)) {
                log.info("mkdir {} starting ...", appJars);
                HdfsUtils.mkdirs(appJars);
            }

            String flinkName = new File(flinkLocalHome).getName();
            String flinkHome = appFlink.concat("/").concat(flinkName);
            if (!HdfsUtils.exists(flinkHome)) {
                log.info("{} is not exists,upload beginning....", flinkHome);
                HdfsUtils.upload(flinkLocalHome, flinkHome, false, false);
            }

            File plugins = new File(WebUtil.getAppDir("plugins"));
            for (File file : Objects.requireNonNull(plugins.listFiles())) {
                String plugin = appPlugins.concat("/").concat(file.getName());
                if (!HdfsUtils.exists(plugin)) {
                    log.info("load plugin:{} to {}", file.getName(), appPlugins);
                    HdfsUtils.upload(file.getAbsolutePath(), appPlugins, false, false);
                }
            }
        } else {
            log.warn("The local test environment is only used in the development phase to provide services to the console web, and many functions will not be available...");
        }
    }

}
