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

package org.apache.streampark.console.core.service;

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.console.SpringTestBase;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.YarnQueue;
import org.apache.streampark.console.core.service.impl.ApplicationServiceImpl;
import org.apache.streampark.console.core.service.impl.YarnQueueServiceImpl;

import org.apache.http.entity.ContentType;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Date;

import static org.apache.streampark.console.core.service.impl.ApplicationServiceImpl.ERROR_APP_QUEUE_HINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** org.apache.streampark.console.core.service.ApplicationServiceTest. */
class ApplicationServiceTest extends SpringTestBase {

  @Autowired private ApplicationService applicationService;

  @Autowired private YarnQueueService yarnQueueService;

  @AfterEach
  void cleanTestRecordsInDatabase() {
    applicationService.remove(new QueryWrapper<>());
    yarnQueueService.remove(new QueryWrapper<>());
  }

  @Test
  void testRevoke() {
    Date now = new Date();
    Application app = new Application();
    app.setId(100001L);
    app.setJobType(1);
    app.setUserId(100000L);
    app.setJobName("socket-test");
    app.setVersionId(1L);
    app.setK8sNamespace("default");
    app.setState(0);
    app.setRelease(2);
    app.setBuild(true);
    app.setRestartSize(0);
    app.setOptionState(0);
    app.setArgs("--hostname hadoop001 --port 8111");
    app.setOptions("{\"taskmanager.numberOfTaskSlots\":1,\"parallelism.default\":1}");
    app.setResolveOrder(0);
    app.setExecutionMode(4);
    app.setAppType(2);
    app.setTracking(0);
    app.setJar("SocketWindowWordCount.jar");
    app.setJarCheckSum(1553115525L);
    app.setMainClass("org.apache.flink.streaming.examples.socket.SocketWindowWordCount");
    app.setCreateTime(now);
    app.setModifyTime(now);
    app.setResourceFrom(2);
    app.setK8sHadoopIntegration(false);
    app.setBackUp(false);
    app.setRestart(false);
    app.setSavePointed(false);
    app.setDrain(false);
    app.setAllowNonRestored(false);

    Assertions.assertDoesNotThrow(() -> applicationService.updateRelease(app));
  }

  @Test
  @Disabled("We couldn't do integration test with external services or components.")
  void testStart() throws Exception {
    Application application = new Application();
    application.setId(1304056220683497473L);
    application.setRestart(false);
    application.setSavePointed(false);
    application.setAllowNonRestored(false);

    applicationService.start(application, false);
  }

  @Test
  void testUpload(@TempDir Path tempDir) throws Exception {
    // specify the file path
    File fileToStoreUploadFile =
        new File(tempDir.toFile().getAbsolutePath() + "/fileToStoreUploadFile");
    FileUtils.createFile(fileToStoreUploadFile.getAbsolutePath());

    File fileToUpload = new File(tempDir.toFile().getAbsolutePath() + "/fileToUpload.jar");
    FileUtils.createFile(fileToUpload.getAbsolutePath());
    assertThat(fileToUpload).exists();
    MultipartFile mulFile =
        new MockMultipartFile(
            "test", // fileName (eg: streampark.jar)
            fileToUpload.getAbsolutePath(), // originalFilename (eg: path + fileName =
            // /tmp/file/streampark.jar)
            ContentType.APPLICATION_OCTET_STREAM.toString(),
            new FileInputStream(fileToStoreUploadFile));
    applicationService.upload(mulFile);
  }

  @Test
  void testIsYarnPerJobAppModeNotDefaultQueue() {

    ApplicationServiceImpl applicationServiceImpl = (ApplicationServiceImpl) applicationService;

    // Test for app not (yarn per-job or application-mode)
    Application app = new Application();
    app.setExecutionMode(ExecutionMode.REMOTE.getMode());
    assertThat(applicationServiceImpl.isYarnPerJobAppModeNotDefaultQueue(app)).isFalse();

    // Test for app with empty & default queue in (yarn per-job or application-mode)
    app.setExecutionMode(ExecutionMode.YARN_PER_JOB.getMode());
    assertThat(applicationServiceImpl.isYarnPerJobAppModeNotDefaultQueue(app)).isFalse();

    app.setExecutionMode(ExecutionMode.YARN_APPLICATION.getMode());
    assertThat(applicationServiceImpl.isYarnPerJobAppModeNotDefaultQueue(app)).isFalse();

    app.setExecutionMode(ExecutionMode.YARN_PER_JOB.getMode());
    app.setYarnQueue(YarnQueueServiceImpl.DEFAULT_QUEUE);
    assertThat(applicationServiceImpl.isYarnPerJobAppModeNotDefaultQueue(app)).isFalse();

    app.setExecutionMode(ExecutionMode.YARN_APPLICATION.getMode());
    app.setYarnQueue(YarnQueueServiceImpl.DEFAULT_QUEUE);
    assertThat(applicationServiceImpl.isYarnPerJobAppModeNotDefaultQueue(app)).isFalse();

    // Test for app with non-empty & non-default queue in (yarn per-job or application-mode)
    app.setExecutionMode(ExecutionMode.YARN_APPLICATION.getMode());
    app.setYarnQueue("testQueue");
    assertThat(applicationServiceImpl.isYarnPerJobAppModeNotDefaultQueue(app)).isTrue();
  }

  @Test
  void testCheckQueueValidationIfNeeded() {
    ApplicationServiceImpl applicationServiceImpl = (ApplicationServiceImpl) applicationService;

    // ------- Test it for the create operation. -------
    final String queueLabel = "queue1@label1";
    final Long targetTeamId = 1L;

    // Test application with available queue
    YarnQueue yarnQueue = mockYarnQueue(targetTeamId, queueLabel);
    yarnQueueService.save(yarnQueue);
    Application application =
        mockYarnModeJobApp(targetTeamId, "app1", queueLabel, ExecutionMode.YARN_APPLICATION);
    applicationServiceImpl.checkQueueValidationIfNeeded(application);

    // Test application without available queue
    application.setYarnQueue("non-exited-queue");
    assertThatThrownBy(() -> applicationServiceImpl.checkQueueValidationIfNeeded(application))
        .isInstanceOf(ApiAlertException.class)
        .hasMessage(
            String.format(
                ERROR_APP_QUEUE_HINT, application.getYarnQueue(), application.getTeamId()));

    // ------- Test it for the update operation. -------
    final String queueLabel1 = "queue1@label1";
    final String queueLabel2 = "queue1@label2";
    final String nonExistedQueue = "nonExistedQueue";
    final String appName = "app1";
    final Long teamId2 = 2L;

    // Test update for both versions in yarn-app or per-job with same yarn queue
    Application app1 =
        mockYarnModeJobApp(teamId2, appName, queueLabel1, ExecutionMode.YARN_APPLICATION);
    Application app2 =
        mockYarnModeJobApp(teamId2, appName, queueLabel1, ExecutionMode.YARN_PER_JOB);
    applicationServiceImpl.checkQueueValidationIfNeeded(app1, app2);

    // Test available queue
    YarnQueue yarnQueueLabel1 = mockYarnQueue(teamId2, queueLabel1);
    yarnQueueService.save(yarnQueueLabel1);
    YarnQueue yarnQueueLabel2 = mockYarnQueue(teamId2, queueLabel2);
    yarnQueueService.save(yarnQueueLabel2);
    app2.setYarnQueue(queueLabel2);
    applicationServiceImpl.checkQueueValidationIfNeeded(app1, app2);

    // Test non-existed queue
    app1.setExecutionMode(ExecutionMode.KUBERNETES_NATIVE_APPLICATION.getMode());
    app2.setYarnQueue(nonExistedQueue);
    assertThatThrownBy(() -> applicationServiceImpl.checkQueueValidationIfNeeded(app1, app2))
        .isInstanceOf(ApiAlertException.class)
        .hasMessage(String.format(ERROR_APP_QUEUE_HINT, app2.getYarnQueue(), app2.getTeamId()));
  }
}
