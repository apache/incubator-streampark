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

package org.apache.streampark.console.core.config;

import org.apache.streampark.common.utils.NetworkUtils;
import org.apache.streampark.registry.api.ConnectStrategyProperties;
import org.apache.streampark.registry.api.enums.RegistryNodeType;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "server")
@Slf4j
public class ConsoleConfig implements Validator {

    /**
     * The console server listen port.
     */
    @Value("${server.port:10000}")
    private int listenPort = 10000;

    /**
     * Console heart beat task execute interval.
     */
    @Value("${streampark.max-heartbeat-interval:10}")
    private Duration maxHeartbeatInterval = Duration.ofSeconds(10);

    private ConnectStrategyProperties registryDisconnectStrategy = new ConnectStrategyProperties();

    /**
     * The IP address and listening port of the console server in the format 'ip:listenPort'.
     */
    private String consoleAddress;

    /**
     * The registry path for the console server in the format '/nodes/console/ip:listenPort'.
     */
    private String consoleRegistryPath;

    @Override
    public boolean supports(Class<?> clazz) {
        return ConsoleConfig.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ConsoleConfig consoleConfig = (ConsoleConfig) target;
        if (consoleConfig.getListenPort() <= 0) {
            errors.rejectValue("listen-port", null, "is invalidated");
        }
        if (consoleConfig.getMaxHeartbeatInterval().toMillis() < 0) {
            errors.rejectValue("max-heartbeat-interval", null, "should be a valid duration");
        }
        if (StringUtils.isEmpty(consoleConfig.getConsoleAddress())) {
            consoleConfig.setConsoleAddress(NetworkUtils.getAddr(consoleConfig.getListenPort()));
        }

        consoleConfig.setConsoleRegistryPath(
            RegistryNodeType.CONSOLE_SERVER.getRegistryPath() + "/" + consoleConfig.getConsoleAddress());

        printConfig();
    }

    private void printConfig() {
        String config =
            "\n****************************Console Configuration**************************************" +
                "\n  listen-port -> " + listenPort +
                "\n  registry-disconnect-strategy -> " + registryDisconnectStrategy +
                "\n  console-address -> " + consoleAddress +
                "\n  console-registry-path: " + consoleRegistryPath +
                "\n****************************Master Configuration**************************************";
        log.info(config);
    }
}
