/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.console.system.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Order
@Slf4j
@Component
public class StartedUpRunner implements ApplicationRunner {

    @Autowired
    private ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        if (context.isActive()) {
            log.info("                          ▒▓██▓██▒                          ");
            log.info("                      ▓████▒▒█▓▒▓███▓▒                      ");
            log.info("                   ▓███▓░░        ▒▒▒▓██▒  ▒                ");
            log.info("                 ░██▒   ▒▒▓▓█▓▓▒░      ▒████                ");
            log.info("                 ██▒         ░▒▓███▒    ▒█▒█▒               ");
            log.info("                   ░▓█            ███   ▓░▒██               ");
            log.info("                     ▓█       ▒▒▒▒▒▓██▓░▒░▓▓█               ");
            log.info("                   █░ █   ▒▒░       ███▓▓█ ▒█▒▒▒            ");
            log.info("                   ████░   ▒▓█▓      ██▒▒▒ ▓███▒            ");
            log.info("                ░▒█▓▓██       ▓█▒    ▓█▒▓██▓ ░█░            ");
            log.info("          ▓░▒▓████▒ ██         ▒█    █▓░▒█▒░▒█▒             ");
            log.info("         ███▓░██▓  ▓█           █   █▓ ▒▓█▓▓█▒              ");
            log.info("       ░██▓  ░█░            █  █▒ ▒█████▓▒ ██▓░▒            ");
            log.info("      ███░ ░ █░          ▓ ░█ █████▒░░    ░█░▓  ▓░          ");
            log.info("     ██▓█ ▒▒▓▒          ▓███████▓░       ▒█▒ ▒▓ ▓██▓        ");
            log.info("  ▒██▓ ▓█ █▓█       ░▒█████▓▓▒░         ██▒▒  █ ▒  ▓█▒      ");
            log.info("  ▓█▓  ▓█ ██▓ ░▓▓▓▓▓▓▓▒              ▒██▓           ░█▒     ");
            log.info("  ▓█    █ ▓███▓▒░              ░▓▓▓███▓          ░▒░ ▓█     ");
            log.info("  ██▓    ██▒    ░▒▓▓███▓▓▓▓▓██████▓▒            ▓███  █     ");
            log.info(" ▓███▒ ███   ░▓▓▒░░   ░▓████▓░                  ░▒▓▒  █▓    ");
            log.info(" █▓▒▒▓▓██  ░▒▒░░░▒▒▒▒▓██▓░                            █▓    ");
            log.info(" ██ ▓░▒█   ▓▓▓▓▒░░  ▒█▓       ▒▓▓██▓    ▓▒          ▒▒▓     ");
            log.info(" ▓█▓ ▓▒█  █▓░  ░▒▓▓██▒            ░▓█▒   ▒▒▒░▒▒▓█████▒      ");
            log.info("  ██░ ▓█▒█▒  ▒▓▓▒  ▓█                █░      ░░░░   ░█▒     ");
            log.info("  ▓█   ▒█▓   ░     █░                ▒█              █▓     ");
            log.info("   █▓   ██         █░                 ▓▓        ▒█▓▓▓▒█░    ");
            log.info("    █▓ ░▓██░       ▓▒                  ▓█▓▒░░░▒▓█░    ▒█    ");
            log.info("     ██   ▓█▓░      ▒                    ░▒█▒██▒      ▓▓    ");
            log.info("      ▓█▒   ▒█▓▒░                         ▒▒ █▒█▓▒▒░░▒██    ");
            log.info("       ░██▒    ▒▓▓▒                     ▓██▓▒█▒ ░▓▓▓▓▒█▓    ");
            log.info("         ░▓██▒                          ▓░  ▒█▓█  ░░▒▒▒     ");
            log.info("             ▒▓▓▓▓▓▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒░░▓▓  ▓░▒█░       ");
            log.info("                                                            ");
            log.info("             +----------------------+                       ");
            log.info("             +  十步杀一人，千里不留行  +                       ");
            log.info("             +  事了拂衣去，深藏身与名  +                       ");
            log.info("             +----------------------+                       ");
            log.info("                                                            ");
            log.info("            [StreamX] let's flink|spark easy ô‿ô!           ");
            log.info("            StreamX 启动完毕，时间：" + LocalDateTime.now());
        }
    }
}
