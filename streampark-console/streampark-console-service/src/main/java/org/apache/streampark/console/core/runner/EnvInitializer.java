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

package org.apache.streampark.console.core.runner;

import org.apache.streampark.common.configuration.Workspace;
import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.console.base.config.SpringConfigurationInitializer;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.service.SettingService;

import org.apache.commons.lang3.StringUtils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.streampark.common.enums.StorageType.LFS;

/**
 * Prepares Console runtime resources after the Spring application context has started.
 *
 * <p>Configuration resolution is delegated to {@link SpringConfigurationInitializer}. This runner
 * coordinates only the dependent startup work: applying database-backed Maven settings and
 * preparing local or remote artifact storage.
 */
@Order(1)
@Slf4j
@Component
public class EnvInitializer implements ApplicationRunner {

    private final ApplicationContext context;
    private final SettingService settingService;
    private final SpringConfigurationInitializer springConfigurationInitializer;

    private final Set<StorageType> initialized = new HashSet<>(2);

    private final FileFilter fileFilter = p -> !".gitkeep".equals(p.getName());

    private static final Pattern PATTERN_FLINK_SHIMS_JAR = Pattern.compile(
        "^streampark-flink-shims_flink-(1\\.1[8-9]|1\\.2\\d|2\\.[0-3])-(.*).jar$",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Creates the ordered Console runtime initializer.
     *
     * @param context active Spring application context
     * @param settingService persistent Console setting service
     * @param springConfigurationInitializer Spring-to-StreamPark configuration bridge
     */
    public EnvInitializer(
                          ApplicationContext context,
                          SettingService settingService,
                          SpringConfigurationInitializer springConfigurationInitializer) {
        this.context = context;
        this.settingService = settingService;
        this.springConfigurationInitializer = springConfigurationInitializer;
    }

    @SneakyThrows
    @Override
    public void run(ApplicationArguments args) throws Exception {

        // Resolve framework-specific sources after Spring has applied environment and CLI rules.
        initConfig();

        boolean isTest = Arrays.asList(context.getEnvironment().getActiveProfiles()).contains("test");
        if (!isTest) {
            // initialize local file system resources
            storageInitialize(LFS);
        }
    }

    private void initConfig() {
        springConfigurationInitializer.initialize(context.getEnvironment());

        settingService.getMavenConfig().updateConfig();
    }

    public synchronized void storageInitialize(StorageType storageType) {

        if (initialized.contains(storageType)) {
            return;
        }

        FsOperator fsOperator = FsOperator.of(storageType);
        Workspace workspace = LFS == storageType ? Workspace.LOCAL : Workspace.REMOTE;

        // 1. prepare workspace dir
        prepareWorkspace(storageType, fsOperator, workspace);
        // 2. upload jar.

        // 2.1) upload client jar
        uploadClientJar(workspace, fsOperator);
        // 2.2) upload shims jar
        uploadShimsJar(workspace, fsOperator);
        // 2.3) create maven local repository dir
        createMvnLocalRepoDir();

        initialized.add(storageType);
    }

    private void prepareWorkspace(
                                  StorageType storageType, FsOperator fsOperator, Workspace workspace) {
        if (LFS == storageType) {
            fsOperator.mkdirsIfNotExists(Workspace.APP_LOCAL_DIST);
        }
        Arrays.asList(
            workspace.uploads,
            workspace.workspace,
            workspace.backups,
            workspace.savepoints,
            workspace.python,
            workspace.jars)
            .forEach(fsOperator::mkdirsIfNotExists);
    }

    private static void createMvnLocalRepoDir() {
        String localMavenRepo = Workspace.MAVEN_LOCAL_PATH;
        if (!FsOperator.lfs().exists(localMavenRepo)) {
            FsOperator.lfs().mkdirs(localMavenRepo);
        }
    }

    private void uploadClientJar(Workspace workspace, FsOperator fsOperator) {
        File client = WebUtils.getAppClientDir();
        File[] clientFiles = client.listFiles(fileFilter);
        AssertUtils.required(
            client.isDirectory() && clientFiles != null && clientFiles.length > 0,
            client.getAbsolutePath().concat(" is not exists or empty directory "));

        String appClient = workspace.client;
        fsOperator.mkCleanDirs(appClient);

        for (File file : clientFiles) {
            log.info("load client:{} to {}", file.getName(), appClient);
            fsOperator.upload(file.getAbsolutePath(), appClient);
        }
    }

    private void uploadShimsJar(Workspace workspace, FsOperator fsOperator) {
        File[] shims = WebUtils.getAppLibDir()
            .listFiles(pathname -> pathname.getName().matches(PATTERN_FLINK_SHIMS_JAR.pattern()));
        AssertUtils.required(shims != null && shims.length > 0, "streampark-flink-shims jar not exist");

        String appShims = workspace.shims;
        fsOperator.delete(appShims);

        for (File file : shims) {
            Matcher matcher = PATTERN_FLINK_SHIMS_JAR.matcher(file.getName());
            if (matcher.matches()) {
                String version = matcher.group(1);
                String shimsPath = appShims.concat("/flink-").concat(version);
                fsOperator.mkdirs(shimsPath);
                log.info("load shims:{} to {}", file.getName(), shimsPath);
                fsOperator.upload(file.getAbsolutePath(), shimsPath);
            }
        }
    }

    public void checkFlinkEnv(StorageType storageType, FlinkEnv flinkEnv) throws IOException {
        String flinkLocalHome = flinkEnv.getFlinkHome();
        if (StringUtils.isBlank(flinkLocalHome)) {
            throw new ExceptionInInitializerError(
                "[StreamPark] FLINK_HOME is undefined,Make sure that Flink is installed.");
        }
        Workspace workspace = LFS == storageType ? Workspace.LOCAL : Workspace.REMOTE;
        String appFlink = workspace.flink;
        FsOperator fsOperator = FsOperator.of(storageType);
        if (!fsOperator.exists(appFlink)) {
            log.info("checkFlinkEnv, now mkdir [{}] starting ...", appFlink);
            fsOperator.mkdirs(appFlink);
        }
        File flinkLocalDir = new File(flinkLocalHome);
        if (Files.isSymbolicLink(flinkLocalDir.toPath())) {
            flinkLocalDir = flinkLocalDir.getCanonicalFile();
        }
        String flinkName = flinkLocalDir.getName();
        String flinkHome = appFlink.concat("/").concat(flinkName);
        if (!fsOperator.exists(flinkHome)) {
            log.info("{} is not exists,upload beginning....", flinkHome);
            fsOperator.upload(flinkLocalHome, flinkHome, false, true);
        }
    }

    public void checkSparkEnv(StorageType storageType, SparkEnv sparkEnv) throws IOException {
        String sparkLocalHome = sparkEnv.getSparkHome();
        if (StringUtils.isBlank(sparkLocalHome)) {
            throw new ExceptionInInitializerError(
                "[StreamPark] SPARK_HOME is undefined,Make sure that Spark is installed.");
        }
        Workspace workspace = LFS == storageType ? Workspace.LOCAL : Workspace.REMOTE;
        String appSpark = workspace.spark;
        FsOperator fsOperator = FsOperator.of(storageType);
        if (!fsOperator.exists(appSpark)) {
            log.info("checkSparkEnv, now mkdir [{}] starting ...", appSpark);
            fsOperator.mkdirs(appSpark);
        }
        File sparkLocalDir = new File(sparkLocalHome);
        if (Files.isSymbolicLink(sparkLocalDir.toPath())) {
            sparkLocalDir = sparkLocalDir.getCanonicalFile();
        }
        String sparkName = sparkLocalDir.getName();
        String sparkHome = appSpark.concat("/").concat(sparkName);
        if (!fsOperator.exists(sparkHome)) {
            log.info("{} is not exists,upload beginning....", sparkHome);
            fsOperator.upload(sparkLocalHome, sparkHome, false, true);
        }
    }
}
