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
import org.apache.streampark.console.core.enums.EffectiveType;
import org.apache.streampark.console.core.enums.ReleaseState;
import org.apache.streampark.console.core.mapper.ApplicationBackUpMapper;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.EffectiveService;
import org.apache.streampark.console.core.service.FlinkSqlService;

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

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationBackUpServiceImpl
    extends ServiceImpl<ApplicationBackUpMapper, ApplicationBackUp>
    implements ApplicationBackUpService {

  @Autowired private ApplicationService applicationService;

  @Autowired private ApplicationConfigService configService;

  @Autowired private EffectiveService effectiveService;

  @Autowired private FlinkSqlService flinkSqlService;

  @Override
  public IPage<ApplicationBackUp> page(ApplicationBackUp backUp, RestRequest request) {
    Page<ApplicationBackUp> page = MybatisPager.getPage(request);
    LambdaQueryWrapper<ApplicationBackUp> queryWrapper =
        new LambdaQueryWrapper<ApplicationBackUp>()
            .eq(ApplicationBackUp::getAppId, backUp.getAppId());
    return this.baseMapper.selectPage(page, queryWrapper);
  }

  @Override
  @Transactional(rollbackFor = {Exception.class})
  public void rollback(ApplicationBackUp backParam) {

    Application application = applicationService.getById(backParam.getAppId());

    FsOperator fsOperator = application.getFsOperator();
    // backup files not exist
    if (!fsOperator.exists(backParam.getPath())) {
      return;
    }

    // if backup files exists, will be rollback
    // When rollback, determine the currently effective project is necessary to be
    // backed up.
    // If necessary, perform the backup first
    if (backParam.isBackup()) {
      application.setBackUpDescription(backParam.getDescription());
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
      configService.setLatestOrEffective(true, backParam.getId(), backParam.getAppId());
    } else {
      effectiveService.saveOrUpdate(backParam.getAppId(), EffectiveType.CONFIG, backParam.getId());
      // if flink sql task, will be rollback sql and dependencies
      if (application.isFlinkSqlJob()) {
        effectiveService.saveOrUpdate(
            backParam.getAppId(), EffectiveType.FLINKSQL, backParam.getSqlId());
      }
    }

    // delete the current valid project files (Note: If the rollback failed, need to
    // restore)
    fsOperator.delete(application.getAppHome());

    // copy backup files to a valid dir
    fsOperator.copyDir(backParam.getPath(), application.getAppHome());

    // update restart status
    applicationService.update(
        new UpdateWrapper<Application>()
            .lambda()
            .eq(Application::getId, application.getId())
            .set(Application::getRelease, ReleaseState.NEED_RESTART.get()));
  }

  @Override
  public void revoke(Application application) {
    Page<ApplicationBackUp> page = new Page<>();
    page.setCurrent(0).setSize(1).setSearchCount(false);
    LambdaQueryWrapper<ApplicationBackUp> queryWrapper =
        new LambdaQueryWrapper<ApplicationBackUp>()
            .eq(ApplicationBackUp::getAppId, application.getId())
            .orderByDesc(ApplicationBackUp::getCreateTime);

    Page<ApplicationBackUp> backUpPages = baseMapper.selectPage(page, queryWrapper);
    if (!backUpPages.getRecords().isEmpty()) {
      ApplicationBackUp backup = backUpPages.getRecords().get(0);
      String path = backup.getPath();
      application.getFsOperator().move(path, application.getWorkspace().APP_WORKSPACE());
      removeById(backup.getId());
    }
  }

  @Override
  public void removeApp(Application application) {
    try {
      baseMapper.delete(
          new LambdaQueryWrapper<ApplicationBackUp>()
              .eq(ApplicationBackUp::getAppId, application.getId()));
      application
          .getFsOperator()
          .delete(
              application
                  .getWorkspace()
                  .APP_BACKUPS()
                  .concat("/")
                  .concat(application.getId().toString()));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public void rollbackFlinkSql(Application application, FlinkSql sql) {
    ApplicationBackUp backUp = getFlinkSqlBackup(application.getId(), sql.getId());
    ApiAlertException.throwIfNull(
        backUp, "Application backup can't be null. Rollback flink sql failed.");
    // rollback config and sql
    effectiveService.saveOrUpdate(backUp.getAppId(), EffectiveType.CONFIG, backUp.getId());
    effectiveService.saveOrUpdate(backUp.getAppId(), EffectiveType.FLINKSQL, backUp.getSqlId());
  }

  @Override
  public boolean isFlinkSqlBacked(Long appId, Long sqlId) {
    LambdaQueryWrapper<ApplicationBackUp> queryWrapper =
        new LambdaQueryWrapper<ApplicationBackUp>()
            .eq(ApplicationBackUp::getAppId, appId)
            .eq(ApplicationBackUp::getSqlId, sqlId);
    return baseMapper.selectCount(queryWrapper) > 0;
  }

  private ApplicationBackUp getFlinkSqlBackup(Long appId, Long sqlId) {
    LambdaQueryWrapper<ApplicationBackUp> queryWrapper =
        new LambdaQueryWrapper<ApplicationBackUp>()
            .eq(ApplicationBackUp::getAppId, appId)
            .eq(ApplicationBackUp::getSqlId, sqlId);
    return baseMapper.selectOne(queryWrapper);
  }

  @Override
  public Boolean delete(Long id) throws InternalException {
    ApplicationBackUp backUp = getById(id);
    try {
      Application application = applicationService.getById(backUp.getAppId());
      application.getFsOperator().delete(backUp.getPath());
      removeById(id);
      return true;
    } catch (Exception e) {
      log.error("Delete application {} failed!", id, e);
      throw new InternalException(e.getMessage());
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class})
  public void backup(Application application, FlinkSql flinkSql) {
    ApplicationConfig config = configService.getEffective(application.getId());
    if (config != null) {
      application.setConfigId(config.getId());
    }
    // flink sql tasks need to back up sql and dependencies
    int version = 1;
    if (flinkSql != null) {
      application.setSqlId(flinkSql.getId());
      version = flinkSql.getVersion();
    } else if (config != null) {
      version = config.getVersion();
    }

    ApplicationBackUp applicationBackUp = new ApplicationBackUp(application);
    applicationBackUp.setVersion(version);

    this.save(applicationBackUp);
  }
}
