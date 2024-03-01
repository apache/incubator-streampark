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

package org.apache.streampark.console.core.service.resource.impl;

import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.bean.Dependency;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.resource.ResourceHandle;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.MavenTool;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.List;
import java.util.Optional;

public abstract class AbstractResourceHandle implements ResourceHandle {

  public static final String STATE = "state";
  public static final String EXCEPTION = "exception";

  protected final ResourceService resourceService;

  protected AbstractResourceHandle(ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  protected RestResponse buildExceptResponse(Exception e, int code) {
    return RestResponse.success()
        .data(ImmutableMap.of(STATE, code, EXCEPTION, ExceptionUtils.stringifyException(e)));
  }

  @Override
  public void handleResource(Resource resource) throws Exception {
    ApiAlertException.throwIfNull(resource.getResourceName(), "The resourceName is required.");
  }

  protected File getResourceJar(Resource resource) throws Exception {
    Dependency dependency = Dependency.toDependency(resource.getResource());
    if (dependency.isEmpty()) {
      return null;
    }
    if (!dependency.getJar().isEmpty()) {
      String jar = dependency.getJar().get(0).split(":")[1];
      return new File(jar);
    } else {
      Artifact artifact = dependency.toArtifact().get(0);
      List<File> files = MavenTool.resolveArtifacts(artifact);
      if (!files.isEmpty()) {
        String fileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
        Optional<File> jarFile =
            files.stream().filter(x -> x.getName().equals(fileName)).findFirst();
        jarFile.ifPresent(
            file ->
                resourceService.transferTeamResource(resource.getTeamId(), file.getAbsolutePath()));
        return jarFile.orElse(null);
      }
      return null;
    }
  }
}
