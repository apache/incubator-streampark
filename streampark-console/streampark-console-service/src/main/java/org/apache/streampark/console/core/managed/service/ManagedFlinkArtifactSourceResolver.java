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

package org.apache.streampark.console.core.managed.service;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.service.ResourceService;

import org.apache.commons.lang3.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Resolves Team resources through the StreamPark file-system abstraction. */
@Component
@RequiredArgsConstructor
class ManagedFlinkArtifactSourceResolver {

    private final ResourceService resourceService;

    ResolvedArtifactSource resolve(Long teamId, String resourceName, long maxArtifactBytes) {
        Resource resource = resourceService.findByResourceName(teamId, resourceName);
        ApiAlertException.throwIfNull(
            resource, "Managed Flink artifact resource does not exist in this Team.");
        String sourcePath = canonicalResourcePath(resource);
        FsOperator fileSystem = FsOperator.lfs();
        ApiAlertException.throwIfFalse(
            fileSystem.exists(sourcePath), "Managed Flink artifact file does not exist.");
        String fileName =
            StringUtils.defaultIfBlank(resource.getFileName(), resource.getResourceName());
        ApiAlertException.throwIfTrue(
            StringUtils.isBlank(fileName)
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".jar"),
            "Managed Flink artifacts must be JAR files.");
        long fileSize = fileSystem.fileSize(sourcePath);
        ApiAlertException.throwIfTrue(
            fileSize <= 0 || fileSize > maxArtifactBytes,
            "Managed Flink artifact size exceeds the environment limit.");
        String checksum = fileSystem.fileSha256(sourcePath);
        return new ResolvedArtifactSource(
            resource.getId(),
            checksum,
            fileName,
            "streampark-" + checksum + ".jar",
            fileSize,
            () -> fileSystem.open(sourcePath));
    }

    private static String canonicalResourcePath(Resource resource) {
        String teamResource =
            String.format(
                "%s/%d/%s",
                Workspace.local().APP_UPLOADS(),
                resource.getTeamId(),
                resource.getResourceName());
        if (FsOperator.lfs().exists(teamResource)) {
            return teamResource;
        }
        ApiAlertException.throwIfTrue(
            StringUtils.isBlank(resource.getFilePath()),
            "Managed Flink artifact resource path is missing.");
        return resource.getFilePath();
    }
}
