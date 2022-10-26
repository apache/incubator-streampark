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

import org.apache.streampark.console.StreamParkConsoleBootstrap;
import org.apache.streampark.console.core.entity.Application;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;

/**
 * org.apache.streampark.console.core.service.ApplicationServiceTest
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = StreamParkConsoleBootstrap.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationServiceTest {

    @Autowired
    private ApplicationService applicationService;

    @Test
    void revokeTest() {
        Date now = new Date();
        Application app = new Application();
        app.setId(100001L);
        app.setJobType(1);
        app.setUserId(100000L);
        app.setJobName("socket-test");
        app.setVersionId(1L);
        app.setK8sNamespace("default");
        app.setState(0);
        app.setLaunch(2);
        app.setBuild(true);
        app.setRestartSize(0);
        app.setOptionState(0);
        app.setArgs("--hostname hadoop001 --port 8111");
        app.setOptions("{\"taskmanager.numberOfTaskSlots\":1,\"parallelism.default\":1}");
        app.setResolveOrder(0);
        app.setExecutionMode(4);
        app.setAppType(2);
        app.setFlameGraph(false);
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

        Assertions.assertDoesNotThrow(() -> applicationService.updateLaunch(app));
    }

    @Test
    void start() throws Exception {
        Application application = new Application();
        application.setId(1304056220683497473L);
        application.setFlameGraph(false);
        application.setRestart(false);
        application.setSavePointed(false);
        application.setAllowNonRestored(false);

        applicationService.start(application, false);
    }

    @Test
    void uploadTest() throws Exception {
        File file = new File(""); // specify the file path
        MultipartFile mulFile = new MockMultipartFile(
            "", // fileName (eg: streampark.jar)
            "", // originalFilename (eg: path + fileName = /tmp/file/streampark.jar)
            ContentType.APPLICATION_OCTET_STREAM.toString(),
            new FileInputStream(file)
        );
        applicationService.upload(mulFile);
    }
}
