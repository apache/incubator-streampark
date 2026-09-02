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

package org.apache.streampark.flink.packer.pipeline;

import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.DependencyInfo;
import org.apache.streampark.flink.packer.maven.MavenTool;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Shared dependency helpers for Flink SQL build pipelines. */
public final class FlinkSqlDependencySupport {

    private static final Artifact SNAKEYAML = new Artifact("org.yaml", "snakeyaml", "2.0");

    private FlinkSqlDependencySupport() {
    }

    public static DependencyInfo withSnakeyaml(DependencyInfo dependencyInfo) {
        Set<String> extJarLibs = new HashSet<>(dependencyInfo.extJarLibs());
        addSnakeyaml(extJarLibs);
        return new DependencyInfo(dependencyInfo.mavenArts(), extJarLibs);
    }

    public static void addSnakeyaml(Set<String> extJarLibs) {
        String appHome = System.getProperty("app.home", "/streampark");
        File snakeyaml = new File(appHome, "lib/snakeyaml-2.0.jar");
        if (snakeyaml.isFile()) {
            extJarLibs.add(snakeyaml.getAbsolutePath());
            return;
        }
        try {
            MavenTool.resolveArtifacts(Collections.singleton(SNAKEYAML))
                .stream()
                .map(File::getAbsolutePath)
                .forEach(extJarLibs::add);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve snakeyaml for Flink SQL application build", e);
        }
    }
}
