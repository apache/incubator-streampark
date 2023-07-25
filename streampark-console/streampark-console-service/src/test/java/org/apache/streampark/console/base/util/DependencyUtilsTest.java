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
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.MavenTool;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Slf4j
class DependencyUtilsTest {

  @Test
  void resolveMavenDependencies() throws Exception {
    Artifact artifact = new Artifact("org.apache.flink", "flink-table-common", "1.17.1", null);

    InternalConfigHolder.set(CommonConfig.STREAMPARK_WORKSPACE_LOCAL(), "~/workspace");
    List<File> files = MavenTool.resolveArtifacts(artifact);
    if (!files.isEmpty()) {
      String fileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
      Optional<File> jarFile = files.stream().filter(x -> x.getName().equals(fileName)).findFirst();
      if (jarFile.isPresent()) {
        String jar = jarFile.get().getAbsolutePath();
        System.out.println(jar);
      }
    }
  }
}
