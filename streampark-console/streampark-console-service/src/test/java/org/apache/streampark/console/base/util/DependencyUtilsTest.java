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

import org.apache.flink.table.factories.Factory;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

@Slf4j
class DependencyUtilsTest {

  @Test
  public void resolveFlinkConnector() throws Exception {
    Artifact artifact = new Artifact("org.apache.flink", "flink-connector-kafka", "1.17.1", null);

    InternalConfigHolder.set(
        CommonConfig.STREAMPARK_WORKSPACE_LOCAL(), "/Users/benjobs/Desktop/streamx_workspace");

    List<File> files = MavenTool.resolveArtifacts(artifact);
    if (!files.isEmpty()) {
      String fileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
      Optional<File> jarFile = files.stream().filter(x -> x.getName().equals(fileName)).findFirst();
      if (jarFile.isPresent()) {
        String jar = jarFile.get().getAbsolutePath();
        Class<Factory> className = Factory.class;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // 动态加载jar到classpath,
        // 优点:简单,
        // 缺点: 会污染当前主线程,每上传一个新的connector都会加载到当前的主线程中,导致加载太多的 class, ...
        ClassLoaderUtils.loadJar(jar);

        ServiceLoader<Factory> serviceLoader = ServiceLoader.load(className, classLoader);
        for (Factory factory : serviceLoader) {
          String name = factory.getClass().getName();
          String id = factory.factoryIdentifier();
          System.out.println(id + "      :     " + name);
        }
      }
    }
  }
}
