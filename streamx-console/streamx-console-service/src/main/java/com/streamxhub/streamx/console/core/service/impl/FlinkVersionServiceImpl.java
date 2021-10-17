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
package com.streamxhub.streamx.console.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.streamx.console.core.dao.FlinkVersionMapper;
import com.streamxhub.streamx.console.core.entity.FlinkVersion;
import com.streamxhub.streamx.console.core.service.FlinkVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkVersionServiceImpl extends ServiceImpl<FlinkVersionMapper, FlinkVersion> implements FlinkVersionService {

    /**
     * 需要校验两个地方:
     * 1) name 不能重复
     * 2) flink版本不能重新(flink版本需要解析得出)
     *
     * @param version
     * @return
     */
    @Override
    public boolean exists(FlinkVersion version) {
        //1) check name
        LambdaQueryWrapper<FlinkVersion> nameQuery = new LambdaQueryWrapper<FlinkVersion>()
                .eq(FlinkVersion::getFlinkName, version.getFlinkName());
        if (version.getId() != null) {
            nameQuery.ne(FlinkVersion::getId, version.getId());
        }
        return this.count(nameQuery) == 0;
    }

    @Override
    public boolean create(FlinkVersion version) {
        int count = this.baseMapper.selectCount(null);
        if (count == 0) {
            version.setIsDefault(true);
        }
        try {
            version.setCreateTime(new Date());
            version.doSetVersion();
            version.doSetFlinkConf();
            return save(version);
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public void update(FlinkVersion version) throws IOException {
        FlinkVersion flinkVersion = super.getById(version.getId());
        assert flinkVersion != null;
        flinkVersion.setDescription(version.getDescription());
        flinkVersion.setFlinkName(version.getFlinkName());
        if (!version.getFlinkHome().equals(flinkVersion.getFlinkHome())) {
            flinkVersion.setFlinkHome(version.getFlinkHome());
            flinkVersion.doSetFlinkConf();
        }
        updateById(flinkVersion);
    }

    @Override
    public void setDefault(Long id) {
        this.baseMapper.setDefault(id);
    }

    @Override
    public FlinkVersion getByAppId(Long appId) {
        return this.baseMapper.getByAppId(appId);
    }

    @Override
    public FlinkVersion getDefault() {
        return this.baseMapper.selectOne(
                new LambdaQueryWrapper<FlinkVersion>().eq(FlinkVersion::getIsDefault, true)
        );
    }

    @Override
    public void syncConf(Long id) throws IOException {
        FlinkVersion flinkVersion = getById(id);
        flinkVersion.doSetFlinkConf();
        updateById(flinkVersion);
    }
}
