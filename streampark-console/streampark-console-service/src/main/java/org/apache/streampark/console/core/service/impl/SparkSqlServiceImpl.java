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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.entity.SparkSql;
import org.apache.streampark.console.core.enums.CandidateTypeEnum;
import org.apache.streampark.console.core.enums.EffectiveTypeEnum;
import org.apache.streampark.console.core.mapper.SparkSqlMapper;
import org.apache.streampark.console.core.service.SparkApplicationBackUpService;
import org.apache.streampark.console.core.service.SparkEffectiveService;
import org.apache.streampark.console.core.service.SparkEnvService;
import org.apache.streampark.console.core.service.SparkSqlService;
import org.apache.streampark.spark.client.proxy.SparkShimsProxy;
import org.apache.streampark.spark.core.util.SparkSqlValidationResult;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkSqlServiceImpl extends ServiceImpl<SparkSqlMapper, SparkSql>
    implements
        SparkSqlService {

    @Autowired
    private SparkEffectiveService effectiveService;

    @Autowired
    private SparkApplicationBackUpService backUpService;

    @Autowired
    private SparkEnvService sparkEnvService;

    private static final String SPARKSQL_VALIDATOR_CLASS = "org.apache.streampark.spark.core.util.SparkSqlValidator";

    @Override
    public SparkSql getEffective(Long appId, boolean decode) {
        SparkSql sparkSql = baseMapper.getEffective(appId);
        if (sparkSql != null && decode) {
            sparkSql.setSql(DeflaterUtils.unzipString(sparkSql.getSql()));
        }
        return sparkSql;
    }

    @Override
    public SparkSql getLatestSparkSql(Long appId, boolean decode) {
        Page<SparkSql> page = new Page<>();
        page.setCurrent(0).setSize(1).setSearchCount(false);
        LambdaQueryWrapper<SparkSql> queryWrapper = new LambdaQueryWrapper<SparkSql>()
            .eq(SparkSql::getAppId, appId)
            .orderByDesc(SparkSql::getVersion);

        Page<SparkSql> sparkSqlPage = baseMapper.selectPage(page, queryWrapper);
        return Optional.ofNullable(sparkSqlPage.getRecords())
            .filter(records -> !records.isEmpty())
            .map(records -> records.get(0))
            .map(
                sparkSql -> {
                    if (decode) {
                        sparkSql.setSql(DeflaterUtils.unzipString(sparkSql.getSql()));
                    }
                    return sparkSql;
                })
            .orElse(null);
    }

    @Override
    public void create(SparkSql sparkSql) {
        Integer version = this.baseMapper.getLatestVersion(sparkSql.getAppId());
        sparkSql.setVersion(version == null ? 1 : version + 1);
        String sql = DeflaterUtils.zipString(sparkSql.getSql());
        sparkSql.setSql(sql);
        this.save(sparkSql);
        this.setCandidate(CandidateTypeEnum.NEW, sparkSql.getAppId(), sparkSql.getId());
    }

    @Override
    public void setCandidate(CandidateTypeEnum candidateTypeEnum, Long appId, Long sqlId) {
        this.update(
            new LambdaUpdateWrapper<SparkSql>()
                .eq(SparkSql::getAppId, appId)
                .set(SparkSql::getCandidate, CandidateTypeEnum.NONE.get()));

        this.update(
            new LambdaUpdateWrapper<SparkSql>()
                .eq(SparkSql::getId, sqlId)
                .set(SparkSql::getCandidate, candidateTypeEnum.get()));
    }

    @Override
    public List<SparkSql> listSparkSqlHistory(Long appId) {
        LambdaQueryWrapper<SparkSql> queryWrapper = new LambdaQueryWrapper<SparkSql>()
            .eq(SparkSql::getAppId, appId)
            .orderByDesc(SparkSql::getVersion);

        List<SparkSql> sqlList = this.baseMapper.selectList(queryWrapper);
        SparkSql effective = getEffective(appId, false);
        if (effective != null) {
            sqlList.stream()
                .filter(sql -> sql.getId().equals(effective.getId()))
                .findFirst()
                .ifPresent(sql -> sql.setEffective(true));
        }
        return sqlList;
    }

    @Override
    public SparkSql getCandidate(Long appId, CandidateTypeEnum candidateTypeEnum) {
        LambdaQueryWrapper<SparkSql> queryWrapper = new LambdaQueryWrapper<SparkSql>().eq(SparkSql::getAppId, appId);
        if (candidateTypeEnum == null) {
            queryWrapper.gt(SparkSql::getCandidate, CandidateTypeEnum.NONE.get());
        } else {
            queryWrapper.eq(SparkSql::getCandidate, candidateTypeEnum.get());
        }
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public void toEffective(Long appId, Long sqlId) {
        effectiveService.saveOrUpdate(appId, EffectiveTypeEnum.SPARKSQL, sqlId);
    }

    @Override
    public void cleanCandidate(Long id) {
        this.update(
            new LambdaUpdateWrapper<SparkSql>()
                .eq(SparkSql::getId, id)
                .set(SparkSql::getCandidate, CandidateTypeEnum.NONE.get()));
    }

    @Override
    public void removeByAppId(Long appId) {
        LambdaQueryWrapper<SparkSql> queryWrapper = new LambdaQueryWrapper<SparkSql>().eq(SparkSql::getAppId, appId);
        baseMapper.delete(queryWrapper);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void rollback(SparkApplication application) {
        SparkSql sql = getCandidate(application.getId(), CandidateTypeEnum.HISTORY);
        AssertUtils.notNull(sql);
        try {
            // check and backup current job
            SparkSql effectiveSql = getEffective(application.getId(), false);
            AssertUtils.notNull(effectiveSql);
            // rollback history sql
            backUpService.rollbackSparkSql(application, sql);
        } catch (Exception e) {
            log.error("Backup and Roll back SparkSql before start failed.");
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public SparkSqlValidationResult verifySql(String sql, Long versionId) {
        SparkEnv sparkEnv = sparkEnvService.getById(versionId);
        return SparkShimsProxy.proxyVerifySql(
            sparkEnv.getSparkVersion(),
            classLoader -> {
                try {
                    Class<?> clazz = classLoader.loadClass(SPARKSQL_VALIDATOR_CLASS);
                    Method method = clazz.getDeclaredMethod("verifySql", String.class);
                    method.setAccessible(true);
                    Object result = method.invoke(null, sql);
                    if (result == null) {
                        return null;
                    }
                    return SparkShimsProxy.getObject(this.getClass().getClassLoader(), result);
                } catch (Throwable e) {
                    log.error(
                        "verifySql invocationTargetException: {}",
                        ExceptionUtils.stringifyException(e));
                }
                return null;
            });
    }

    @Override
    public List<SparkSql> listByTeamId(Long teamId) {
        return this.baseMapper.selectSqlsByTeamId(teamId);
    }

    @Override
    public IPage<SparkSql> getPage(Long appId, RestRequest request) {
        request.setSortField("version");
        Page<SparkSql> page = MybatisPager.getPage(request);
        LambdaQueryWrapper<SparkSql> queryWrapper = new LambdaQueryWrapper<SparkSql>().eq(SparkSql::getAppId, appId);
        IPage<SparkSql> sqlList = this.baseMapper.selectPage(page, queryWrapper);
        SparkSql effectiveSql = baseMapper.getEffective(appId);
        if (effectiveSql != null) {
            for (SparkSql sql : sqlList.getRecords()) {
                if (sql.getId().equals(effectiveSql.getId())) {
                    sql.setEffective(true);
                    break;
                }
            }
        }
        return sqlList;
    }
}
