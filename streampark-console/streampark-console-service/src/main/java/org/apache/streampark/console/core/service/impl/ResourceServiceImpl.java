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

import org.apache.streampark.common.Constant;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.exception.ApiAlertException;
import org.apache.streampark.common.exception.ApiDetailException;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.PremisesUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.bean.Dependency;
import org.apache.streampark.console.core.bean.FlinkConnector;
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
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.MavenTool;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.table.factories.Factory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource>
    implements ResourceService {

  public static final String STATE = "state";
  public static final String EXCEPTION = "exception";

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
    PremisesUtils.throwIfNull(
        resourceStr, "Please add pom or jar resource.", ApiAlertException.class);

    // check
    Dependency dependency = Dependency.toDependency(resourceStr);
    List<String> jars = dependency.getJar();
    List<MavenPom> poms = dependency.getPom();
    check(resource, jars, poms);

    if (resource.getResourceType() == ResourceTypeEnum.CONNECTOR) {
      processConnectorResource(resource);
    } else {
      ApiAlertException.throwIfNull(resource.getResourceName(), "The resourceName is required.");
    }

    ApiAlertException.throwIfNotNull(
        this.findByResourceName(resource.getTeamId(), resource.getResourceName()),
        "the resource %s already exists, please check.",
        resource.getResourceName());

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

  private static void processConnectorResource(Resource resource) throws JsonProcessingException {
    String connector = resource.getConnector();
    PremisesUtils.throwIfNull(connector, "the flink connector is null.");
    FlinkConnector connectorResource = JacksonUtils.read(connector, FlinkConnector.class);
    resource.setResourceName(connectorResource.getFactoryIdentifier());
    Optional.ofNullable(connectorResource.getRequiredOptions())
        .ifPresent(
            v ->
                resource.setConnectorRequiredOptions(
                    ExceptionUtils.wrapRuntimeException(v, JacksonUtils::write)));
    Optional.ofNullable(connectorResource.getOptionalOptions())
        .ifPresent(
            v ->
                resource.setConnectorOptionalOptions(
                    ExceptionUtils.wrapRuntimeException(v, JacksonUtils::write)));
  }

  private void check(Resource resource, List<String> jars, List<MavenPom> poms) {
    ApiAlertException.throwIfTrue(
        jars.isEmpty() && poms.isEmpty(), "Please add pom or jar resource.");
    ApiAlertException.throwIfTrue(
        resource.getResourceType() == ResourceTypeEnum.FLINK_APP && jars.isEmpty(),
        "Please upload jar for Flink_App resource");
    ApiAlertException.throwIfTrue(
        jars.size() + poms.size() > 1, "Please do not add multi dependency at one time.");
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
      PremisesUtils.throwIfFalse(
          resourceName.equals(findResource.getResourceName()),
          "Please make sure the resource name is not changed.",
          ApiAlertException.class);

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
  public void remove(Long id) {
    Resource findResource = getById(id);
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

    this.removeById(id);
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
  public RestResponse checkResource(Resource resourceParam) throws JsonProcessingException {
    ResourceTypeEnum type = resourceParam.getResourceType();
    switch (type) {
      case FLINK_APP:
        return checkFlinkApp(resourceParam);
      case CONNECTOR:
        return checkConnector(resourceParam);
    }
    return RestResponse.success().data(ImmutableMap.of(STATE, 0));
  }

  private RestResponse checkConnector(Resource resourceParam) throws JsonProcessingException {
    // 1) get connector jar
    FlinkConnector connectorResource;
    List<File> jars;
    File connector;
    List<String> factories;
    try {
      File file = getResourceJar(resourceParam);
      connector = file;
      jars = Collections.singletonList(file);
    } catch (Exception e) {
      // get jarFile error
      return buildExceptResponse(e, 1);
    }

    // 2) parse connector Factory
    try {
      factories = getConnectorFactory(connector);
    } catch (Exception e) {
      // flink connector invalid
      return buildExceptResponse(e, 2);
    }

    // 3) get connector resource
    connectorResource = getConnectorResource(jars, factories);
    if (connectorResource == null) {
      // connector is null
      return buildExceptResponse(new RuntimeException("connector is null"), 3);
    }

    // 2) check connector exists
    boolean exists =
        existsFlinkConnector(resourceParam.getId(), connectorResource.getFactoryIdentifier());
    if (exists) {
      return buildExceptResponse(new RuntimeException("connector is already exists"), 4);
    }

    if (resourceParam.getId() != null
        && !(getById(resourceParam.getId())
            .getResourceName()
            .equals(connectorResource.getFactoryIdentifier()))) {
      return buildExceptResponse(
          new RuntimeException("resource name different with FactoryIdentifier"), 5);
    }
    return RestResponse.success()
        .data(ImmutableMap.of(STATE, 0, "connector", JacksonUtils.write(connectorResource)));
  }

  private static RestResponse buildExceptResponse(Exception e, int code) {
    return RestResponse.success()
        .data(ImmutableMap.of(STATE, code, EXCEPTION, ExceptionUtils.stringifyException(e)));
  }

  private RestResponse checkFlinkApp(Resource resourceParam) {
    // check main.
    File jarFile;
    try {
      jarFile = getResourceJar(resourceParam);
    } catch (Exception e) {
      // get jarFile error
      return buildExceptResponse(e, 1);
    }
    PremisesUtils.throwIfTrue(
        jarFile == null, "flink app jar must exist.", ApiAlertException.class);
    Map<String, Serializable> resp = new HashMap<>(0);
    resp.put(STATE, 0);
    if (jarFile.getName().endsWith(Constant.PYTHON_SUFFIX)) {
      return RestResponse.success().data(resp);
    }
    String mainClass = Utils.getJarManClass(jarFile);
    if (mainClass == null) {
      // main class is null
      return buildExceptResponse(new RuntimeException("main class is null"), 2);
    }
    return RestResponse.success().data(resp);
  }

  private boolean existsFlinkConnector(Long id, String connectorId) {
    LambdaQueryWrapper<Resource> lambdaQueryWrapper =
        new LambdaQueryWrapper<Resource>().eq(Resource::getResourceName, connectorId);
    if (id != null) {
      lambdaQueryWrapper.ne(Resource::getId, id);
    }
    return getBaseMapper().exists(lambdaQueryWrapper);
  }

  private FlinkConnector getConnectorResource(List<File> jars, List<String> factories) {
    Class<Factory> className = Factory.class;
    URL[] array =
        jars.stream()
            .map(
                file -> ExceptionUtils.wrapRuntimeException(file, handle -> handle.toURI().toURL()))
            .toArray(URL[]::new);

    try (URLClassLoader urlClassLoader = URLClassLoader.newInstance(array)) {
      ServiceLoader<Factory> serviceLoader = ServiceLoader.load(className, urlClassLoader);
      for (Factory factory : serviceLoader) {
        String factoryClassName = factory.getClass().getName();
        if (factories.contains(factoryClassName)) {
          FlinkConnector connectorResource = new FlinkConnector();
          try {
            connectorResource.setClassName(factoryClassName);
            connectorResource.setFactoryIdentifier(factory.factoryIdentifier());
          } catch (Exception ignored) {
          }

          try {
            Map<String, String> requiredOptions = new HashMap<>(0);
            factory
                .requiredOptions()
                .forEach(x -> requiredOptions.put(x.key(), getOptionDefaultValue(x)));
            connectorResource.setRequiredOptions(requiredOptions);
          } catch (Exception ignored) {

          }

          try {
            Map<String, String> optionalOptions = new HashMap<>(0);
            factory
                .optionalOptions()
                .forEach(x -> optionalOptions.put(x.key(), getOptionDefaultValue(x)));
            connectorResource.setOptionalOptions(optionalOptions);
          } catch (Exception ignored) {
          }
          return connectorResource;
        }
      }
      return null;
    } catch (Exception e) {
      log.error("getConnectorResource failed. " + e);
    }
    return null;
  }

  private File getResourceJar(Resource resource) throws Exception {
    Dependency dependency = Dependency.toDependency(resource.getResource());
    if (dependency.isEmpty()) {
      return null;
    }
    if (!dependency.getJar().isEmpty()) {
      String jar = dependency.getJar().get(0).split(":")[1];
      return new File(jar);
    } else {
      Artifact artifact = dependency.toArtifact().get(0);
      List<File> files = MavenTool.resolveArtifacts(artifact);
      if (!files.isEmpty()) {
        String fileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
        Optional<File> jarFile =
            files.stream().filter(x -> x.getName().equals(fileName)).findFirst();
        jarFile.ifPresent(
            file -> transferTeamResource(resource.getTeamId(), file.getAbsolutePath()));
        return jarFile.orElse(null);
      }
      return null;
    }
  }

  private void transferTeamResource(Long teamId, String resourcePath) {
    String teamUploads = String.format("%s/%d", Workspace.local().APP_UPLOADS(), teamId);
    if (!FsOperator.lfs().exists(teamUploads)) {
      FsOperator.lfs().mkdirs(teamUploads);
    }
    File localJar = new File(resourcePath);
    File teamUploadJar = new File(teamUploads, localJar.getName());
    PremisesUtils.throwIfFalse(
        localJar.exists(),
        "Missing file: " + resourcePath + ", please upload again",
        ApiAlertException.class);
    FsOperator.lfs()
        .upload(localJar.getAbsolutePath(), teamUploadJar.getAbsolutePath(), false, true);
  }

  private void checkOrElseAlert(Resource resource) {
    PremisesUtils.throwIfNull(resource, "The resource does not exist.", ApiAlertException.class);

    PremisesUtils.throwIfTrue(
        isDependByApplications(resource),
        "Sorry, the resource is still in use, cannot be removed.",
        ApiAlertException.class);
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

  private List<String> getConnectorFactory(File connector) throws Exception {
    String configFile = "META-INF/services/org.apache.flink.table.factories.Factory";
    JarFile jarFile = new JarFile(connector);
    JarEntry entry = jarFile.getJarEntry(configFile);
    if (entry == null) {
      throw new IllegalArgumentException("invalid flink connector");
    }
    List<String> factories = new ArrayList<>(0);
    try (InputStream inputStream = jarFile.getInputStream(entry)) {
      Scanner scanner = new Scanner(new InputStreamReader(inputStream));
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine().trim();
        if (line.length() > 0 && !line.startsWith("#")) {
          factories.add(line);
        }
      }
      scanner.close();
    }
    return factories;
  }

  private String getOptionDefaultValue(ConfigOption<?> option) {
    if (!option.hasDefaultValue()) {
      return null;
    }
    Object value = option.defaultValue();
    if (value instanceof Duration) {
      return value.toString().replace("PT", "").toLowerCase();
    }
    return value.toString();
  }
}
