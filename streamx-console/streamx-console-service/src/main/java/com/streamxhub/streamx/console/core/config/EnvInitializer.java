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

package com.streamxhub.streamx.console.core.config;

import static com.streamxhub.streamx.common.enums.StorageType.LFS;

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.enums.StorageType;
import com.streamxhub.streamx.common.fs.FsOperator;
import com.streamxhub.streamx.common.fs.FsOperatorGetter;
import com.streamxhub.streamx.common.util.HdfsUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.service.SettingService;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author benjobs
 */
@Order
@Slf4j
@Component
public class EnvInitializer {

    @Autowired
    private SettingService settingService;

    private final Map<StorageType, Boolean> initialized = new ConcurrentHashMap<>(2);

    /**
     * @param storageType
     * @throws Exception
     */
    public synchronized void storageInitialize(StorageType storageType) throws Exception {
        if (initialized.get(storageType) == null) {

            FsOperator fsOperator = FsOperatorGetter.get(storageType);

            String appUploads = ConfigConst.APP_UPLOADS();
            if (!HdfsUtils.exists(appUploads)) {
                log.info("mkdir {} starting ...", appUploads);
                fsOperator.mkdirs(appUploads);
            }

            String appWorkspace = ConfigConst.APP_WORKSPACE();
            if (!fsOperator.exists(appWorkspace)) {
                log.info("mkdir {} starting ...", appWorkspace);
                fsOperator.mkdirs(appWorkspace);
            }

            String appBackups = ConfigConst.APP_BACKUPS();
            if (!fsOperator.exists(appBackups)) {
                log.info("mkdir {} starting ...", appBackups);
                fsOperator.mkdirs(appBackups);
            }

            String appSavePoints = ConfigConst.APP_SAVEPOINTS();
            if (!fsOperator.exists(appSavePoints)) {
                log.info("mkdir {} starting ...", appSavePoints);
                fsOperator.mkdirs(appSavePoints);
            }

            String appJars = ConfigConst.APP_JARS();
            if (!fsOperator.exists(appJars)) {
                log.info("mkdir {} starting ...", appJars);
                fsOperator.mkdirs(appJars);
            }

            String appPlugins = ConfigConst.APP_PLUGINS();
            if (fsOperator.exists(appPlugins)) {
                fsOperator.delete(appPlugins);
            }
            fsOperator.mkdirs(appPlugins);

            String keepFile = ".gitkeep";

            File plugins = new File(WebUtils.getAppDir("plugins"));
            for (File file : Objects.requireNonNull(plugins.listFiles())) {
                String plugin = appPlugins.concat("/").concat(file.getName());
                if (!fsOperator.exists(plugin) && !keepFile.equals(file.getName())) {
                    log.info("load plugin:{} to {}", file.getName(), appPlugins);
                    fsOperator.upload(file.getAbsolutePath(), appPlugins);
                }
            }

            String appShims = ConfigConst.APP_SHIMS();
            if (fsOperator.exists(appShims)) {
                fsOperator.delete(appShims);
            }
            String regex = "^streamx-flink-shims_flink-(1.12|1.13)-(.*).jar$";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            File[] shims = new File(WebUtils.getAppDir("lib")).listFiles(pathname -> pathname.getName().matches(regex));
            for (File file : Objects.requireNonNull(shims)) {
                Matcher matcher = pattern.matcher(file.getName());
                if (!keepFile.equals(file.getName()) && matcher.matches()) {
                    String version = matcher.group(1);
                    String shimsPath = appShims.concat("/flink-").concat(version);
                    if (!fsOperator.exists(shimsPath)) {
                        fsOperator.mkdirs(shimsPath);
                    }
                    log.info("load shims:{} to {}", file.getName(), shimsPath);
                    fsOperator.upload(file.getAbsolutePath(), shimsPath);
                }
            }
            // create maven local repository dir
            String localMavnRepo = ConfigConst.MAVEN_LOCAL_DIR();
            if (FsOperatorGetter.get(LFS).exists(localMavnRepo)){
                FsOperatorGetter.get(LFS).mkdirs(localMavnRepo);
            }
            initialized.put(storageType, Boolean.TRUE);
        }
    }

    public void checkFlinkEnv(StorageType storageType) {
        String flinkLocalHome = settingService.getEffectiveFlinkHome();
        if (flinkLocalHome == null) {
            throw new ExceptionInInitializerError("[StreamX] FLINK_HOME is undefined,Make sure that Flink is installed.");
        }
        String appFlink = ConfigConst.APP_FLINK();
        FsOperator fsOperator = FsOperatorGetter.get(storageType);
        if (!fsOperator.exists(appFlink)) {
            log.info("mkdir {} starting ...", appFlink);
            fsOperator.mkdirs(appFlink);
        }
        String flinkName = new File(flinkLocalHome).getName();
        String flinkHome = appFlink.concat("/").concat(flinkName);
        if (!fsOperator.exists(flinkHome)) {
            log.info("{} is not exists,upload beginning....", flinkHome);
            fsOperator.upload(flinkLocalHome, flinkHome, false, false);
        }
    }
}
