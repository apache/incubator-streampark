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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
class DependencyUtilsTest {

  @Test
  public void resolveFlinkConnector() throws Exception {

    Artifact artifact =
        new Artifact("org.apache.flink", "flink-connector-hive_2.12", "1.17.1", null);

    InternalConfigHolder.set(CommonConfig.STREAMPARK_WORKSPACE_LOCAL(), "~/tmp");

    List<File> files = MavenTool.resolveArtifacts(artifact);
    if (files.isEmpty()) {
      return;
    }

    String fileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
    Optional<File> jarFile = files.stream().filter(x -> x.getName().equals(fileName)).findFirst();
    File connector = jarFile.get();

    List<String> factories = getConnectorFactory(connector);

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
    try {
      for (Factory factory : serviceLoader) {
        String factoryClassName = factory.getClass().getName();
        if (factories.contains(factoryClassName)) {
          FlinkConnectorResource connectorResource = new FlinkConnectorResource();
          connectorResource.setClassName(factoryClassName);
          connectorResource.setFactoryIdentifier(factory.factoryIdentifier());
          try {
            connectorResource.setRequiredOptions(factory.requiredOptions());
            connectorResource.setOptionalOptions(factory.optionalOptions());
          } catch (Throwable e) {
            //
          }
          connectorResources.add(connectorResource);
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    urlClassLoader.close();
    System.out.println(connectorResources);
  }

  private List<String> getConnectorFactory(File connector) throws Exception {
    String configFile = "META-INF/services/org.apache.flink.table.factories.Factory";
    JarFile jarFile = new JarFile(connector);
    JarEntry entry = jarFile.getJarEntry(configFile);
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
}
