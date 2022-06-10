/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console;

import com.streamxhub.streamx.common.util.SystemPropertyUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * <pre>
 *
 *               .+.
 *         _____/ /_________  ____ _____ ___  _  __
 *        / ___/ __/ ___/ _ \/ __ `/ __ `__ \| |/_/
 *       (__  ) /_/ /  /  __/ /_/ / / / / / />   <
 *      /____/\__/_/   \___/\__,_/_/ /_/ /_/_/|_|
 *                                            |/
 *                                            .
 *
 *      WebSite:  http://www.streamxhub.com
 *      GitHub :  https://github.com/streamxhub/streamx
 *      Gitee  :  https://gitee.com/streamxhub/streamx
 *
 *      [StreamX] Make stream processing easier ô~ô!
 *
 *      十步杀一人 千里不留行 事了拂衣去 深藏身与名
 *
 * </pre>
 *
 * @author benjobs
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class StreamXConsole {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(StreamXConsole.class);
        String pid = SystemPropertyUtils.get("pid");
        if (pid != null) {
            application.addListeners(new ApplicationPidFileWriter(pid));
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("application shutdown now, pid: " + getPid());
            if (pid != null) {
                File pidFile = new File(pid);
                pidFile.delete();
            }
        }));

        application.run();
    }

    private static Integer getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

}
