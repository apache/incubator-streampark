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

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Dependency;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.mapper.DependencyMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.DependencyService;
import org.apache.streampark.console.core.service.FlinkSqlService;

import org.apache.commons.collections.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class DependencyServiceImpl extends ServiceImpl<DependencyMapper, Dependency>
    implements DependencyService {

  @Autowired private ApplicationService applicationService;
  @Autowired private CommonService commonService;
  @Autowired private FlinkSqlService flinkSqlService;

  @Override
  public IPage<Dependency> page(Dependency dependency, RestRequest restRequest) {
    if (dependency.getTeamId() == null) {
      return null;
    }
    Page<Dependency> page = new MybatisPager<Dependency>().getDefaultPage(restRequest);
    return this.baseMapper.page(page, dependency);
  }

  @Override
  public void addDependency(Dependency dependency) {
    String dependencyName = dependency.getDependencyName();
    ApiAlertException.throwIfNull(dependencyName, "No dependency uploaded.");

    Long teamId = dependency.getTeamId();
    ApiAlertException.throwIfTrue(
        this.findByDependencyName(teamId, dependencyName) != null,
        String.format("Sorry, the dependency %s already exists.", dependency.getDependencyName()));

    // copy jar to team upload directory
    transferTeamDependency(teamId, dependencyName);

    dependency.setCreatorId(commonService.getUserId());
    this.save(dependency);
  }

  @Override
  public Dependency findByDependencyName(Long teamId, String name) {
    LambdaQueryWrapper<Dependency> queryWrapper =
        new LambdaQueryWrapper<Dependency>()
            .eq(Dependency::getDependencyName, name)
            .eq(Dependency::getTeamId, teamId);
    return baseMapper.selectOne(queryWrapper);
  }

  @Override
  public void updateDependency(Dependency dependency) {
    Dependency findDependency = getById(dependency.getId());
    checkOrElseAlert(findDependency);

    String dependencyName = dependency.getDependencyName();
    if (dependencyName != null) {
      ApiAlertException.throwIfFalse(
          dependencyName.equals(findDependency.getDependencyName()),
          "Please make sure the dependency name is not changed.");
      transferTeamDependency(findDependency.getTeamId(), dependencyName);
    }

    findDependency.setDescription(dependency.getDescription());
    baseMapper.updateById(findDependency);
  }

  @Override
  public void deleteDependency(Dependency dependency) {
    Dependency findDependency = getById(dependency.getId());
    checkOrElseAlert(findDependency);

    FsOperator.lfs()
        .delete(
            String.format(
                "%s/%d/%s",
                Workspace.local().APP_UPLOADS(),
                findDependency.getTeamId(),
                findDependency.getDependencyName()));

    this.removeById(dependency);
  }

  public List<Dependency> findByTeamId(Long teamId) {
    LambdaQueryWrapper<Dependency> queryWrapper =
        new LambdaQueryWrapper<Dependency>().eq(Dependency::getTeamId, teamId);
    return baseMapper.selectList(queryWrapper);
  }

  private void transferTeamDependency(Long teamId, String dependencyName) {
    String teamUploads = String.format("%s/%d", Workspace.local().APP_UPLOADS(), teamId);
    if (!FsOperator.lfs().exists(teamUploads)) {
      FsOperator.lfs().mkdirs(teamUploads);
    }
    File localJar = new File(WebUtils.getAppTempDir(), dependencyName);
    File teamUploadJar = new File(teamUploads, dependencyName);
    ApiAlertException.throwIfFalse(
        localJar.exists(), "Missing file: " + dependencyName + ", please upload again");
    FsOperator.lfs()
        .upload(localJar.getAbsolutePath(), teamUploadJar.getAbsolutePath(), false, true);
  }

  private void checkOrElseAlert(Dependency dependency) {
    ApiAlertException.throwIfNull(dependency, "The dependency does not exist.");

    ApiAlertException.throwIfTrue(
        isDependByApplications(dependency),
        "Sorry, the dependency is still in use, cannot be removed.");
  }

  private boolean isDependByApplications(Dependency dependency) {
    return CollectionUtils.isNotEmpty(getDependencyApplicationsById(dependency));
  }

  private List<Application> getDependencyApplicationsById(Dependency dependency) {
    List<Application> dependApplications = new ArrayList<>();
    List<Application> applications = applicationService.getByTeamId(dependency.getTeamId());
    Map<Long, Application> applicationMap =
        applications.stream()
            .collect(Collectors.toMap(Application::getId, application -> application));

    // Get the application that depends on this dependency
    List<FlinkSql> flinkSqls = flinkSqlService.getByTeamId(dependency.getTeamId());
    for (FlinkSql flinkSql : flinkSqls) {
      String sqlTeamDependency = flinkSql.getTeamDependency();
      if (sqlTeamDependency != null
          && sqlTeamDependency.contains(String.valueOf(dependency.getTeamId()))) {
        Application app = applicationMap.get(flinkSql.getAppId());
        if (!dependApplications.contains(app)) {
          dependApplications.add(applicationMap.get(flinkSql.getAppId()));
        }
      }
    }

    return dependApplications;
  }
}
