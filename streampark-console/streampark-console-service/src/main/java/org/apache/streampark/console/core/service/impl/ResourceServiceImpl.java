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
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.mapper.ResourceMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ResourceService;

import org.apache.commons.collections.CollectionUtils;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource>
    implements ResourceService {

  @Autowired private ApplicationService applicationService;
  @Autowired private CommonService commonService;
  @Autowired private FlinkSqlService flinkSqlService;

  @Override
  public IPage<Resource> page(Resource resource, RestRequest restRequest) {
    if (resource.getTeamId() == null) {
      return null;
    }
    Page<Resource> page = new MybatisPager<Resource>().getDefaultPage(restRequest);
    return this.baseMapper.page(page, resource);
  }

  /**
   * check resource exists by user id
   *
   * @param userId user id
   * @return true if exists
   */
  @Override
  public boolean existsByUserId(Long userId) {
    return this.baseMapper.existsByUserId(userId);
  }

  @Override
  public void addResource(Resource resource) {
    String resourceName = resource.getResourceName();
    ApiAlertException.throwIfNull(resourceName, "No resource uploaded.");

    Long teamId = resource.getTeamId();
    ApiAlertException.throwIfTrue(
        this.findByResourceName(teamId, resourceName) != null,
        String.format("Sorry, the resource %s already exists.", resource.getResourceName()));

    // copy jar to team upload directory
    transferTeamResource(teamId, resourceName);

    resource.setCreatorId(commonService.getUserId());
    this.save(resource);
  }

  @Override
  public Resource findByResourceName(Long teamId, String name) {
    LambdaQueryWrapper<Resource> queryWrapper =
        new LambdaQueryWrapper<Resource>()
            .eq(Resource::getResourceName, name)
            .eq(Resource::getTeamId, teamId);
    return baseMapper.selectOne(queryWrapper);
  }

  @Override
  public void updateResource(Resource resource) {
    Resource findResource = getById(resource.getId());
    checkOrElseAlert(findResource);

    String resourceName = resource.getResourceName();
    if (resourceName != null) {
      ApiAlertException.throwIfFalse(
          resourceName.equals(findResource.getResourceName()),
          "Please make sure the resource name is not changed.");
      transferTeamResource(findResource.getTeamId(), resourceName);
    }

    findResource.setDescription(resource.getDescription());
    baseMapper.updateById(findResource);
  }

  @Override
  public void deleteResource(Resource resource) {
    Resource findResource = getById(resource.getId());
    checkOrElseAlert(findResource);

    FsOperator.lfs()
        .delete(
            String.format(
                "%s/%d/%s",
                Workspace.local().APP_UPLOADS(),
                findResource.getTeamId(),
                findResource.getResourceName()));

    this.removeById(resource);
  }

  public List<Resource> findByTeamId(Long teamId) {
    LambdaQueryWrapper<Resource> queryWrapper =
        new LambdaQueryWrapper<Resource>().eq(Resource::getTeamId, teamId);
    return baseMapper.selectList(queryWrapper);
  }

  /**
   * change resource owner
   *
   * @param userId original user id
   * @param targetUserId target user id
   */
  @Override
  public void changeUser(Long userId, Long targetUserId) {
    LambdaUpdateWrapper<Resource> updateWrapper =
        new LambdaUpdateWrapper<Resource>()
            .eq(Resource::getCreatorId, userId)
            .set(Resource::getCreatorId, targetUserId);
    this.baseMapper.update(null, updateWrapper);
  }

  private void transferTeamResource(Long teamId, String resourceName) {
    String teamUploads = String.format("%s/%d", Workspace.local().APP_UPLOADS(), teamId);
    if (!FsOperator.lfs().exists(teamUploads)) {
      FsOperator.lfs().mkdirs(teamUploads);
    }
    File localJar = new File(WebUtils.getAppTempDir(), resourceName);
    File teamUploadJar = new File(teamUploads, resourceName);
    ApiAlertException.throwIfFalse(
        localJar.exists(), "Missing file: " + resourceName + ", please upload again");
    FsOperator.lfs()
        .upload(localJar.getAbsolutePath(), teamUploadJar.getAbsolutePath(), false, true);
  }

  private void checkOrElseAlert(Resource resource) {
    ApiAlertException.throwIfNull(resource, "The resource does not exist.");

    ApiAlertException.throwIfTrue(
        isDependByApplications(resource),
        "Sorry, the resource is still in use, cannot be removed.");
  }

  private boolean isDependByApplications(Resource resource) {
    return CollectionUtils.isNotEmpty(getResourceApplicationsById(resource));
  }

  private List<Application> getResourceApplicationsById(Resource resource) {
    List<Application> dependApplications = new ArrayList<>();
    List<Application> applications = applicationService.getByTeamId(resource.getTeamId());
    Map<Long, Application> applicationMap =
        applications.stream()
            .collect(Collectors.toMap(Application::getId, application -> application));

    // Get the application that depends on this resource
    List<FlinkSql> flinkSqls = flinkSqlService.getByTeamId(resource.getTeamId());
    for (FlinkSql flinkSql : flinkSqls) {
      String sqlTeamResource = flinkSql.getTeamResource();
      if (sqlTeamResource != null
          && sqlTeamResource.contains(String.valueOf(resource.getTeamId()))) {
        Application app = applicationMap.get(flinkSql.getAppId());
        if (!dependApplications.contains(app)) {
          dependApplications.add(applicationMap.get(flinkSql.getAppId()));
        }
      }
    }

    return dependApplications;
  }
}
