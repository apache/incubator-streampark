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
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.dao.FlinkSqlMapper;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.FlinkSql;
import com.streamxhub.streamx.console.core.enums.CandidateType;
import com.streamxhub.streamx.console.core.enums.EffectiveType;
import com.streamxhub.streamx.console.core.service.ApplicationBackUpService;
import com.streamxhub.streamx.console.core.service.EffectiveService;
import com.streamxhub.streamx.console.core.service.FlinkSqlService;
import com.streamxhub.streamx.flink.core.SqlError;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private final Map<String, URLClassLoader> shimsClassLoaderCache = new ConcurrentHashMap<>();

    /**
     * @param appId
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
    public void create(FlinkSql flinkSql, CandidateType type) {
        Integer version = this.baseMapper.getLastVersion(flinkSql.getAppId());
        flinkSql.setVersion(version == null ? 1 : version + 1);
        String sql = DeflaterUtils.zipString(flinkSql.getSql());
        flinkSql.setSql(sql);
        this.save(flinkSql);
        this.setCandidateOrEffective(type, flinkSql.getAppId(), flinkSql.getId());
    }

    @Override
    public void setCandidateOrEffective(CandidateType candidateType, Long appId, Long sqlId) {
        if (CandidateType.NONE.equals(candidateType)) {
            this.toEffective(appId, sqlId);
        } else {
            this.setCandidate(appId, sqlId, candidateType);
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    public void setCandidate(Long appId, Long sqlId, CandidateType candidateType) {
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

        for (FlinkSql sql : sqlList) {
            if (sql.getId().equals(effective.getId())) {
                sql.setEffective(true);
                break;
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
    public void rollback(Application application) {
        FlinkSql sql = getCandidate(application.getId(), CandidateType.HISTORY);
        assert sql != null;

        //检查并备份当前的任务.
        FlinkSql effectiveSql = getEffective(application.getId(), false);
        assert effectiveSql != null;
        if (!isFlinkSqlBacked(effectiveSql)) {
            log.info("current job version:{}, Backing up...", sql.getVersion());
            backUpService.backup(application);
        } else {
            log.info("current job version:{}, already backed", sql.getVersion());
        }
        //回滚历史版本的任务
        backUpService.rollbackFlinkSql(application, sql);
    }

    @SneakyThrows
    @Override
    public SqlError verifySql(String sql) {
        ClassLoader loader = getFlinkShimsClassLoader();
        Class<?> clazz = loader.loadClass("com.streamxhub.streamx.flink.core.FlinkSqlValidator");
        Method method = clazz.getDeclaredMethod("verifySql", String.class);
        method.setAccessible(true);
        return (SqlError) method.invoke(null, sql);
    }

    @SneakyThrows
    private synchronized ClassLoader getFlinkShimsClassLoader() {
        // TODO: 根据用户选择的Flink版本选择对应的版本实现.
        String version = "1.13";

        if (!shimsClassLoaderCache.containsKey(version)) {
            String shimsRegex = "(^|.*)streamx-flink-shims_flink-(1.12|1.13)-(.*).jar$";
            Pattern pattern = Pattern.compile(shimsRegex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

            File[] libJars = new File(WebUtils.getAppDir("lib")).listFiles(pathname -> !pathname.getName().matches(shimsRegex));
            assert libJars != null;
            List<URL> libList = new ArrayList<>(0);
            for (File jar : libJars) {
                libList.add(jar.toURI().toURL());
            }

            List<URL> shimsList = new ArrayList<>(0);
            File[] shimsJars = new File(WebUtils.getAppDir("lib")).listFiles(pathname -> pathname.getName().matches(shimsRegex));
            assert shimsJars != null;
            for (File jar : shimsJars) {
                shimsList.add(jar.toURI().toURL());
            }

            List<URL> shimsUrls = shimsList.stream().filter(url -> {
                Matcher matcher = pattern.matcher(url.getPath());
                return matcher.matches() && version.equals(matcher.group(2));
            }).collect(Collectors.toList());

            shimsUrls.addAll(libList);

            URLClassLoader classLoader = new URLClassLoader(shimsUrls.toArray(new URL[0]));
            shimsClassLoaderCache.put(version, classLoader);
        }

        return shimsClassLoaderCache.get(version);

    }

    private boolean isFlinkSqlBacked(FlinkSql sql) {
        return backUpService.isFlinkSqlBacked(sql.getAppId(), sql.getId());
    }
}
