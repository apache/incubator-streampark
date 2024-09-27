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
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.YarnQueue;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** base tester. */
@Slf4j
@EnableScheduling
@ActiveProfiles("test")
@AutoConfigureTestEntityManager
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
@ExtendWith({MockitoExtension.class, SpringExtension.class})
@SpringBootApplication(scanBasePackageClasses = {StreamParkConsoleBootstrap.class})
public abstract class SpringUnitTestBase {

    protected static final Logger LOG = LoggerFactory.getLogger(SpringUnitTestBase.class);

    @BeforeAll
    public static void init(@TempDir File tempPath) throws IOException {
        // Skip the EnvInitializer#run method by flag in System.properties.
        LOG.info("Start mock EnvInitializer init.");
        String mockedHome = tempPath.getAbsolutePath();
        Path localWorkspace = Files.createDirectories(new File(mockedHome + "/localWorkspace").toPath());

        System.setProperty(ConfigKeys.KEY_APP_HOME(), mockedHome);
        System.setProperty(
            CommonConfig.STREAMPARK_WORKSPACE_LOCAL().key(),
            localWorkspace.toAbsolutePath().toString());

        Files.createDirectories(new File(mockedHome + "/temp").toPath());

        LOG.info(
            "Complete mock EnvInitializer init, app home: {}, {}: {}",
            tempPath.getAbsolutePath(),
            CommonConfig.STREAMPARK_WORKSPACE_LOCAL().key(),
            localWorkspace.toAbsolutePath());
    }

    // Help methods.

    protected FlinkCluster mockYarnSessionFlinkCluster(
                                                       String name, String yarnQueue, Long versionId) {
        FlinkCluster cluster = new FlinkCluster();
        cluster.setClusterName(name);
        cluster.setYarnQueue(yarnQueue);
        cluster.setVersionId(versionId);
        cluster.setDeployMode(FlinkDeployMode.YARN_SESSION.getMode());
        return cluster;
    }

    protected FlinkApplication mockYarnModeJobApp(
                                                  Long teamId, String name, String yarnQueue,
                                                  FlinkDeployMode deployModeEnum) {
        FlinkApplication application = new FlinkApplication();
        application.setYarnQueue(yarnQueue);
        application.setTeamId(teamId);
        application.setJobName(name);
        application.setDeployMode(deployModeEnum.getMode());
        application.doSetHotParams();
        return application;
    }

    protected YarnQueue mockYarnQueue(Long teamId, String queueLabel) {
        YarnQueue yarnQueue = new YarnQueue();
        yarnQueue.setTeamId(teamId);
        yarnQueue.setQueueLabel(queueLabel);
        return yarnQueue;
    }
}
