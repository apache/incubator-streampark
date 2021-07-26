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
import com.streamxhub.streamx.console.base.util.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        System.getProperties().setProperty(
                ConfigConst.KEY_STREAMX_HDFS_WORKSPACE(),
                context.getEnvironment().getProperty(
                        ConfigConst.KEY_STREAMX_HDFS_WORKSPACE(),
                        ConfigConst.STREAMX_HDFS_WORKSPACE_DEFAULT()
                )
        );

        String profiles = context.getEnvironment().getActiveProfiles()[0];

        if (profiles.equals(PROD_ENV_NAME)) {

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

            String appSavePoints = ConfigConst.APP_SAVEPOINTS();
            if (!HdfsUtils.exists(appSavePoints)) {
                log.info("mkdir {} starting ...", appSavePoints);
                HdfsUtils.mkdirs(appSavePoints);
            }

            String appJars = ConfigConst.APP_JARS();
            if (!HdfsUtils.exists(appJars)) {
                log.info("mkdir {} starting ...", appJars);
                HdfsUtils.mkdirs(appJars);
            }

            String appPlugins = ConfigConst.APP_PLUGINS();
            if (HdfsUtils.exists(appPlugins)) {
                HdfsUtils.delete(appPlugins);
            }
            HdfsUtils.mkdirs(appPlugins);

            String keepFile = ".gitkeep";

            File plugins = new File(WebUtils.getAppDir("plugins"));
            for (File file : Objects.requireNonNull(plugins.listFiles())) {
                String plugin = appPlugins.concat("/").concat(file.getName());
                if (!HdfsUtils.exists(plugin) && !keepFile.equals(file.getName())) {
                    log.info("load plugin:{} to {}", file.getName(), appPlugins);
                    HdfsUtils.upload(file.getAbsolutePath(), appPlugins, false, true);
                }
            }

            String appShims = ConfigConst.APP_SHIMS();
            if (HdfsUtils.exists(appShims)) {
                HdfsUtils.delete(appShims);
            }
            String regex = "^streamx-flink-shims_flink-(1.12|1.13)-(.*).jar$";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            File[] shims = new File(WebUtils.getAppDir("lib")).listFiles(pathname -> pathname.getName().matches(regex));
            for (File file : Objects.requireNonNull(shims)) {
                Matcher matcher = pattern.matcher(file.getName());
                if (!keepFile.equals(file.getName()) && matcher.matches()) {
                    String version = matcher.group(1);
                    String shimsPath = appShims.concat("/flink-").concat(version);
                    if (!HdfsUtils.exists(shimsPath)) {
                        HdfsUtils.mkdirs(shimsPath);
                    }
                    log.info("load shims:{} to {}", file.getName(), shimsPath);
                    HdfsUtils.upload(file.getAbsolutePath(), shimsPath, false, true);
                }
            }
        } else {
            log.warn("The local test environment is only used in the development phase to provide services to the console web, and many functions will not be available...");
        }
    }

}
