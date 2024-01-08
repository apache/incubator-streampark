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
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.bean.Dependency;
import org.apache.streampark.console.core.bean.MavenPom;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.ResourceTypeEnum;
import org.apache.streampark.console.core.mapper.ResourceMapper;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.console.core.service.resource.ResourceTypeHandleFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource>
    implements ResourceService {

  @Autowired private ApplicationManageService applicationManageService;

  @Autowired private CommonService commonService;

  @Autowired private FlinkSqlService flinkSqlService;

  @Override
  public IPage<Resource> getPage(Resource resource, RestRequest request) {
    if (resource.getTeamId() == null) {
      return null;
    }
    Page<Resource> page = MybatisPager.getPage(request);
    return this.baseMapper.selectPage(page, resource);
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
  public void addResource(Resource resource) throws Exception {
    String resourceStr = resource.getResource();
    ApiAlertException.throwIfNull(resourceStr, "Please add pom or jar resource.");

    // check
    Dependency dependency = Dependency.toDependency(resourceStr);
    List<String> jars = dependency.getJar();
    List<MavenPom> poms = dependency.getPom();

    ApiAlertException.throwIfTrue(
        jars.isEmpty() && poms.isEmpty(), "Please add pom or jar resource.");

    ApiAlertException.throwIfTrue(
        resource.getResourceType() == ResourceTypeEnum.FLINK_APP && jars.isEmpty(),
        "Please upload jar for Flink_App resource");

    ApiAlertException.throwIfTrue(
        jars.size() + poms.size() > 1, "Please do not add multi dependency at one time.");
    // 处理 resource
    ResourceTypeHandleFactory.getResourceHandle(resource.getResourceType(), this)
        .handleResource(resource);
    ApiAlertException.throwIfTrue(
        this.findByResourceName(resource.getTeamId(), resource.getResourceName()) != null,
        String.format("the resource %s already exists, please check.", resource.getResourceName()));

    if (!jars.isEmpty()) {
      String resourcePath = jars.get(0);
      resource.setResourcePath(resourcePath);
      // copy jar to team upload directory
      String upFile = resourcePath.split(":")[1];
      transferTeamResource(resource.getTeamId(), upFile);
    }

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

      Dependency dependency = Dependency.toDependency(resource.getResource());
      if (!dependency.getJar().isEmpty()) {
        String jarFile = dependency.getJar().get(0).split(":")[1];
        transferTeamResource(findResource.getTeamId(), jarFile);
      }
    }

    findResource.setDescription(resource.getDescription());
    baseMapper.updateById(findResource);
  }

  @Override
  public void remove(Resource resource) {
    Resource findResource = getById(resource.getId());
    checkOrElseAlert(findResource);

    String filePath =
        String.format(
            "%s/%d/%s",
            Workspace.local().APP_UPLOADS(),
            findResource.getTeamId(),
            findResource.getResourceName());

    if (!new File(filePath).exists() && StringUtils.isNotBlank(findResource.getFilePath())) {
      filePath = findResource.getFilePath();
    }

    FsOperator.lfs().delete(filePath);

    this.removeById(resource);
  }

  public List<Resource> listByTeamId(Long teamId) {
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
  public void changeOwnership(Long userId, Long targetUserId) {
    LambdaUpdateWrapper<Resource> updateWrapper =
        new LambdaUpdateWrapper<Resource>()
            .eq(Resource::getCreatorId, userId)
            .set(Resource::getCreatorId, targetUserId);
    this.baseMapper.update(null, updateWrapper);
  }

  /**
   * @param file
   * @return
   */
  @Override
  public String upload(MultipartFile file) throws IOException {
    File temp = WebUtils.getAppTempDir();
    String fileName = FilenameUtils.getName(Objects.requireNonNull(file.getOriginalFilename()));
    File saveFile = new File(temp, fileName);
    if (!saveFile.exists()) {
      // save file to temp dir
      try {
        file.transferTo(saveFile);
      } catch (Exception e) {
        throw new ApiDetailException(e);
      }
    }
    return saveFile.getAbsolutePath();
  }

  @Override
  public RestResponse checkResource(Resource resourceParam) throws Exception {
    return ResourceTypeHandleFactory.getResourceHandle(resourceParam.getResourceType(), this)
        .checkResource(resourceParam);
  }

  public void transferTeamResource(Long teamId, String resourcePath) {
    String teamUploads = String.format("%s/%d", Workspace.local().APP_UPLOADS(), teamId);
    if (!FsOperator.lfs().exists(teamUploads)) {
      FsOperator.lfs().mkdirs(teamUploads);
    }
    File localJar = new File(resourcePath);
    File teamUploadJar = new File(teamUploads, localJar.getName());
    ApiAlertException.throwIfFalse(
        localJar.exists(), "Missing file: " + resourcePath + ", please upload again");
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
    List<Application> applications = applicationManageService.listByTeamId(resource.getTeamId());
    Map<Long, Application> applicationMap =
        applications.stream()
            .collect(Collectors.toMap(Application::getId, application -> application));

    // Get the application that depends on this resource
    List<FlinkSql> flinkSqls = flinkSqlService.listByTeamId(resource.getTeamId());
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
