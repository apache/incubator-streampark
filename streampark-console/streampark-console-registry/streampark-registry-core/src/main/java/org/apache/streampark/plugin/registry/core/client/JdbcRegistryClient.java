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

package org.apache.streampark.plugin.registry.core.client;

import org.apache.streampark.common.utils.NetworkUtils;
import org.apache.streampark.common.utils.OSUtils;
import org.apache.streampark.common.utils.UUIDUtils;
import org.apache.streampark.plugin.registry.core.JdbcRegistryProperties;
import org.apache.streampark.plugin.registry.core.model.DTO.DataType;
import org.apache.streampark.plugin.registry.core.model.DTO.JdbcRegistryDataDTO;
import org.apache.streampark.plugin.registry.core.server.ConnectionStateListener;
import org.apache.streampark.plugin.registry.core.server.IJdbcRegistryServer;
import org.apache.streampark.plugin.registry.core.server.JdbcRegistryDataChangeListener;
import org.apache.streampark.plugin.registry.core.server.JdbcRegistryServerState;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * The client of jdbc registry, used to interact with the {@link org.apache.streampark.plugin.registry.core.server.JdbcRegistryServer}.
 */
@Slf4j
public class JdbcRegistryClient implements IJdbcRegistryClient {

    private static final String DEFAULT_CLIENT_NAME = NetworkUtils.getHost() + "_" + OSUtils.getProcessID();

    private final JdbcRegistryProperties jdbcRegistryProperties;

    private final JdbcRegistryClientIdentify jdbcRegistryClientIdentify;

    private final IJdbcRegistryServer jdbcRegistryServer;

    public JdbcRegistryClient(JdbcRegistryProperties jdbcRegistryProperties, IJdbcRegistryServer jdbcRegistryServer) {
        this.jdbcRegistryProperties = jdbcRegistryProperties;
        this.jdbcRegistryServer = jdbcRegistryServer;
        this.jdbcRegistryClientIdentify =
            new JdbcRegistryClientIdentify(UUIDUtils.generateUUID(), DEFAULT_CLIENT_NAME);
    }

    @Override
    public void start() {
        jdbcRegistryServer.registerClient(this);
    }

    @Override
    public JdbcRegistryClientIdentify getJdbcRegistryClientIdentify() {
        return jdbcRegistryClientIdentify;
    }

    @Override
    public void subscribeConnectionStateChange(ConnectionStateListener connectionStateListener) {
        jdbcRegistryServer.subscribeConnectionStateChange(connectionStateListener);
    }

    @Override
    public void subscribeJdbcRegistryDataChange(JdbcRegistryDataChangeListener jdbcRegistryDataChangeListener) {
        jdbcRegistryServer.subscribeJdbcRegistryDataChange(jdbcRegistryDataChangeListener);
    }

    @Override
    public Optional<JdbcRegistryDataDTO> getJdbcRegistryDataByKey(String key) {
        return jdbcRegistryServer.getJdbcRegistryDataByKey(key);
    }

    @Override
    public void putJdbcRegistryData(String key, String value, DataType dataType) {
        jdbcRegistryServer.putJdbcRegistryData(jdbcRegistryClientIdentify.getClientId(), key, value, dataType);
    }

    @Override
    public void deleteJdbcRegistryDataByKey(String key) {
        jdbcRegistryServer.deleteJdbcRegistryDataByKey(key);
    }

    @Override
    public List<JdbcRegistryDataDTO> listJdbcRegistryDataChildren(String key) {
        return jdbcRegistryServer.listJdbcRegistryDataChildren(key);
    }

    @Override
    public boolean existJdbcRegistryDataKey(String key) {
        return jdbcRegistryServer.existJdbcRegistryDataKey(key);
    }

    @Override
    public void acquireJdbcRegistryLock(String key) {
        jdbcRegistryServer.acquireJdbcRegistryLock(jdbcRegistryClientIdentify.getClientId(), key);
    }

    @Override
    public boolean acquireJdbcRegistryLock(String key, long timeout) {
        return jdbcRegistryServer.acquireJdbcRegistryLock(jdbcRegistryClientIdentify.getClientId(), key, timeout);
    }

    @Override
    public void releaseJdbcRegistryLock(String key) {
        jdbcRegistryServer.releaseJdbcRegistryLock(jdbcRegistryClientIdentify.getClientId(), key);
    }

    @Override
    public void close() {
        jdbcRegistryServer.deregisterClient(this);
        log.info("Closed JdbcRegistryClient: {}", jdbcRegistryClientIdentify);
    }

    @Override
    public boolean isConnectivity() {
        return jdbcRegistryServer.getServerState() == JdbcRegistryServerState.STARTED;
    }

}
