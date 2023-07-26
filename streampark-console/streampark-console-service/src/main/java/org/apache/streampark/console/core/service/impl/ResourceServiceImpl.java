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
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.bean.Dependency;
import org.apache.streampark.console.core.bean.FlinkConnectorResource;
import org.apache.streampark.console.core.bean.Pom;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.ResourceType;
import org.apache.streampark.console.core.mapper.ResourceMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.MavenTool;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.factories.Factory;
import org.apache.hadoop.shaded.org.apache.commons.codec.digest.DigestUtils;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource>
    implements ResourceService {

  @Autowired private ApplicationService applicationService;
  @Autowired private CommonService commonService;
  @Autowired private FlinkSqlService flinkSqlService;

  public ResourceServiceImpl() {}

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
    String resourceStr = resource.getResource();
    ApiAlertException.throwIfNull(resourceStr, "Please add pom or jar resource.");

    if (resource.getResourceType() == ResourceType.GROUP) {
      ApiAlertException.throwIfNull(
          resource.getResourceName(), "The name of resource group is required.");
    } else {
      Dependency dependency = Dependency.toDependency(resourceStr);
      List<String> jars = dependency.getJar();
      List<Pom> poms = dependency.getPom();

      ApiAlertException.throwIfTrue(
          jars.isEmpty() && poms.isEmpty(), "Please add pom or jar resource.");
      ApiAlertException.throwIfTrue(
          jars.size() + poms.size() > 1, "Please do not add multi dependency at one time.");
      ApiAlertException.throwIfTrue(
          resource.getResourceType() == ResourceType.FLINK_APP && jars.isEmpty(),
          "Please upload jar for Flink_App resource");

      Long teamId = resource.getTeamId();
      String resourceName = null;

      if (poms.isEmpty()) {
        resourceName = jars.get(0);
        ApiAlertException.throwIfTrue(
            this.findByResourceName(teamId, resourceName) != null,
            String.format("Sorry, the resource %s already exists.", resourceName));

        // copy jar to team upload directory
        transferTeamResource(teamId, resourceName);
      } else {
        Pom pom = poms.get(0);
        resourceName =
            String.format("%s:%s:%s", pom.getGroupId(), pom.getArtifactId(), pom.getVersion());
        if (StringUtils.isNotBlank(pom.getClassifier())) {
          resourceName = resourceName + ":" + pom.getClassifier();
        }
        ApiAlertException.throwIfTrue(
            this.findByResourceName(teamId, resourceName) != null,
            String.format("Sorry, the resource %s already exists.", resourceName));
      }

      resource.setResourceName(resourceName);
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

    String name = file.getOriginalFilename();
    String suffix = name.substring(name.lastIndexOf("."));

    String sha256Hex = DigestUtils.sha256Hex(file.getInputStream());
    String fileName = sha256Hex.concat(suffix);

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
  public RestResponse checkResource(Resource resource) {
    ResourceType type = resource.getResourceType();
    switch (type) {
      case FLINK_APP:
        // check main.
        File jarFile = null;
        try {
          jarFile = getResourceJar(resource);
        } catch (Exception e) {
          // get jarFile error
          return RestResponse.success().data(1);
        }
        Manifest manifest = Utils.getJarManifest(jarFile);
        String mainClass = manifest.getMainAttributes().getValue("Main-Class");
        if (mainClass == null) {
          // main class is null
          return RestResponse.success().data(2);
        }
        // successful.
        return RestResponse.success().data(0);
      case CONNECTOR:
        // 1) get connector id
        List<FlinkConnectorResource> connectorResources;
        try {
          connectorResources = getConnectorResource(resource);
        } catch (Exception e) {
          return RestResponse.success().data(1);
        }

        if (Utils.isEmpty(connectorResources)) {
          // connector id is null
          return RestResponse.success().data(2);
        }
        // 2) check connector exists
        List<String> connectorIds =
            connectorResources.stream()
                .map(FlinkConnectorResource::getFactoryIdentifier)
                .collect(Collectors.toList());

        boolean exists = existsResourceByConnectorIds(connectorIds);
        if (exists) {
          return RestResponse.success(3);
        }
        return RestResponse.success().data(0);
    }
    return RestResponse.success().data(0);
  }

  private boolean existsResourceByConnectorIds(List<String> connectorIds) {
    return false;
  }

  @Override
  public List<FlinkConnectorResource> getConnectorResource(Resource resource) throws Exception {

    ApiAlertException.throwIfFalse(
        !ResourceType.CONNECTOR.equals(resource.getResourceType()),
        "getConnectorId method error, resource not flink connector.");

    Dependency dependency = Dependency.toDependency(resource.getResource());
    List<File> jars;
    if (!dependency.getPom().isEmpty()) {
      // 1) pom
      Artifact artifact = dependency.toArtifact().get(0);
      jars = MavenTool.resolveArtifacts(artifact);
    } else {
      // 2) jar
      String jar = dependency.getJar().get(0);
      jars = Collections.singletonList(new File(WebUtils.getAppTempDir(), jar));
    }

    Class<Factory> className = Factory.class;
    URL[] array =
        jars.stream()
            .map(
                x -> {
                  try {
                    return x.toURI().toURL();
                  } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                  }
                })
            .toArray(URL[]::new);

    try (URLClassLoader urlClassLoader = URLClassLoader.newInstance(array)) {
      ServiceLoader<Factory> serviceLoader = ServiceLoader.load(className, urlClassLoader);
      List<FlinkConnectorResource> connectorResources = new ArrayList<>();
      for (Factory factory : serviceLoader) {
        String factoryClassName = factory.getClass().getName();
        if (!factoryClassName.equals("org.apache.flink.table.module.CoreModuleFactory")) {
          FlinkConnectorResource connectorResource = new FlinkConnectorResource();
          connectorResource.setClassName(factoryClassName);
          connectorResource.setFactoryIdentifier(factory.factoryIdentifier());
          connectorResource.setRequiredOptions(factory.requiredOptions());
          connectorResource.setOptionalOptions(factory.optionalOptions());
          connectorResources.add(connectorResource);
        }
      }
      return connectorResources;
    }
  }

  private File getResourceJar(Resource resource) throws Exception {
    Dependency dependency = Dependency.toDependency(resource.getResource());
    if (dependency.isEmpty()) {
      return null;
    }
    if (!dependency.getJar().isEmpty()) {
      String jar = dependency.getJar().get(0);
      return new File(WebUtils.getAppTempDir(), jar);
    } else {
      Artifact artifact = dependency.toArtifact().get(0);
      List<File> files = MavenTool.resolveArtifacts(artifact);
      if (!files.isEmpty()) {
        String fileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
        Optional<File> jarFile =
            files.stream().filter(x -> x.getName().equals(fileName)).findFirst();
        if (jarFile.isPresent()) {
          return jarFile.get();
        }
      }
      return null;
    }
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
