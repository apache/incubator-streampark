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

package org.apache.streampark.console.base.util;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.console.core.bean.FlinkConnectorResource;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.MavenTool;

import org.apache.flink.table.factories.Factory;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Slf4j
class DependencyUtilsTest {

  @Test
  public void resolveFlinkConnector() throws Exception {

    Artifact artifact = new Artifact("com.ververica", "flink-connector-mysql-cdc", "2.4.1", null);

    InternalConfigHolder.set(CommonConfig.STREAMPARK_WORKSPACE_LOCAL(), "~/tmp");

    List<File> files = MavenTool.resolveArtifacts(artifact);
    if (files.isEmpty()) {
      return;
    }

    Class<Factory> className = Factory.class;
    URL[] array =
        files.stream()
            .map(
                x -> {
                  try {
                    return x.toURI().toURL();
                  } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                  }
                })
            .toArray(URL[]::new);

    URLClassLoader urlClassLoader = URLClassLoader.newInstance(array);
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
    urlClassLoader.close();
    System.out.println(connectorResources);
  }
}
