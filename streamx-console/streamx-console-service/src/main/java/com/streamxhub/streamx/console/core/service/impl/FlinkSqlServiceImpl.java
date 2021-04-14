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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.console.core.dao.FlinkSqlMapper;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.FlinkSql;
import com.streamxhub.streamx.console.core.enums.EffectiveType;
import com.streamxhub.streamx.console.core.service.EffectiveService;
import com.streamxhub.streamx.console.core.service.FlinkSqlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkSqlServiceImpl extends ServiceImpl<FlinkSqlMapper, FlinkSql> implements FlinkSqlService {

    @Autowired
    private EffectiveService effectiveService;

    /**
     * @param appParam
     * @param decode
     * @return
     */
    @Override
    public FlinkSql getEffective(Long appId, boolean decode) {
        FlinkSql flinkSql = baseMapper.getEffective(appId);
        if (decode) {
            flinkSql.setSql(DeflaterUtils.unzipString(flinkSql.getSql()));
        }
        return flinkSql;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void create(FlinkSql flinkSql) {
        Integer version = this.baseMapper.getLastVersion(flinkSql.getAppId());
        flinkSql.setVersion(version == null ? 1 : version + 1);
        String sql = DeflaterUtils.zipString(flinkSql.getSql());
        flinkSql.setSql(sql);
        this.save(flinkSql);
        this.setLatest(flinkSql.getId(), flinkSql.getAppId());
    }

    @Override
    public void setLatestOrEffective(Boolean latest, Long sqlId, Long appId) {
        if (latest) {
            this.setLatest(sqlId, appId);
        } else {
            this.toEffective(appId, sqlId);
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    public void setLatest(Long appId, Long sqlId) {
        LambdaUpdateWrapper<FlinkSql> updateWrapper = new UpdateWrapper<FlinkSql>().lambda();
        updateWrapper.set(FlinkSql::getLatest, 0)
                .eq(FlinkSql::getAppId, appId);
        this.update(updateWrapper);

        updateWrapper = new UpdateWrapper<FlinkSql>().lambda();
        updateWrapper.set(FlinkSql::getLatest, 1)
                .eq(FlinkSql::getId, sqlId);
        this.update(updateWrapper);
    }

    @Override
    public List<FlinkSql> history(Application application) {
        LambdaQueryWrapper<FlinkSql> wrapper = new QueryWrapper<FlinkSql>().lambda();
        wrapper.eq(FlinkSql::getAppId, application.getId())
                .orderByDesc(FlinkSql::getVersion);

        List<FlinkSql> sqlList = this.baseMapper.selectList(wrapper);
        FlinkSql effective = getEffective(application.getId(), false);

        for (FlinkSql sql : sqlList) {
            if (sql.getId().equals(effective.getId())) {
                sql.setEffective(true);
                break;
            }
        }

        return sqlList;
    }

    @Override
    public FlinkSql getLatest(Long appId) {
        return baseMapper.getLatest(appId);
    }

    @Override
    public void toEffective(Long appId, Long sqlId) {
        this.baseMapper.clearLatest(appId);
        effectiveService.saveOrUpdate(appId, EffectiveType.FLINKSQL, sqlId);
    }
}
