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

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class MavenExclusion {

  private String groupId;

  private String artifactId;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MavenExclusion that = (MavenExclusion) o;
    if (this.groupId == null || that.groupId == null) {
      return false;
    }
    if (this.artifactId == null || that.artifactId == null) {
      return false;
    }
    return this.groupId.equals(that.groupId) && this.artifactId.equals(that.artifactId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId);
  }
}
