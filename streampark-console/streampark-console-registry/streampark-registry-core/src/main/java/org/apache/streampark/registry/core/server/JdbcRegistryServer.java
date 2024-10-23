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

package org.apache.streampark.registry.core.server;

import org.apache.streampark.registry.api.enums.RegistryNodeType;
import org.apache.streampark.registry.core.JdbcRegistryProperties;
import org.apache.streampark.registry.core.JdbcRegistryThreadFactory;
import org.apache.streampark.registry.core.model.DTO.JdbcRegistryDataDTO;
import org.apache.streampark.registry.core.repository.JdbcRegistryDataChanceEventRepository;
import org.apache.streampark.registry.core.repository.JdbcRegistryDataRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The JdbcRegistryServer will manage the client, once a client is disconnected, the server will remove the client from the registry, and remove it's related data and lock.
 */
@Slf4j
public class JdbcRegistryServer implements IJdbcRegistryServer {

    private final JdbcRegistryProperties jdbcRegistryProperties;

    private final JdbcRegistryDataManager jdbcRegistryDataManager;

    private JdbcRegistryServerState jdbcRegistryServerState;

    public JdbcRegistryServer(JdbcRegistryDataRepository jdbcRegistryDataRepository,
                              JdbcRegistryDataChanceEventRepository jdbcRegistryDataChanceEventRepository,
                              JdbcRegistryProperties jdbcRegistryProperties) {
        this.jdbcRegistryProperties = checkNotNull(jdbcRegistryProperties);
        this.jdbcRegistryDataManager = new JdbcRegistryDataManager(
            jdbcRegistryProperties, jdbcRegistryDataRepository, jdbcRegistryDataChanceEventRepository);
        this.jdbcRegistryServerState = JdbcRegistryServerState.INIT;
    }

    @Override
    public void start() {
        if (jdbcRegistryServerState != JdbcRegistryServerState.INIT) {
            // The server is already started or stopped, will not start again.
            return;
        }
        jdbcRegistryDataManager.start();
        jdbcRegistryServerState = JdbcRegistryServerState.STARTED;
        JdbcRegistryThreadFactory.getDefaultSchedulerThreadExecutor()
            .scheduleWithFixedDelay(this::checkServersHeartbeat,
                0,
                jdbcRegistryProperties.getHeartbeatRefreshInterval().toMillis(),
                TimeUnit.MILLISECONDS);
    }

    private void checkServersHeartbeat() {
        if (jdbcRegistryServerState == JdbcRegistryServerState.STOPPED) {
            log.warn("The JdbcRegistryServer is STOPPED, will not refresh servers heartbeat.");
            return;
        }
        // Check jdbc registry servers heartbeat
        try {
            long now = System.currentTimeMillis();
            List<JdbcRegistryDataDTO> jdbcRegistryServers =
                listJdbcRegistryDataChildren(RegistryNodeType.CONSOLE_SERVER.getRegistryPath());

            for (JdbcRegistryDataDTO jdbcRegistryServer : jdbcRegistryServers) {
                long lastUpdateTime = jdbcRegistryServer.getLastUpdateTime().getTime();
                if (now - lastUpdateTime > jdbcRegistryProperties.getSessionTimeout().toMillis()) {
                    deleteJdbcRegistryDataByKey(jdbcRegistryServer.getDataKey());
                    log.info("{} has no heartbeat after {}s, will remove it from the servers.",
                        jdbcRegistryServer.getDataKey(), jdbcRegistryProperties.getSessionTimeout());
                }
            }
        } catch (Exception ex) {
            log.error("Failed to check servers heartbeat", ex);
        }
    }

    @Override
    public void subscribeJdbcRegistryDataChange(JdbcRegistryDataChangeListener jdbcRegistryDataChangeListener) {
        checkNotNull(jdbcRegistryDataChangeListener);
        jdbcRegistryDataManager.subscribeRegistryRowChange(
            new IRegistryRowChangeNotifier.RegistryRowChangeListener<JdbcRegistryDataDTO>() {

                @Override
                public void onRegistryRowUpdated(JdbcRegistryDataDTO data) {
                    jdbcRegistryDataChangeListener.onJdbcRegistryDataChanged(data.getDataKey(),
                        data.getDataValue());
                }

                @Override
                public void onRegistryRowAdded(JdbcRegistryDataDTO data) {
                    jdbcRegistryDataChangeListener.onJdbcRegistryDataAdded(data.getDataKey(), data.getDataValue());
                }

                @Override
                public void onRegistryRowDeleted(JdbcRegistryDataDTO data) {
                    jdbcRegistryDataChangeListener.onJdbcRegistryDataDeleted(data.getDataKey());
                }
            });
    }

    @Override
    public Optional<JdbcRegistryDataDTO> getJdbcRegistryDataByKey(String key) {
        return jdbcRegistryDataManager.getRegistryDataByKey(key);
    }

    @Override
    public List<JdbcRegistryDataDTO> listJdbcRegistryDataChildren(String key) {
        return jdbcRegistryDataManager.listJdbcRegistryDataChildren(key);
    }

    @Override
    public void putJdbcRegistryData(String key, String value) {
        jdbcRegistryDataManager.putJdbcRegistryData(key, value);
    }

    @Override
    public void deleteJdbcRegistryDataByKey(String key) {
        jdbcRegistryDataManager.deleteJdbcRegistryDataByKey(key);
    }

    @Override
    public void close() {
        jdbcRegistryServerState = JdbcRegistryServerState.STOPPED;
        JdbcRegistryThreadFactory.getDefaultSchedulerThreadExecutor().shutdown();
    }
}
