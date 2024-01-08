package org.apache.streampark.console.core.service.resource.impl;

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

import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.bean.FlinkConnector;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.service.impl.ResourceServiceImpl;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.table.factories.Factory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class ConnectorResourceHandle extends AbstractResourceHandle {

  public ConnectorResourceHandle(ResourceServiceImpl resourceService) {
    super(resourceService);
  }

  @Override
  public RestResponse checkResource(Resource resourceParam) throws Exception {
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
        && !(resourceService
            .getById(resourceParam.getId())
            .getResourceName()
            .equals(connectorResource.getFactoryIdentifier()))) {
      return buildExceptResponse(
          new RuntimeException("resource name different with FactoryIdentifier"), 5);
    }
    return RestResponse.success()
        .data(ImmutableMap.of(STATE, 0, "connector", JacksonUtils.write(connectorResource)));
  }

  @Override
  public void handleResource(Resource resource) throws Exception {
    String connector = resource.getConnector();
    ApiAlertException.throwIfTrue(connector == null, "the flink connector is null.");
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
        if (!line.isEmpty() && !line.startsWith("#")) {
          factories.add(line);
        }
      }
      scanner.close();
    }
    return factories;
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
      throw new RuntimeException(e);
    }
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

  private boolean existsFlinkConnector(Long id, String connectorId) {
    LambdaQueryWrapper<Resource> lambdaQueryWrapper =
        new LambdaQueryWrapper<Resource>().eq(Resource::getResourceName, connectorId);
    if (id != null) {
      lambdaQueryWrapper.ne(Resource::getId, id);
    }
    return resourceService.getBaseMapper().exists(lambdaQueryWrapper);
  }
}
