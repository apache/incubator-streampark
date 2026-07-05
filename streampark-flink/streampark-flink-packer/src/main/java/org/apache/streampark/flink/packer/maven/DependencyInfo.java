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
package org.apache.streampark.flink.packer.maven;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependencyInfo {
    private final Set<Artifact> mavenArts;
    private final Set<String> extJarLibs;

    public DependencyInfo() {
        this(Collections.emptySet(), Collections.emptySet());
    }

    public DependencyInfo(Set<Artifact> mavenArts, Set<String> extJarLibs) {
        this.mavenArts = mavenArts == null ? Collections.emptySet() : mavenArts;
        this.extJarLibs = extJarLibs == null ? Collections.emptySet() : extJarLibs;
    }

    public DependencyInfo(List<Artifact> mavenArts, List<String> extJarLibs) {
        this(new HashSet<>(mavenArts), new HashSet<>(extJarLibs));
    }

    public Set<Artifact> mavenArts() { return mavenArts; }
    public Set<String> extJarLibs() { return extJarLibs; }

    public DependencyInfo merge(Set<String> jarLibs) {
        if (jarLibs == null) return this;
        Set<String> merged = new HashSet<>(extJarLibs);
        merged.addAll(jarLibs);
        return new DependencyInfo(mavenArts, merged);
    }

    public DependencyInfo merge(List<Artifact> mvnPoms, List<String> jarLibs) {
        Set<Artifact> arts = new HashSet<>(mavenArts);
        Set<String> libs = new HashSet<>(extJarLibs);
        if (mvnPoms != null) arts.addAll(mvnPoms);
        if (jarLibs != null) libs.addAll(jarLibs);
        return new DependencyInfo(arts, libs);
    }

    public static DependencyInfo empty() { return new DependencyInfo(); }
}
