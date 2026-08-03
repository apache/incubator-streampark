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
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.DependencyInfo;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class Dependency {

    private List<MavenPom> pom = Collections.emptyList();
    private List<String> jar = Collections.emptyList();

    @SneakyThrows
    public static Dependency toDependency(String dependency) {
        if (Utils.isNotEmpty(dependency)) {
            return JacksonUtils.read(dependency, new TypeReference<Dependency>() {
            });
        }
        return new Dependency();
    }

    public boolean isEmpty() {
        return pom.isEmpty() && jar.isEmpty();
    }

    public boolean equals(Dependency other) {
        if (other == null) {
            return false;
        }
        if (this.isEmpty() && other.isEmpty()) {
            return true;
        }

        if (this.pom.size() != other.pom.size() || this.jar.size() != other.jar.size()) {
            return false;
        }
        File localJar = WebUtils.getAppTempDir();
        File localUploads = new File(Workspace.local().APP_UPLOADS());
        Set<String> otherJars = new HashSet<>(other.jar);
        for (String jarName : jar) {
            if (!otherJars.contains(jarName)
                || !FileUtils.equals(new File(localJar, jarName), new File(localUploads, jarName))) {
                return false;
            }
        }
        return new HashSet<>(pom).containsAll(other.pom);
    }

    public DependencyInfo toJarPackDeps() {
        List<Artifact> mvnArts = toArtifact();
        List<String> extJars = this.jar.stream()
            .map(jar -> Workspace.local().APP_UPLOADS() + "/" + jar)
            .collect(Collectors.toList());
        return new DependencyInfo(mvnArts, extJars);
    }

    public List<Artifact> toArtifact() {
        return this.pom.stream()
            .map(
                pom -> new Artifact(
                    pom.getGroupId(), pom.getArtifactId(), pom.getVersion(),
                    pom.getClassifier()))
            .collect(Collectors.toList());
    }
}
