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

import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.common.util.ExceptionUtils;
import com.streamxhub.streamx.console.core.dao.FlinkSqlMapper;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.entity.FlinkSql;
import com.streamxhub.streamx.console.core.enums.CandidateType;
import com.streamxhub.streamx.console.core.enums.EffectiveType;
import com.streamxhub.streamx.console.core.service.ApplicationBackUpService;
import com.streamxhub.streamx.console.core.service.EffectiveService;
import com.streamxhub.streamx.console.core.service.FlinkEnvService;
import com.streamxhub.streamx.console.core.service.FlinkSqlService;
import com.streamxhub.streamx.flink.core.SqlError;
import com.streamxhub.streamx.flink.proxy.FlinkShimsProxy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkSqlServiceImpl extends ServiceImpl<FlinkSqlMapper, FlinkSql> implements FlinkSqlService {

    @Autowired
    private EffectiveService effectiveService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private FlinkEnvService flinkEnvService;

    /**
     * @param appId
     * @param decode
     * @return
     */
    @Override
    public FlinkSql getEffective(Long appId, boolean decode) {
        FlinkSql flinkSql = baseMapper.getEffective(appId);
        if (flinkSql != null && decode) {
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
        this.setCandidate(CandidateType.NEW, flinkSql.getAppId(), flinkSql.getId());
    }

    @Override
    public void setCandidate(CandidateType candidateType, Long appId, Long sqlId) {
        LambdaUpdateWrapper<FlinkSql> updateWrapper = new UpdateWrapper<FlinkSql>().lambda();
        updateWrapper.set(FlinkSql::getCandidate, 0)
                .eq(FlinkSql::getAppId, appId);
        this.update(updateWrapper);

        updateWrapper = new UpdateWrapper<FlinkSql>().lambda();
        updateWrapper.set(FlinkSql::getCandidate, candidateType.get())
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
        if (effective != null && !sqlList.isEmpty()) {
            for (FlinkSql sql : sqlList) {
                if (sql.getId().equals(effective.getId())) {
                    sql.setEffective(true);
                    break;
                }
            }
        }
        return sqlList;
    }

    @Override
    public FlinkSql getCandidate(Long appId, CandidateType candidateType) {
        if (candidateType == null) {
            return baseMapper.getCandidate(appId);
        } else {
            return baseMapper.getCandidateByType(appId, candidateType.get());
        }
    }

    @Override
    public void toEffective(Long appId, Long sqlId) {
        effectiveService.saveOrUpdate(appId, EffectiveType.FLINKSQL, sqlId);
    }

    @Override
    public void cleanCandidate(Long id) {
        this.baseMapper.cleanCandidate(id);
    }

    @Override
    public void removeApp(Long appId) {
        baseMapper.removeApp(appId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void rollback(Application application) {
        FlinkSql sql = getCandidate(application.getId(), CandidateType.HISTORY);
        assert sql != null;
        try {
            //检查并备份当前的任务.
            FlinkSql effectiveSql = getEffective(application.getId(), false);
            assert effectiveSql != null;
            //回滚历史版本的任务
            backUpService.rollbackFlinkSql(application, sql);
        } catch (Exception e) {
            log.error("Backup and Roll back FlinkSql before start failed.");
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public SqlError verifySql(String sql, Long versionId) {
        FlinkEnv flinkEnv = flinkEnvService.getById(versionId);
        return FlinkShimsProxy.proxy(flinkEnv.getFlinkVersion(), (Function<ClassLoader, SqlError>) classLoader -> {
            try {
                Class<?> clazz = classLoader.loadClass("com.streamxhub.streamx.flink.core.FlinkSqlValidator");
                Method method = clazz.getDeclaredMethod("verifySql", String.class);
                method.setAccessible(true);
                Object sqlError = method.invoke(null, sql);
                if (sqlError == null) {
                    return null;
                }
                return FlinkShimsProxy.getObject(this.getClass().getClassLoader(), sqlError);
            } catch (Throwable e) {
                log.error("verifySql invocationTargetException: {}", ExceptionUtils.stringifyException(e));
            }
            return null;
        });
    }

    private boolean isFlinkSqlBacked(FlinkSql sql) {
        return backUpService.isFlinkSqlBacked(sql.getAppId(), sql.getId());
    }
}
