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

import org.apache.streampark.common.CommonConfiguration;
import org.apache.streampark.common.IStoppable;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.console.base.config.SpringProperties;
import org.apache.streampark.console.base.util.SpringContextUtils;
import org.apache.streampark.console.core.registry.ConsoleRegistryClient;
import org.apache.streampark.registry.api.RegistryConfiguration;
import org.apache.streampark.registry.api.lifecycle.ServerLifeCycleManager;
import org.apache.streampark.registry.api.thread.ThreadUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

/**
 *
 *
 * <pre>
 *
 *      _____ __                                             __
 *     / ___// /_________  ____ _____ ___  ____  ____ ______/ /__
 *     \__ \/ __/ ___/ _ \/ __ `/ __ `__ \/ __ \  __ `/ ___/ //_/
 *    ___/ / /_/ /  /  __/ /_/ / / / / / / /_/ / /_/ / /  / ,<
 *   /____/\__/_/   \___/\__,_/_/ /_/ /_/ ____/\__,_/_/  /_/|_|
 *                                     /_/
 *
 *   WebSite:  https://streampark.apache.org
 *   GitHub :  https://github.com/apache/incubator-streampark
 *
 *   [StreamPark] Make stream processing easier ô~ô!
 *
 * </pre>
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
@Import({CommonConfiguration.class,
        RegistryConfiguration.class})
public class StreamParkConsoleBootstrap implements IStoppable {

    @Autowired
    private ConsoleRegistryClient consoleRegistryClient;

    @Autowired
    private SpringContextUtils springContextUtils;

    public static void main(String[] args) throws Exception {
        new SpringApplicationBuilder()
            .properties(SpringProperties.get())
            .sources(StreamParkConsoleBootstrap.class)
            .run(args);
    }

    @PostConstruct
    public void run() {
        consoleRegistryClient.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!ServerLifeCycleManager.isStopped()) {
                close("ConsoleServer shutdownHook");
            }
        }));
    }

    /**
     * gracefully close console server
     *
     * @param cause close cause
     */
    public void close(String cause) {
        // set stop signal is true
        // execute only once
        if (!ServerLifeCycleManager.toStopped()) {
            log.warn("MasterServer is already stopped, current cause: {}", cause);
            return;
        }
        // thread sleep 3 seconds for thread quietly stop
        ThreadUtils.sleep(Constants.SERVER_CLOSE_WAIT_TIME.toMillis());
        try (ConsoleRegistryClient closedMasterRegistryClient = consoleRegistryClient) {
            // todo: close other resources
            springContextUtils.close();
            log.info("Master server is stopping, current cause : {}", cause);
        } catch (Exception e) {
            log.error("MasterServer stop failed, current cause: {}", cause, e);
            return;
        }
        log.info("MasterServer stopped, current cause: {}", cause);
    }

    @Override
    public void stop(String cause) {
        close(cause);

        // make sure exit after server closed, don't call System.exit in close logic, will cause deadlock if close
        // multiple times at the same time
        System.exit(1);
    }
}
