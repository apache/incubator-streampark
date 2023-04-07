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
import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.YarnQueue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** base tester. */
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestEntityManager
@AutoConfigureWebTestClient(timeout = "60000")
@TestPropertySource(locations = {"classpath:application-test.yml"})
@ExtendWith({MockitoExtension.class, SpringExtension.class})
@SpringBootTest(
    classes = StreamParkConsoleBootstrap.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public abstract class SpringTestBase {

  protected static final Logger LOG = LoggerFactory.getLogger(SpringTestBase.class);

  @BeforeAll
  public static void init(@TempDir File tempPath) throws IOException {
    // Skip the EnvInitializer#run method by flag in System.properties.
    // See https://github.com/apache/incubator-streampark/issues/2014
    LOG.info("Start mock EnvInitializer init.");
    String mockedHome = tempPath.getAbsolutePath();
    Path localWorkspace =
        Files.createDirectories(new File(mockedHome + "/localWorkspace").toPath());

    System.setProperty(ConfigConst.KEY_APP_HOME(), mockedHome);
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
    cluster.setExecutionMode(ExecutionMode.YARN_SESSION.getMode());
    return cluster;
  }

  protected Application mockYarnModeJobApp(
      Long teamId, String name, String yarnQueue, ExecutionMode executionMode) {
    Application application = new Application();
    application.setYarnQueue(yarnQueue);
    application.setTeamId(teamId);
    application.setJobName(name);
    application.setExecutionMode(executionMode.getMode());
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
