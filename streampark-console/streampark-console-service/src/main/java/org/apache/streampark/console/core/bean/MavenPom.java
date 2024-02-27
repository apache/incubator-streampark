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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class MavenPom {
  private String groupId;
  private String artifactId;
  private String version;
  private String classifier;
  private Set<MavenExclusion> exclusions = Collections.emptySet();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MavenPom that = (MavenPom) o;

    boolean basic =
        this.groupId.equals(that.groupId)
            && this.artifactId.equals(that.artifactId)
            && this.version.equals(that.version);

    boolean classify =
        StringUtils.isAllBlank(this.classifier, that.classifier)
            || this.classifier.equals(that.classifier);

    if (basic && classify) {
      Set<MavenExclusion> thisEx =
          this.exclusions == null ? Collections.emptySet() : this.exclusions;
      Set<MavenExclusion> thatEx =
          that.exclusions == null ? Collections.emptySet() : that.exclusions;
      return thisEx.size() == thatEx.size() && thisEx.containsAll(thatEx);
    }
    return false;
  }

  public String artifactName() {
    if (StringUtils.isBlank(classifier)) {
      return String.format("%s-%s.jar", artifactId, version);
    }
    return String.format("%s-%s-%s.jar", artifactId, version, classifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, classifier);
  }

  @Override
  public String toString() {
    return groupId + ":" + artifactId + ":" + version + getClassifier();
  }

  public Set<String> toExclusionString() {
    return this.exclusions.stream()
        .map(x -> String.format("%s:%s", x.getGroupId(), x.getArtifactId()))
        .collect(Collectors.toSet());
  }

  public String getClassifier() {
    return StringUtils.isBlank(classifier) ? "" : ":" + classifier;
  }
}
