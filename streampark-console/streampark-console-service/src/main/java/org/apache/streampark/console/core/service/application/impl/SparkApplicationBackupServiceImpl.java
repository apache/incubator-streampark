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

package org.apache.streampark.console.core.service.application.impl;

import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkApplicationBackup;
import org.apache.streampark.console.core.entity.SparkApplicationConfig;
import org.apache.streampark.console.core.entity.SparkSql;
import org.apache.streampark.console.core.enums.EffectiveTypeEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.mapper.SparkApplicationBackUpMapper;
import org.apache.streampark.console.core.service.SparkEffectiveService;
import org.apache.streampark.console.core.service.SparkSqlService;
import org.apache.streampark.console.core.service.application.SparkApplicationBackupService;
import org.apache.streampark.console.core.service.application.SparkApplicationConfigService;
import org.apache.streampark.console.core.service.application.SparkApplicationManageService;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkApplicationBackupServiceImpl
    extends
        ServiceImpl<SparkApplicationBackUpMapper, SparkApplicationBackup>
    implements
        SparkApplicationBackupService {

    @Autowired
    private SparkApplicationManageService applicationManageService;

    @Autowired
    private SparkApplicationConfigService configService;

    @Autowired
    private SparkEffectiveService effectiveService;

    @Autowired
    private SparkSqlService sparkSqlService;

    @Override
    public IPage<SparkApplicationBackup> getPage(SparkApplicationBackup bakParam, RestRequest request) {
        return this.lambdaQuery().eq(SparkApplicationBackup::getAppId, bakParam.getAppId())
            .page(MybatisPager.getPage(request));
    }

    @Override
    public void rollback(SparkApplicationBackup bakParam) {

        SparkApplication application = applicationManageService.getById(bakParam.getAppId());

        FsOperator fsOperator = application.getFsOperator();
        // backup files not exist
        if (!fsOperator.exists(bakParam.getPath())) {
            return;
        }

        // if backup files exists, will be rollback
        // When rollback, determine the currently effective project is necessary to be
        // backed up.
        // If necessary, perform the backup first
        if (bakParam.isBackup()) {
            application.setBackUpDescription(bakParam.getDescription());
            if (application.isSparkSqlJob()) {
                SparkSql sparkSql = sparkSqlService.getEffective(application.getId(), false);
                backup(application, sparkSql);
            } else {
                backup(application, null);
            }
        }

        // restore config and sql

        // if running, set Latest
        if (application.isRunning()) {
            // rollback to back up config
            configService.setLatestOrEffective(true, bakParam.getId(), bakParam.getAppId());
        } else {
            effectiveService.saveOrUpdate(
                bakParam.getAppId(), EffectiveTypeEnum.SPARKCONFIG, bakParam.getId());
            // if spark sql task, will be rollback sql and dependencies
            if (application.isSparkSqlJob()) {
                effectiveService.saveOrUpdate(
                    bakParam.getAppId(), EffectiveTypeEnum.SPARKSQL, bakParam.getSqlId());
            }
        }

        // delete the current valid project files (Note: If the rollback failed, need to
        // restore)
        fsOperator.delete(application.getAppHome());

        // copy backup files to a valid dir
        fsOperator.copyDir(bakParam.getPath(), application.getAppHome());

        // update restart status
        applicationManageService.update(
            new UpdateWrapper<SparkApplication>()
                .lambda()
                .eq(SparkApplication::getId, application.getId())
                .set(SparkApplication::getRelease, ReleaseStateEnum.NEED_RESTART.get()));
    }

    @Override
    public void revoke(SparkApplication appParam) {
        Page<SparkApplicationBackup> page = new Page<>();
        page.setCurrent(0).setSize(1).setSearchCount(false);
        Page<SparkApplicationBackup> backUpPages =
            this.lambdaQuery().eq(SparkApplicationBackup::getAppId, appParam.getId())
                .orderByDesc(SparkApplicationBackup::getCreateTime).page(page);
        if (!backUpPages.getRecords().isEmpty()) {
            SparkApplicationBackup backup = backUpPages.getRecords().get(0);
            String path = backup.getPath();
            appParam.getFsOperator().move(path, appParam.getWorkspace().APP_WORKSPACE());
            super.removeById(backup.getId());
        }
    }

    @Override
    public void remove(SparkApplication appParam) {
        try {
            this.lambdaUpdate().eq(SparkApplicationBackup::getAppId, appParam.getId()).remove();
            appParam
                .getFsOperator()
                .delete(
                    appParam
                        .getWorkspace()
                        .APP_BACKUPS()
                        .concat("/")
                        .concat(appParam.getId().toString()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void rollbackSparkSql(SparkApplication appParam, SparkSql sparkSqlParam) {
        SparkApplicationBackup backUp = this.lambdaQuery()
            .eq(SparkApplicationBackup::getAppId, appParam.getId())
            .eq(SparkApplicationBackup::getSqlId, sparkSqlParam.getId())
            .one();
        ApiAlertException.throwIfNull(
            backUp, "Application backup can't be null. Rollback spark sql failed.");
        // rollback config and sql
        effectiveService.saveOrUpdate(backUp.getAppId(), EffectiveTypeEnum.SPARKCONFIG, backUp.getId());
        effectiveService.saveOrUpdate(backUp.getAppId(), EffectiveTypeEnum.SPARKSQL, backUp.getSqlId());
    }

    @Override
    public Boolean removeById(Long id) throws InternalException {
        SparkApplicationBackup backUp = getById(id);
        try {
            SparkApplication application = applicationManageService.getById(backUp.getAppId());
            application.getFsOperator().delete(backUp.getPath());
            super.removeById(id);
            return true;
        } catch (Exception e) {
            throw new InternalException(e.getMessage());
        }
    }

    @Override
    public void backup(SparkApplication appParam, SparkSql sparkSqlParam) {
        // basic configuration file backup
        String appHome = (appParam.isFromBuildJob())
            ? appParam.getDistHome()
            : appParam.getAppHome();
        FsOperator fsOperator = appParam.getFsOperator();
        if (fsOperator.exists(appHome)) {
            // move files to back up directory
            SparkApplicationConfig config = configService.getEffective(appParam.getId());
            if (config != null) {
                appParam.setConfigId(config.getId());
            }
            // spark sql tasks need to back up sql and dependencies
            int version = 1;
            if (sparkSqlParam != null) {
                appParam.setSqlId(sparkSqlParam.getId());
                version = sparkSqlParam.getVersion();
            } else if (config != null) {
                version = config.getVersion();
            }

            SparkApplicationBackup applicationBackUp = new SparkApplicationBackup(appParam);
            applicationBackUp.setVersion(version);

            this.save(applicationBackUp);
            fsOperator.mkdirs(applicationBackUp.getPath());
            fsOperator.copyDir(appHome, applicationBackUp.getPath());
        }
    }
}
