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

import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationBackUp;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.enums.EffectiveTypeEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.mapper.ApplicationBackUpMapper;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.EffectiveService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_SQL_BACKUP_IS_NULL_ROLLBACK_FAILED;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationBackUpServiceImpl
    extends
        ServiceImpl<ApplicationBackUpMapper, ApplicationBackUp>
    implements
        ApplicationBackUpService {

    @Autowired
    private ApplicationManageService applicationManageService;

    @Autowired
    private ApplicationConfigService configService;

    @Autowired
    private EffectiveService effectiveService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Override
    public IPage<ApplicationBackUp> getPage(ApplicationBackUp bakParam, RestRequest request) {
        Page<ApplicationBackUp> page = MybatisPager.getPage(request);
        LambdaQueryWrapper<ApplicationBackUp> queryWrapper = new LambdaQueryWrapper<ApplicationBackUp>()
            .eq(ApplicationBackUp::getAppId, bakParam.getAppId());
        return this.baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public void rollback(ApplicationBackUp bakParam) {

        Application application = applicationManageService.getById(bakParam.getAppId());

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
            if (application.isFlinkSqlJob()) {
                FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
                backup(application, flinkSql);
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
                bakParam.getAppId(), EffectiveTypeEnum.CONFIG, bakParam.getId());
            // if flink sql task, will be rollback sql and dependencies
            if (application.isFlinkSqlJob()) {
                effectiveService.saveOrUpdate(
                    bakParam.getAppId(), EffectiveTypeEnum.FLINKSQL, bakParam.getSqlId());
            }
        }

        // delete the current valid project files (Note: If the rollback failed, need to
        // restore)
        fsOperator.delete(application.getAppHome());

        // copy backup files to a valid dir
        fsOperator.copyDir(bakParam.getPath(), application.getAppHome());

        // update restart status
        applicationManageService.update(
            new UpdateWrapper<Application>()
                .lambda()
                .eq(Application::getId, application.getId())
                .set(Application::getRelease, ReleaseStateEnum.NEED_RESTART.get()));
    }

    @Override
    public void revoke(Application appParam) {
        Page<ApplicationBackUp> page = new Page<>();
        page.setCurrent(0).setSize(1).setSearchCount(false);
        LambdaQueryWrapper<ApplicationBackUp> queryWrapper = new LambdaQueryWrapper<ApplicationBackUp>()
            .eq(ApplicationBackUp::getAppId, appParam.getId())
            .orderByDesc(ApplicationBackUp::getCreateTime);

        Page<ApplicationBackUp> backUpPages = baseMapper.selectPage(page, queryWrapper);
        if (!backUpPages.getRecords().isEmpty()) {
            ApplicationBackUp backup = backUpPages.getRecords().get(0);
            String path = backup.getPath();
            appParam.getFsOperator().move(path, appParam.getWorkspace().APP_WORKSPACE());
            super.removeById(backup.getId());
        }
    }

    @Override
    public void remove(Application appParam) {
        try {
            baseMapper.delete(
                new LambdaQueryWrapper<ApplicationBackUp>()
                    .eq(ApplicationBackUp::getAppId, appParam.getId()));
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
    public void rollbackFlinkSql(Application appParam, FlinkSql flinkSqlParam) {
        LambdaQueryWrapper<ApplicationBackUp> queryWrapper = new LambdaQueryWrapper<ApplicationBackUp>()
            .eq(ApplicationBackUp::getAppId, appParam.getId())
            .eq(ApplicationBackUp::getSqlId, flinkSqlParam.getId());
        ApplicationBackUp backUp = baseMapper.selectOne(queryWrapper);
        ApiAlertException.throwIfNull(
            backUp, FLINK_SQL_BACKUP_IS_NULL_ROLLBACK_FAILED);
        // rollback config and sql
        effectiveService.saveOrUpdate(backUp.getAppId(), EffectiveTypeEnum.CONFIG, backUp.getId());
        effectiveService.saveOrUpdate(backUp.getAppId(), EffectiveTypeEnum.FLINKSQL, backUp.getSqlId());
    }

    @Override
    public Boolean removeById(Long id) throws InternalException {
        ApplicationBackUp backUp = getById(id);
        try {
            Application application = applicationManageService.getById(backUp.getAppId());
            application.getFsOperator().delete(backUp.getPath());
            super.removeById(id);
            return true;
        } catch (Exception e) {
            throw new InternalException(e.getMessage());
        }
    }

    @Override
    public void backup(Application appParam, FlinkSql flinkSqlParam) {
        // basic configuration file backup
        String appHome = (appParam.isCustomCodeJob() && appParam.isCICDJob())
            ? appParam.getDistHome()
            : appParam.getAppHome();
        FsOperator fsOperator = appParam.getFsOperator();
        if (fsOperator.exists(appHome)) {
            // move files to back up directory
            ApplicationConfig config = configService.getEffective(appParam.getId());
            if (config != null) {
                appParam.setConfigId(config.getId());
            }
            // flink sql tasks need to back up sql and dependencies
            int version = 1;
            if (flinkSqlParam != null) {
                appParam.setSqlId(flinkSqlParam.getId());
                version = flinkSqlParam.getVersion();
            } else if (config != null) {
                version = config.getVersion();
            }

            ApplicationBackUp applicationBackUp = new ApplicationBackUp(appParam);
            applicationBackUp.setVersion(version);

            this.save(applicationBackUp);
            fsOperator.mkdirs(applicationBackUp.getPath());
            fsOperator.copyDir(appHome, applicationBackUp.getPath());
        }
    }
}
