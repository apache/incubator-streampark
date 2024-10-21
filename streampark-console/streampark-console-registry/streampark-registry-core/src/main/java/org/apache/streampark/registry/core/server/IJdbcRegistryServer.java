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

import org.apache.streampark.registry.core.model.DO.JdbcRegistryData;
import org.apache.streampark.registry.core.model.DTO.JdbcRegistryDataDTO;

import java.util.List;
import java.util.Optional;

/**
 * The JdbcRegistryServer is represent the server side of the jdbc registry, it can be thought as db server.
 */
public interface IJdbcRegistryServer extends AutoCloseable {

    void start();

    /**
     * Subscribe the {@link JdbcRegistryData} change.
     */
    void subscribeJdbcRegistryDataChange(JdbcRegistryDataChangeListener jdbcRegistryDataChangeListener);

    /**
     * Get the {@link JdbcRegistryDataDTO} by key.
     */
    Optional<JdbcRegistryDataDTO> getJdbcRegistryDataByKey(String key);

    /**
     * List all the {@link JdbcRegistryDataDTO} children by key.
     * <p>
     * e.g. key = "/streampark/master", and data exist in db is "/streampark/master/master1", "/streampark/master/master2"
     * <p>
     * then the return value will be ["master1", "master2"]
     */
    List<JdbcRegistryDataDTO> listJdbcRegistryDataChildren(String key);

    /**
     * Put the {@link JdbcRegistryDataDTO} to the jdbc registry server.
     * <p>
     * If the key is already exist, then update the {@link JdbcRegistryDataDTO}. If the key is not exist, then insert a new {@link JdbcRegistryDataDTO}.
     */
    void putJdbcRegistryData(String key, String value);

    /**
     * Delete the {@link JdbcRegistryDataDTO} by key.
     */
    void deleteJdbcRegistryDataByKey(String key);

    /**
     * Close the server, once the server been closed, it cannot work anymore.
     */
    @Override
    void close();
}
