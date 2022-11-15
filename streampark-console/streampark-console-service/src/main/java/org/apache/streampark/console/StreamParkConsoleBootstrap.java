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

import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.console.base.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

/**
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
 *
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class StreamParkConsoleBootstrap {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(StreamParkConsoleBootstrap.class);
        String pid = SystemPropertyUtils.get("pid");
        if (pid != null) {
            application.addListeners(new ApplicationPidFileWriter(pid));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("application shutdown now, pid: " + CommonUtils.getPid());
                File pidFile = new File(pid);
                pidFile.delete();
            }));
        }
        application.run();
    }

}
