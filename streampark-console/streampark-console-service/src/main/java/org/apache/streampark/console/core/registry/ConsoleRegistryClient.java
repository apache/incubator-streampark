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

package org.apache.streampark.console.core.registry;

import org.apache.streampark.common.utils.JSONUtils;
import org.apache.streampark.common.utils.NetworkUtils;
import org.apache.streampark.console.core.config.ConsoleConfig;
import org.apache.streampark.console.core.service.DistributedTaskService;
import org.apache.streampark.console.core.task.ConsoleHeartBeatTask;
import org.apache.streampark.registry.api.RegistryClient;
import org.apache.streampark.registry.api.RegistryException;
import org.apache.streampark.registry.api.enums.RegistryNodeType;
import org.apache.streampark.registry.api.thread.ThreadUtils;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.apache.streampark.common.constants.Constants.SLEEP_TIME_MILLIS;

/**
 * <p>streampark console register client, used to connect to registry and hand the registry events.
 * <p>When the console node startup, it will register in registry center. And start a {@link ConsoleHeartBeatTask} to update its metadata in registry.
 */
@Component
@Slf4j
public class ConsoleRegistryClient implements AutoCloseable {

    @Autowired
    private RegistryClient registryClient;

    @Autowired
    private ConsoleConfig consoleConfig;

    @Autowired
    private DistributedTaskService distributedTaskService;

    private ConsoleHeartBeatTask consoleHeartBeatTask;

    public void start() {
        try {
            log.info("consoleConfig: {}", consoleConfig);
            this.consoleHeartBeatTask = new ConsoleHeartBeatTask(consoleConfig, registryClient);
            // console registry
            registry();
            registryClient.subscribe(RegistryNodeType.ALL_SERVERS.getRegistryPath(), new ConsoleRegistryDataListener());
        } catch (Exception e) {
            throw new RegistryException("Console registry client start up error", e);
        }
    }

    @Override
    public void close() {
        // TODO unsubscribe ConsoleRegistryDataListener
        log.info("ConsoleRegistryClient close");
        deregister();
    }

    /**
     * add console node path
     *
     * @param path     node path
     * @param nodeType node type
     */
    public void addConsoleNodePath(String path, RegistryNodeType nodeType) {
        log.info("{} node added : {}", nodeType, path);

        if (StringUtils.isEmpty(path)) {
            log.error("server start error: empty path: {}, nodeType:{}", path, nodeType);
            return;
        }

        String serverHost = registryClient.getHostByEventDataPath(path);
        if (StringUtils.isEmpty(serverHost)) {
            log.error("server start error: unknown path: {}, nodeType:{}", path, nodeType);
            return;
        }

        try {
            if (!registryClient.exists(path)) {
                log.info("path: {} not exists", path);
            }
            distributedTaskService.addServer(serverHost);
        } catch (Exception e) {
            log.error("{} server failover failed, host:{}", nodeType, serverHost, e);
        }
    }

    /**
     * remove console node path
     *
     * @param path     node path
     * @param nodeType node type
     * @param failover is failover
     */
    public void removeConsoleNodePath(String path, RegistryNodeType nodeType, boolean failover) {
        log.info("{} node deleted : {}", nodeType, path);

        if (StringUtils.isEmpty(path)) {
            log.error("server down error: empty path: {}, nodeType:{}", path, nodeType);
            return;
        }

        String serverHost = registryClient.getHostByEventDataPath(path);
        if (StringUtils.isEmpty(serverHost)) {
            log.error("server down error: unknown path: {}, nodeType:{}", path, nodeType);
            return;
        }

        try {
            if (!registryClient.exists(path)) {
                log.info("path: {} not exists", path);
            }
            // todo: add failover logic here
        } catch (Exception e) {
            log.error("{} server failover failed, host:{}", nodeType, serverHost, e);
        }
    }

    /**
     * Registry the current console server itself to registry.
     */
    void registry() {
        log.info("Console node : {} registering to registry center", consoleConfig.getConsoleAddress());
        String consoleRegistryPath = consoleConfig.getConsoleRegistryPath();

        // delete before persist
        registryClient.delete(consoleRegistryPath);
        registryClient.put(consoleRegistryPath,
            JSONUtils.toJsonString(consoleHeartBeatTask.getHeartBeat()));

        // waiting for the console server node to be registered
        while (!registryClient.checkNodeExists(NetworkUtils.getHost(), RegistryNodeType.CONSOLE_SERVER)) {
            log.warn("The current console server node:{} cannot find in registry", NetworkUtils.getHost());
            ThreadUtils.sleep(SLEEP_TIME_MILLIS);
        }

        // sleep 1s, waiting console failover remove
        ThreadUtils.sleep(SLEEP_TIME_MILLIS);

        // start console heart beat task
        consoleHeartBeatTask.start();

        Set<String> allServers = registryClient.getServerNodeSet(RegistryNodeType.CONSOLE_SERVER);
        distributedTaskService.init(allServers, consoleConfig.getConsoleAddress());
        log.info("Console node : {} registered to registry center successfully", consoleConfig.getConsoleAddress());
    }

    public void deregister() {
        try {
            registryClient.delete(consoleConfig.getConsoleRegistryPath());
            log.info("Console node : {} unRegistry to register center.", consoleConfig.getConsoleAddress());
            if (consoleHeartBeatTask != null) {
                consoleHeartBeatTask.shutdown();
            }
            registryClient.close();
        } catch (Exception e) {
            log.error("ConsoleServer remove registry path exception ", e);
        }
    }
}
