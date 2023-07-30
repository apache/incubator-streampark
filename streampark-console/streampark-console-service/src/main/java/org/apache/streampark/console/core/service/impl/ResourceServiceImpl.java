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
import org.apache.streampark.console.base.util.JacksonUtils;
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
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.table.factories.Factory;
import org.apache.hadoop.shaded.org.apache.commons.codec.digest.DigestUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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
  public void addResource(Resource resource) throws Exception {
    String resourceStr = resource.getResource();
    ApiAlertException.throwIfNull(resourceStr, "Please add pom or jar resource.");

    // check
    Dependency dependency = Dependency.toDependency(resourceStr);
    List<String> jars = dependency.getJar();
    List<Pom> poms = dependency.getPom();

    ApiAlertException.throwIfTrue(
        jars.isEmpty() && poms.isEmpty(), "Please add pom or jar resource.");

    ApiAlertException.throwIfTrue(
        resource.getResourceType() == ResourceType.FLINK_APP && jars.isEmpty(),
        "Please upload jar for Flink_App resource");

    ApiAlertException.throwIfTrue(
        jars.size() + poms.size() > 1, "Please do not add multi dependency at one time.");

    if (resource.getResourceType() != ResourceType.CONNECTOR) {
      ApiAlertException.throwIfNull(resource.getResourceName(), "The resourceName is required.");
    } else {
      String connector = resource.getConnector();
      ApiAlertException.throwIfTrue(connector == null, "the flink connector is null.");
      FlinkConnectorResource connectorResource =
          JacksonUtils.read(connector, FlinkConnectorResource.class);
      resource.setResourceName(connectorResource.getFactoryIdentifier());
      if (connectorResource.getRequiredOptions() != null) {
        resource.setConnectorRequiredOptions(
            JacksonUtils.write(connectorResource.getRequiredOptions()));
      }
      if (connectorResource.getOptionalOptions() != null) {
        resource.setConnectorOptionalOptions(
            JacksonUtils.write(connectorResource.getOptionalOptions()));
      }
    }

    ApiAlertException.throwIfTrue(
        this.findByResourceName(resource.getTeamId(), resource.getResourceName()) != null,
        String.format("Sorry, the resource %s already exists.", resource.getResourceName()));

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
  public RestResponse checkResource(Resource resourceParam) throws JsonProcessingException {
    ResourceType type = resourceParam.getResourceType();
    Map<String, Serializable> resp = new HashMap<>(0);
    resp.put("state", 0);
    switch (type) {
      case FLINK_APP:
        // check main.
        File jarFile;
        try {
          jarFile = getResourceJar(resourceParam);
        } catch (Exception e) {
          // get jarFile error
          resp.put("state", 1);
          resp.put("exception", Utils.stringifyException(e));
          return RestResponse.success().data(resp);
        }
        Manifest manifest = Utils.getJarManifest(jarFile);
        String mainClass = manifest.getMainAttributes().getValue("Main-Class");

        if (mainClass == null) {
          // main class is null
          resp.put("state", 2);
          return RestResponse.success().data(resp);
        }
        return RestResponse.success().data(resp);
      case CONNECTOR:
        // 1) get connector id
        FlinkConnectorResource connectorResource;

        ApiAlertException.throwIfFalse(
            ResourceType.CONNECTOR.equals(resourceParam.getResourceType()),
            "getConnectorId method error, resource not flink connector.");

        List<File> jars;
        File connector = null;
        List<String> factories;

        Dependency dependency = Dependency.toDependency(resourceParam.getResource());

        // 1) get connector jar
        if (!dependency.getPom().isEmpty()) {
          Artifact artifact = dependency.toArtifact().get(0);
          try {
            jars = MavenTool.resolveArtifacts(artifact);
          } catch (Exception e) {
            // connector download is null
            resp.put("state", 1);
            resp.put("exception", Utils.stringifyException(e));
            return RestResponse.success().data(resp);
          }
          String fileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
          Optional<File> file = jars.stream().filter(x -> x.getName().equals(fileName)).findFirst();
          if (file.isPresent()) {
            connector = file.get();
          }
        } else {
          // 2) jar
          String jar = dependency.getJar().get(0).split(":")[1];
          File file = new File(jar);
          connector = file;
          jars = Collections.singletonList(file);
        }

        // 2) parse connector Factory
        try {
          factories = getConnectorFactory(connector);
        } catch (Exception e) {
          // flink connector invalid
          resp.put("state", 2);
          resp.put("exception", Utils.stringifyException(e));
          return RestResponse.success().data(resp);
        }

        // 3) get connector resource
        connectorResource = getConnectorResource(jars, factories);
        if (connectorResource == null) {
          // connector is null
          resp.put("state", 3);
          return RestResponse.success().data(resp);
        }

        // 2) check connector exists
        boolean exists =
            existsFlinkConnector(resourceParam.getId(), connectorResource.getFactoryIdentifier());
        if (exists) {
          resp.put("state", 4);
          resp.put("name", connectorResource.getFactoryIdentifier());
          return RestResponse.success(resp);
        }

        if (resourceParam.getId() != null) {
          Resource resource = getById(resourceParam.getId());
          if (!resource.getResourceName().equals(connectorResource.getFactoryIdentifier())) {
            resp.put("state", 5);
            return RestResponse.success().data(resp);
          }
        }
        resp.put("state", 0);
        resp.put("connector", JacksonUtils.write(connectorResource));
        return RestResponse.success().data(resp);
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

  private FlinkConnectorResource getConnectorResource(List<File> jars, List<String> factories) {
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
      for (Factory factory : serviceLoader) {
        String factoryClassName = factory.getClass().getName();
        if (factories.contains(factoryClassName)) {
          FlinkConnectorResource connectorResource = new FlinkConnectorResource();
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
        if (jarFile.isPresent()) {
          return jarFile.get();
        }
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
