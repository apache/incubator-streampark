/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.streamxhub.streamx.console.StreamXConsole;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.service.ApplicationService;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

/**
 * ApplicationServiceTest
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StreamXConsole.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationServiceTest {

    @Autowired
    private ApplicationService applicationService;

    @Test
    public void revokeTest() {
        Date now = new Date();
        Application app = new Application();
        app.setId(100001L);
        app.setJobType(1);
        app.setUserId(100000L);
        app.setTeamId(1L);
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

}
