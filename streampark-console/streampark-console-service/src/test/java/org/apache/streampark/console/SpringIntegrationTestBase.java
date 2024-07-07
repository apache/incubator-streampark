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

package org.apache.streampark.console;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.SystemPropertyUtils;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Integration base tester. Note: The all children classes of the base must run after the
 * project-level package phrase.
 */
@Slf4j
@EnableScheduling
@ActiveProfiles("integration-test")
@AutoConfigureTestEntityManager
@AutoConfigureWebTestClient(timeout = "60000")
@ExtendWith({MockitoExtension.class, SpringExtension.class})
@SpringBootTest(classes = StreamParkConsoleBootstrap.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "server.port=10000",
        "spring.application.name=Apache StreamPark",
        "spring.main.banner-mode=false",
        "spring.aop.proxy-target-class=true",
        "spring.messages.encoding=utf-8",
        "spring.main.allow-circular-references=true",
        "spring.mvc.converters.preferred-json-mapper=jackson",
        "spring.jackson.date-format=yyyy-MM-dd HH:mm:ss",
        "spring.jackson.time-zone=GMT+8",
        "spring.jackson.deserialization.fail-on-unknown-properties=false",
        "spring.mvc.pathmatch.matching-strategy=ant_path_matcher",
        "datasource.dialect=h2",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.datasource.url=jdbc:h2:mem:streampark;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true;INIT=runscript from 'classpath:db/schema-h2.sql'",
        "spring.sql.init.data-locations=classpath:db/data-h2.sql",
        "spring.sql.init.continue-on-error=true",
        "spring.sql.init.username=sa",
        "spring.sql.init.password=sa",
        "spring.sql.init.mode=always"
})
public abstract class SpringIntegrationTestBase {

    protected static final Logger LOG = LoggerFactory.getLogger(SpringIntegrationTestBase.class);

    protected static final String RUN_PKG_SCRIPT_HINT = "Please run package script before running the test case.";

    protected static final String DEFAULT_APP_HOME_DIR_NAME = "apache-streampark";
    protected static final String DEFAULT_LOCAL_WORKSPACE_DIR_NAME = "localWorkspace";
    protected static final String DEFAULT_FLINK_VERSION = "1.17.1";
    protected static final FileFilter PKG_NAME_FILTER = file -> file.getName().startsWith(DEFAULT_APP_HOME_DIR_NAME)
            && file.isDirectory();
    protected static String defaultFlinkHome = "/tmp/flink-1.17.1";
    protected static String appHome;

    @BeforeAll
    public static void init(@TempDir File tempPath) throws IOException {

        LOG.info("Start prepare the real running env.");
        String tempAbsPath = tempPath.getAbsolutePath();
        LOG.info("Integration test base tmp dir: {}", tempAbsPath);

        FileUtils.copyDirectory(
                tryFindStreamParkPackagedDirFile(), new File(tempAbsPath, DEFAULT_APP_HOME_DIR_NAME));

        Path localWorkspace = Files.createDirectories(new File(tempAbsPath, DEFAULT_LOCAL_WORKSPACE_DIR_NAME).toPath());

        appHome = new File(tempAbsPath, DEFAULT_APP_HOME_DIR_NAME).getAbsolutePath();
        System.setProperty(ConfigKeys.KEY_APP_HOME(), appHome);
        System.setProperty(
                CommonConfig.STREAMPARK_WORKSPACE_LOCAL().key(),
                localWorkspace.toAbsolutePath().toString());

        LOG.info(
                "Complete mock EnvInitializer init, app home: {}, {}: {}",
                appHome,
                CommonConfig.STREAMPARK_WORKSPACE_LOCAL().key(),
                localWorkspace.toAbsolutePath());
    }

    private static File tryFindStreamParkPackagedDirFile() {
        String userDir = AssertUtils.notNull(SystemPropertyUtils.get("user.dir"));
        File pkgTargetDirFile = new File(userDir, "target");
        AssertUtils.state(
                pkgTargetDirFile.exists(),
                String.format(
                        "The target directory of %s doesn't exist. %s", userDir, RUN_PKG_SCRIPT_HINT));
        Optional<File> availablePkgParentFileOpt = Arrays
                .stream(requireNonNull(pkgTargetDirFile.listFiles(PKG_NAME_FILTER))).findFirst();
        final File availablePkgParentFile = availablePkgParentFileOpt
                .orElseThrow(() -> new RuntimeException(RUN_PKG_SCRIPT_HINT));
        Optional<File> targetDirFile = Arrays.stream(requireNonNull(availablePkgParentFile.listFiles(PKG_NAME_FILTER)))
                .findFirst();
        return targetDirFile.orElseThrow(() -> new RuntimeException(RUN_PKG_SCRIPT_HINT));
    }
}
