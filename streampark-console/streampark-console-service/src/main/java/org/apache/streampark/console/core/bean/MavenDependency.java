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

package org.apache.streampark.console.core.bean;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.MavenArtifact;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class MavenDependency {

  private Set<MavenPom> pom = Collections.emptySet();

  private Set<String> jar = Collections.emptySet();

  @SneakyThrows
  public static MavenDependency of(String dependency) {
    if (StringUtils.isNotBlank(dependency)) {
      dependency = dependency.replaceAll(",\\s*\"exclusions\"\\s*:\\s*\\{\\s*}", "");
      return JacksonUtils.read(dependency, MavenDependency.class);
    }
    return new MavenDependency();
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }

    if (that == null || getClass() != that.getClass()) {
      return false;
    }

    MavenDependency thatDep = (MavenDependency) that;

    if (this.pom.size() != thatDep.pom.size()
        || this.jar.size() != thatDep.jar.size()
        || !this.pom.containsAll(thatDep.pom)) {
      return false;
    }

    File localJar = WebUtils.getAppTempDir();
    File localUploads = new File(Workspace.local().APP_UPLOADS());
    for (String jarName : jar) {
      if (!thatDep.jar.contains(jarName)
          || !FileUtils.equals(new File(localJar, jarName), new File(localUploads, jarName))) {
        return false;
      }
    }

    return true;
  }

  public MavenArtifact toMavenArtifact() {
    List<Artifact> mvnArts =
        this.pom.stream()
            .map(
                pom ->
                    new Artifact(
                        pom.getGroupId(),
                        pom.getArtifactId(),
                        pom.getVersion(),
                        pom.getClassifier(),
                        pom.toExclusionString()))
            .collect(Collectors.toList());
    List<String> extJars =
        this.jar.stream()
            .map(jar -> Workspace.local().APP_UPLOADS() + "/" + jar)
            .collect(Collectors.toList());
    return new MavenArtifact(mvnArts, extJars);
  }
}
