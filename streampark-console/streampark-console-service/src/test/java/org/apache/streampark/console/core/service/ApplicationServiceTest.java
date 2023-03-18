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

import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.enums.JobType;
import org.apache.streampark.console.SpringTestBase;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.enums.ReleaseState;
import org.apache.streampark.console.core.enums.ResourceFrom;

import org.apache.http.entity.ContentType;

import org.h2.store.fs.FileUtils;
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

import static org.assertj.core.api.Assertions.assertThat;

/** org.apache.streampark.console.core.service.ApplicationServiceTest. */
class ApplicationServiceTest extends SpringTestBase {

  @Autowired private ApplicationService applicationService;

  @Test
  void testRevoke() {
    Date now = new Date();
    Application app = new Application();
    app.setId(100001L);
    app.setJobType(JobType.CUSTOM_CODE);
    app.setUserId(100000L);
    app.setJobName("socket-test");
    app.setVersionId(1L);
    app.setK8sNamespace("default");
    app.setState(FlinkAppState.ADDED);
    app.setRelease(ReleaseState.RELEASING);
    app.setBuild(true);
    app.setRestartSize(0);
    app.setOptionState(OptionState.NONE);
    app.setArgs("--hostname hadoop001 --port 8111");
    app.setOptions("{\"taskmanager.numberOfTaskSlots\":1,\"parallelism.default\":1}");
    app.setResolveOrder(0);
    app.setExecutionMode(ExecutionMode.YARN_APPLICATION);
    app.setAppType(ApplicationType.APACHE_FLINK);
    app.setTracking(0);
    app.setJar("SocketWindowWordCount.jar");
    app.setJarCheckSum(1553115525L);
    app.setMainClass("org.apache.flink.streaming.examples.socket.SocketWindowWordCount");
    app.setCreateTime(now);
    app.setModifyTime(now);
    app.setResourceFrom(ResourceFrom.UPLOAD);
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
}
