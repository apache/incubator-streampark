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

package org.apache.streampark.registry.core.repository;

import org.apache.streampark.registry.core.mapper.JdbcRegistryDataChanceEventMapper;
import org.apache.streampark.registry.core.model.DO.JdbcRegistryDataChanceEvent;
import org.apache.streampark.registry.core.model.DTO.JdbcRegistryDataChanceEventDTO;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class JdbcRegistryDataChanceEventRepository {

    private final JdbcRegistryDataChanceEventMapper jdbcRegistryDataChanceEventMapper;

    public JdbcRegistryDataChanceEventRepository(JdbcRegistryDataChanceEventMapper jdbcRegistryDataChanceEventMapper) {
        this.jdbcRegistryDataChanceEventMapper = jdbcRegistryDataChanceEventMapper;
    }

    public long getMaxJdbcRegistryDataChanceEventId() {
        Long maxId = jdbcRegistryDataChanceEventMapper.getMaxId();
        if (maxId == null) {
            return -1;
        } else {
            return maxId;
        }
    }

    public List<JdbcRegistryDataChanceEventDTO> selectJdbcRegistryDataChangeEventWhereIdAfter(long id) {
        return jdbcRegistryDataChanceEventMapper.selectJdbcRegistryDataChangeEventWhereIdAfter(id)
            .stream()
            .map(JdbcRegistryDataChanceEventDTO::fromJdbcRegistryDataChanceEvent)
            .collect(Collectors.toList());
    }

    public void insert(JdbcRegistryDataChanceEventDTO registryDataChanceEvent) {
        JdbcRegistryDataChanceEvent jdbcRegistryDataChanceEvent =
            JdbcRegistryDataChanceEventDTO.toJdbcRegistryDataChanceEvent(registryDataChanceEvent);
        jdbcRegistryDataChanceEventMapper.insert(jdbcRegistryDataChanceEvent);
        registryDataChanceEvent.setId(jdbcRegistryDataChanceEvent.getId());
    }

    public void deleteJdbcRegistryDataChangeEventBeforeCreateTime(Date createTime) {
        jdbcRegistryDataChanceEventMapper.deleteJdbcRegistryDataChangeEventBeforeCreateTime(createTime);
    }
}
