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

import javax.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Maven artifact coordinates. */
public final class Artifact {

    private static final Pattern PATTERN = Pattern.compile("([^: ]+):([^: ]+):([^: ]+)");

    private final String groupId;
    private final String artifactId;
    private final String version;
    @Nullable
    private final String classifier;

    public Artifact(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, null);
    }

    public Artifact(String groupId, String artifactId, String version, @Nullable String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    @Nullable
    public String classifier() {
        return classifier;
    }

    public boolean eq(org.eclipse.aether.artifact.Artifact artifact) {
        if (!groupId.equals(artifact.getGroupId())) {
            return false;
        }
        String a = artifact.getArtifactId();
        if ("*".equals(a)) {
            return true;
        }
        return artifactId.equals(a);
    }

    /** build from coords */
    public static Artifact of(String coords) {
        Matcher matcher = PATTERN.matcher(coords);
        if (matcher.matches()) {
            return new Artifact(matcher.group(1), matcher.group(2), matcher.group(3));
        }
        throw new IllegalArgumentException(
            "Bad artifact coordinates "
                + coords
                + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
    }
}
