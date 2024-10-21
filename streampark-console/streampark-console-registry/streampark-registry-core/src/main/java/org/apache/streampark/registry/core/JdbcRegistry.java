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

package org.apache.streampark.registry.core;

import org.apache.streampark.registry.api.Event;
import org.apache.streampark.registry.api.Registry;
import org.apache.streampark.registry.api.RegistryException;
import org.apache.streampark.registry.api.SubscribeListener;
import org.apache.streampark.registry.core.model.DTO.JdbcRegistryDataDTO;
import org.apache.streampark.registry.core.server.IJdbcRegistryServer;
import org.apache.streampark.registry.core.server.JdbcRegistryDataChangeListener;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is one of the implementation of {@link Registry}, with this implementation, you need to rely on mysql database to
 * store the streampark master/worker's metadata and do the server registry/unRegistry.
 */
@Slf4j
public final class JdbcRegistry implements Registry {

    private final IJdbcRegistryServer jdbcRegistryServer;

    JdbcRegistry(IJdbcRegistryServer jdbcRegistryServer) {
        this.jdbcRegistryServer = jdbcRegistryServer;
        log.info("Initialize Jdbc Registry...");
    }

    @Override
    public void start() {
        log.info("Starting Jdbc Registry...");
        jdbcRegistryServer.start();
        log.info("Started Jdbc Registry...");
    }

    @Override
    public void subscribe(String path, SubscribeListener listener) {
        checkNotNull(path);
        checkNotNull(listener);
        jdbcRegistryServer.subscribeJdbcRegistryDataChange(new JdbcRegistryDataChangeListener() {

            @Override
            public void onJdbcRegistryDataChanged(String key, String value) {
                if (!key.startsWith(path)) {
                    return;
                }
                Event event = Event.builder()
                    .key(key)
                    .path(path)
                    .data(value)
                    .type(Event.Type.UPDATE)
                    .build();
                listener.notify(event);
            }

            @Override
            public void onJdbcRegistryDataDeleted(String key) {
                if (!key.startsWith(path)) {
                    return;
                }
                Event event = Event.builder()
                    .key(key)
                    .path(key)
                    .type(Event.Type.REMOVE)
                    .build();
                listener.notify(event);
            }

            @Override
            public void onJdbcRegistryDataAdded(String key, String value) {
                if (!key.startsWith(path)) {
                    return;
                }
                Event event = Event.builder()
                    .key(key)
                    .path(key)
                    .data(value)
                    .type(Event.Type.ADD)
                    .build();
                listener.notify(event);
            }
        });
    }

    @Override
    public String get(String key) {
        try {
            // get the key value
            // Directly get from the db?
            Optional<JdbcRegistryDataDTO> jdbcRegistryDataOptional = jdbcRegistryServer.getJdbcRegistryDataByKey(key);
            if (!jdbcRegistryDataOptional.isPresent()) {
                throw new RegistryException("key: " + key + " not exist");
            }
            return jdbcRegistryDataOptional.get().getDataValue();
        } catch (RegistryException registryException) {
            throw registryException;
        } catch (Exception e) {
            throw new RegistryException(String.format("Get key: %s error", key), e);
        }
    }

    @Override
    public void put(String key, String value) {
        try {
            jdbcRegistryServer.putJdbcRegistryData(key, value);
        } catch (Exception ex) {
            throw new RegistryException(String.format("put key:%s, value:%s error", key, value), ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            jdbcRegistryServer.deleteJdbcRegistryDataByKey(key);
        } catch (Exception e) {
            throw new RegistryException(String.format("Delete key: %s error", key), e);
        }
    }

    @Override
    public Collection<String> children(String key) {
        try {
            List<JdbcRegistryDataDTO> children = jdbcRegistryServer.listJdbcRegistryDataChildren(key);
            return children
                .stream()
                .map(JdbcRegistryDataDTO::getDataKey)
                .filter(fullPath -> fullPath.length() > key.length())
                .map(fullPath -> StringUtils.substringBefore(fullPath.substring(key.length() + 1), "/"))
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RegistryException(String.format("Get key: %s children error", key), e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return jdbcRegistryServer.getJdbcRegistryDataByKey(key).isPresent();
        } catch (Exception e) {
            throw new RegistryException(String.format("Check key: %s exist error", key), e);
        }
    }

    @Override
    public void close() {
        log.info("Closing Jdbc Registry...");
        // remove the current Ephemeral node, if can connect to jdbc
        try {
            jdbcRegistryServer.close();
        } catch (Exception e) {
            log.error("Close Jdbc Registry error", e);
        }
        log.info("Closed Jdbc Registry...");
    }
}
