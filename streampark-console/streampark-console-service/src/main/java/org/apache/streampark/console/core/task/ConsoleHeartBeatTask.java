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

package org.apache.streampark.console.core.task;

import org.apache.streampark.common.utils.JSONUtils;
import org.apache.streampark.common.utils.NetworkUtils;
import org.apache.streampark.console.core.config.ConsoleConfig;
import org.apache.streampark.registry.api.RegistryClient;
import org.apache.streampark.registry.api.lifecycle.ServerLifeCycleManager;
import org.apache.streampark.registry.api.model.BaseHeartBeatTask;
import org.apache.streampark.registry.api.model.ConsoleHeartBeat;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleHeartBeatTask extends BaseHeartBeatTask<ConsoleHeartBeat> {

    private final ConsoleConfig consoleConfig;

    private final RegistryClient registryClient;

    private final String heartBeatPath;

    public ConsoleHeartBeatTask(@NonNull ConsoleConfig consoleConfig,
                                @NonNull RegistryClient registryClient) {
        super("ConsoleHeartBeatTask", consoleConfig.getMaxHeartbeatInterval().toMillis());
        this.consoleConfig = consoleConfig;
        this.registryClient = registryClient;
        this.heartBeatPath = consoleConfig.getConsoleRegistryPath();
    }

    @Override
    public ConsoleHeartBeat getHeartBeat() {
        return ConsoleHeartBeat.builder()
            .startupTime(ServerLifeCycleManager.getServerStartupTime())
            .reportTime(System.currentTimeMillis())
            .host(NetworkUtils.getHost())
            .port(consoleConfig.getListenPort())
            .build();
    }

    @Override
    public void writeHeartBeat(ConsoleHeartBeat consoleHeartBeat) {
        String masterHeartBeatJson = JSONUtils.toJsonString(consoleHeartBeat);
        registryClient.put(heartBeatPath, masterHeartBeatJson);
        log.debug("Success write master heartBeatInfo into registry, masterRegistryPath: {}, heartBeatInfo: {}",
            heartBeatPath, masterHeartBeatJson);
    }
}
