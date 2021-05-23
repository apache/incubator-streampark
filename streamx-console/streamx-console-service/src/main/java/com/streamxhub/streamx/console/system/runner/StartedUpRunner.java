/*
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
package com.streamxhub.streamx.console.system.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author benjobs
 */
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
            log.info("              ____ __                          _  __        ");
            log.info("             / __// /_ ____ ___  ___ _ __ _   | |/_/        ");
            log.info("            _\\ \\ / __// __// -_)/ _ `//  ' \\ _>  <       ");
            log.info("           /___/ \\__//_/   \\__/ \\_,_//_/_/_//_/|_|       ");
            log.info("                                                            ");
            log.info("            [StreamX] Make Flink|Spark easier ô‿ô!           ");
            log.info("                                                            ");
            log.info("            StreamX 启动完毕，时间：" + LocalDateTime.now());
        }
    }
}
