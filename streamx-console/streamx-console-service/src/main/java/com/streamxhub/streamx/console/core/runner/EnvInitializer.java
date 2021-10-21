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
import com.streamxhub.streamx.common.conf.Workspace;
import com.streamxhub.streamx.common.enums.StorageType;
import com.streamxhub.streamx.common.fs.FsOperator;
import com.streamxhub.streamx.common.util.SystemPropertyUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.entity.FlinkVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.streamxhub.streamx.common.enums.StorageType.LFS;

/**
 * @author benjobs
 */
@Order
@Slf4j
@Component
public class EnvInitializer implements ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    private final Map<StorageType, Boolean> initialized = new ConcurrentHashMap<>(2);

    private static final Pattern PATTERN_FLINK_SHIMS_JAR = Pattern.compile(
        "^streamx-flink-shims_flink-(1.12|1.13|1.14)-(.*).jar$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        overrideSystemProp(ConfigConst.KEY_STREAMX_WORKSPACE_LOCAL(), ConfigConst.STREAMX_WORKSPACE_DEFAULT());
        overrideSystemProp(ConfigConst.KEY_STREAMX_WORKSPACE_REMOTE(), ConfigConst.STREAMX_WORKSPACE_DEFAULT());
        overrideSystemProp(ConfigConst.KEY_DOCKER_IMAGE_NAMESPACE(), ConfigConst.DOCKER_IMAGE_NAMESPACE_DEFAULT());
        //automatic in local
        storageInitialize(LFS);
    }

    private void overrideSystemProp(String key, String defaultValue) {
        String value = context.getEnvironment().getProperty(key, defaultValue);
        log.info("initialize system properties: key:{}, value:{}", key, value);
        SystemPropertyUtils.set(key, value);
    }

    /**
     * @param storageType
     * @throws Exception
     */
    public synchronized void storageInitialize(StorageType storageType) throws Exception {
        if (initialized.get(storageType) == null) {
            FsOperator fsOperator = FsOperator.of(storageType);
            Workspace workspace = Workspace.of(storageType);

            if (storageType.equals(LFS)) {
                String localDist = workspace.APP_LOCAL_DIST();
                if (!fsOperator.exists(localDist)) {
                    log.info("mkdir {} starting ...", localDist);
                    fsOperator.mkdirs(localDist);
                }
            }

            String appUploads = workspace.APP_UPLOADS();
            if (!fsOperator.exists(appUploads)) {
                log.info("mkdir {} starting ...", appUploads);
                fsOperator.mkdirs(appUploads);
            }

            String appWorkspace = workspace.APP_WORKSPACE();
            if (!fsOperator.exists(appWorkspace)) {
                log.info("mkdir {} starting ...", appWorkspace);
                fsOperator.mkdirs(appWorkspace);
            }

            String appBackups = workspace.APP_BACKUPS();
            if (!fsOperator.exists(appBackups)) {
                log.info("mkdir {} starting ...", appBackups);
                fsOperator.mkdirs(appBackups);
            }

            String appSavePoints = workspace.APP_SAVEPOINTS();
            if (!fsOperator.exists(appSavePoints)) {
                log.info("mkdir {} starting ...", appSavePoints);
                fsOperator.mkdirs(appSavePoints);
            }

            String appJars = workspace.APP_JARS();
            if (!fsOperator.exists(appJars)) {
                log.info("mkdir {} starting ...", appJars);
                fsOperator.mkdirs(appJars);
            }

            String appPlugins = workspace.APP_PLUGINS();
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

            String appShims = workspace.APP_SHIMS();
            if (fsOperator.exists(appShims)) {
                fsOperator.delete(appShims);
            }

            File[] shims = new File(WebUtils.getAppDir("lib")).listFiles(pathname -> pathname.getName().matches(PATTERN_FLINK_SHIMS_JAR.pattern()));
            for (File file : Objects.requireNonNull(shims)) {
                Matcher matcher = PATTERN_FLINK_SHIMS_JAR.matcher(file.getName());
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
            String localMavenRepo = workspace.MAVEN_LOCAL_DIR();
            if (FsOperator.lfs().exists(localMavenRepo)) {
                FsOperator.lfs().mkdirs(localMavenRepo);
            }
            initialized.put(storageType, Boolean.TRUE);
        }
    }

    public void checkFlinkEnv(StorageType storageType, FlinkVersion flinkVersion) {
        String flinkLocalHome = flinkVersion.getFlinkHome();
        if (flinkLocalHome == null) {
            throw new ExceptionInInitializerError("[StreamX] FLINK_HOME is undefined,Make sure that Flink is installed.");
        }
        Workspace workspace = Workspace.of(storageType);
        String appFlink = workspace.APP_FLINK();
        FsOperator fsOperator = FsOperator.of(storageType);
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
