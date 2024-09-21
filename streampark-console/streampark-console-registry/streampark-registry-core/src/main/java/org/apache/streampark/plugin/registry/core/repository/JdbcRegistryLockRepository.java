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

package org.apache.streampark.plugin.registry.core.repository;

import org.apache.streampark.plugin.registry.core.mapper.JdbcRegistryLockMapper;
import org.apache.streampark.plugin.registry.core.model.DO.JdbcRegistryLock;
import org.apache.streampark.plugin.registry.core.model.DTO.JdbcRegistryLockDTO;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Repository
public class JdbcRegistryLockRepository {

    @Autowired
    private JdbcRegistryLockMapper jdbcRegistryLockMapper;

    public void deleteByClientIds(List<Long> clientIds) {
        if (CollectionUtils.isEmpty(clientIds)) {
            return;
        }
        jdbcRegistryLockMapper.deleteByClientIds(clientIds);
    }

    public void insert(JdbcRegistryLockDTO jdbcRegistryLock) {
        checkNotNull(jdbcRegistryLock);
        JdbcRegistryLock jdbcRegistryLockDO = JdbcRegistryLockDTO.toJdbcRegistryLock(jdbcRegistryLock);
        jdbcRegistryLockMapper.insert(jdbcRegistryLockDO);
        jdbcRegistryLock.setId(jdbcRegistryLockDO.getId());
    }

    public void deleteById(Long id) {
        jdbcRegistryLockMapper.deleteById(id);
    }
}
