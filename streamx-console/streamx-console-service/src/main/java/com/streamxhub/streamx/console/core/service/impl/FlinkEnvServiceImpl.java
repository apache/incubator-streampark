/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.console.core.dao.FlinkEnvMapper;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.service.FlinkEnvService;
import com.streamxhub.streamx.console.core.task.FlinkTrackingTask;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class FlinkEnvServiceImpl extends ServiceImpl<FlinkEnvMapper, FlinkEnv> implements FlinkEnvService {

    /**
     * 需要校验两个地方:
     * 1) name 不能重复
     * 2) flink版本不能重新(flink版本需要解析得出)
     *
     * @param version
     * @return
     */
    @Override
    public boolean exists(FlinkEnv version) {
        //1) check name
        LambdaQueryWrapper<FlinkEnv> nameQuery = new LambdaQueryWrapper<FlinkEnv>()
            .eq(FlinkEnv::getFlinkName, version.getFlinkName());
        if (version.getId() != null) {
            nameQuery.ne(FlinkEnv::getId, version.getId());
        }
        return this.count(nameQuery) == 0;
    }

    @Override
    public boolean create(FlinkEnv version) throws Exception {
        int count = this.baseMapper.selectCount(null);
        if (count == 0) {
            version.setIsDefault(true);
        }
        version.setCreateTime(new Date());
        version.doSetFlinkConf();
        version.doSetVersion();
        return save(version);
    }

    @Override
    public void update(FlinkEnv version) throws IOException {
        FlinkEnv flinkEnv = super.getById(version.getId());
        if (flinkEnv == null) {
            throw new RuntimeException("flink home message lost, please check database status!");
        }
        flinkEnv.setDescription(version.getDescription());
        flinkEnv.setFlinkName(version.getFlinkName());
        if (!version.getFlinkHome().equals(flinkEnv.getFlinkHome())) {
            flinkEnv.setFlinkHome(version.getFlinkHome());
            flinkEnv.doSetFlinkConf();
            version.doSetVersion();
        }
        updateById(flinkEnv);
        FlinkTrackingTask.getFlinkEnvMap().put(flinkEnv.getId(), version);
    }

    @Override
    public void setDefault(Long id) {
        this.baseMapper.setDefault(id);
    }

    @Override
    public FlinkEnv getByAppId(Long appId) {
        return this.baseMapper.getByAppId(appId);
    }

    @Override
    public FlinkEnv getDefault() {
        return this.baseMapper.selectOne(
            new LambdaQueryWrapper<FlinkEnv>().eq(FlinkEnv::getIsDefault, true)
        );
    }

    @Override
    public FlinkEnv getByIdOrDefault(Long id) {
        FlinkEnv flinkEnv = getById(id);
        if (flinkEnv == null) {
            return getDefault();
        }
        return flinkEnv;
    }

    @Override
    public void syncConf(Long id) throws IOException {
        FlinkEnv flinkEnv = getById(id);
        flinkEnv.doSetFlinkConf();
        updateById(flinkEnv);
    }
}
