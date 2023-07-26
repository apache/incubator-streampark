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
import org.apache.streampark.common.util.ClassLoaderUtils;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.MavenTool;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.table.factories.Factory;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.Set;
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
    if (!files.isEmpty()) {
      Class<Factory> className = Factory.class;
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      files.forEach(x -> ClassLoaderUtils.loadJar(x.getAbsolutePath()));
      ServiceLoader<Factory> serviceLoader = ServiceLoader.load(className, classLoader);
      for (Factory factory : serviceLoader) {
        String name = factory.getClass().getName();
        String id = factory.factoryIdentifier();
        System.out.println("id: " + id + "          class :" + name);

        Set<ConfigOption<?>> requiredOptions = factory.requiredOptions();
        System.out.println(" ------------requiredOptions---------- ");
        requiredOptions.forEach(
            x -> System.out.println(x.key() + "         defValue: " + x.defaultValue()));

        System.out.println(" ------------optionalOptions---------- ");
        Set<ConfigOption<?>> options = factory.optionalOptions();
        options.forEach(
            x -> System.out.println(x.key() + "            defValue: " + x.defaultValue()));

        System.out.println();
      }
    }
  }

  @Test
  public void resolveFlinkConnector2() throws Exception {
    Artifact artifact =
        new Artifact("org.apache.flink", "flink-connector-elasticsearch6", "3.0.1-1.17", null);

    InternalConfigHolder.set(CommonConfig.STREAMPARK_WORKSPACE_LOCAL(), "~/tmp");
    List<File> files = MavenTool.resolveArtifacts(artifact);

    String path = null;
    if (!files.isEmpty()) {
      String fileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
      Optional<File> jarFile = files.stream().filter(x -> x.getName().equals(fileName)).findFirst();
      if (jarFile.isPresent()) {
        path = jarFile.get().getAbsolutePath();
      }
    }

    String configFile = "META-INF/services/org.apache.flink.table.factories.Factory";

    JarFile jarFile = new JarFile(path);
    JarEntry entry = jarFile.getJarEntry(configFile);

    List<String> factories = new ArrayList<>(0);
    InputStream inputStream = jarFile.getInputStream(entry);
    Scanner scanner = new Scanner(new InputStreamReader(inputStream));
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine().trim();
      if (line.length() > 0 && !line.startsWith("#")) {
        factories.add(line);
      }
    }

    factories.forEach(System.out::println);
    for (String factory : factories) {
      String packageName = factory.replace('.', '/') + ".class";
      JarEntry classEntry = jarFile.getJarEntry(packageName);
      InputStream in = jarFile.getInputStream(classEntry);

      // TODO parse connector
      System.out.println(in);
    }
  }
}
