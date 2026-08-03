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

package org.apache.streampark.console.core.util;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.bean.Dependency;
import org.apache.streampark.console.core.entity.ApplicationBuildPipeline;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.Message;
import org.apache.streampark.console.core.entity.ReleaseOutcomeTarget;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.NoticeTypeEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.enums.ResourceTypeEnum;
import org.apache.streampark.console.core.service.MessageService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.application.ApplicationLogService;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.DependencyInfo;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.PipelineSnapshot;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public final class ApplicationBuildPipelineUtils {

    private ApplicationBuildPipelineUtils() {
    }

    public static ApplicationBuildPipeline finishedSnapshot(
                                                            PipelineSnapshot snapshot,
                                                            BuildResult result,
                                                            Long appId) {
        return ApplicationBuildPipeline.fromPipeSnapshot(snapshot).setAppId(appId).setBuildResult(result);
    }

    public static void applySuccessfulRelease(ReleaseOutcomeTarget app, Runnable promoteEffectiveVersion) {
        if (app.isRunning()) {
            app.setRelease(ReleaseStateEnum.NEED_RESTART.get());
        } else {
            app.setOptionState(OptionStateEnum.NONE.getValue());
            app.setRelease(ReleaseStateEnum.DONE.get());
            promoteEffectiveVersion.run();
        }
        app.setBuild(false);
    }

    public static void finalizeRelease(
                                       Runnable updateRelease,
                                       ApplicationLog applicationLog,
                                       ApplicationLogService applicationLogService,
                                       Runnable refreshWatcherIfWatching) {
        updateRelease.run();
        applicationLogService.save(applicationLog);
        refreshWatcherIfWatching.run();
    }

    public static void prepareBuildResources(
                                             boolean jarJob,
                                             Runnable jarJobPreparer,
                                             Dependency dependencyObject,
                                             ResourceService resourceService) {
        if (jarJob) {
            jarJobPreparer.run();
        } else {
            uploadSqlJobDependencies(dependencyObject, resourceService);
        }
    }

    public static ApplicationLog createReleaseLog(Long appId, Integer jobTypeCode) {
        ApplicationLog applicationLog = new ApplicationLog();
        if (jobTypeCode != null) {
            applicationLog.setJobType(jobTypeCode);
        }
        applicationLog.setOptionName(org.apache.streampark.console.core.enums.OperationEnum.RELEASE.getValue());
        applicationLog.setAppId(appId);
        applicationLog.setCreateTime(new Date());
        applicationLog.setUserId(ServiceHelper.getUserId());
        return applicationLog;
    }

    public static ApplicationBuildPipeline fromSnapshot(PipelineSnapshot snapshot, Long appId) {
        return ApplicationBuildPipeline.fromPipeSnapshot(snapshot).setAppId(appId);
    }

    public static void checkOrElseUploadJar(
                                            FsOperator fsOperator, File localJar, String targetJar, String targetDir) {
        if (!fsOperator.exists(targetJar)) {
            fsOperator.upload(localJar.getAbsolutePath(), targetDir, false, true);
        } else if (!FileUtils.equals(localJar, new File(targetJar))) {
            fsOperator.upload(localJar.getAbsolutePath(), targetDir, false, true);
        }
    }

    public static void uploadSqlJobDependencies(Dependency dependencyObject, ResourceService resourceService) {
        if (dependencyObject.getJar().isEmpty()) {
            return;
        }
        String localUploads = Workspace.local().APP_UPLOADS();
        for (String jar : dependencyObject.getJar()) {
            File localJar = new File(WebUtils.getAppTempDir(), jar);
            File uploadJar = new File(localUploads, jar);
            if (!localJar.exists() && !uploadJar.exists()) {
                throw new ApiAlertException("Missing file: " + jar + ", please upload again");
            }
            if (localJar.exists()) {
                checkOrElseUploadJar(FsOperator.lfs(), localJar, uploadJar.getAbsolutePath(), localUploads);
            }
        }
    }

    public static void prepareJarJobHome(
                                         Long teamId,
                                         String jar,
                                         String appHome,
                                         String appLib,
                                         String distHome,
                                         FsOperator fsOperator,
                                         String appUploads,
                                         boolean fromUpload,
                                         ApplicationType applicationType,
                                         ResourceService resourceService,
                                         boolean tempDirFallback) {
        fsOperator.delete(appHome);
        if (!fromUpload) {
            fsOperator.upload(distHome, appHome);
            return;
        }

        String uploadJar = appUploads.concat("/").concat(jar);
        File localJar = new File(String.format("%s/%d/%s", Workspace.local().APP_UPLOADS(), teamId, jar));
        if (!localJar.exists()) {
            Resource resource = resourceService.findByResourceName(teamId, jar);
            if (resource != null && StringUtils.isNotBlank(resource.getFilePath())) {
                localJar = new File(resource.getFilePath());
                uploadJar = appUploads.concat("/").concat(localJar.getName());
            } else if (tempDirFallback) {
                localJar = new File(WebUtils.getAppTempDir(), jar);
                uploadJar = appUploads.concat("/").concat(localJar.getName());
            }
        }
        checkOrElseUploadJar(fsOperator, localJar, uploadJar, appUploads);

        switch (applicationType) {
            case STREAMPARK_FLINK:
            case STREAMPARK_SPARK:
                fsOperator.mkdirs(appLib);
                fsOperator.copy(uploadJar, appLib, false, true);
                break;
            case APACHE_FLINK:
            case APACHE_SPARK:
                fsOperator.mkdirs(appHome);
                fsOperator.copy(uploadJar, appHome, false, true);
                break;
            default:
                throw new IllegalArgumentException(
                    "[StreamPark] unsupported ApplicationType: " + applicationType);
        }
    }

    public static DependencyInfo getMergedDependencyInfo(
                                                         DependencyInfo dependencyInfo,
                                                         String teamResource,
                                                         Long teamId,
                                                         ResourceService resourceService,
                                                         Logger log) {
        return getMergedDependencyInfo(dependencyInfo, teamResource, teamId, resourceService, log, false);
    }

    public static DependencyInfo getMergedDependencyInfo(
                                                         DependencyInfo dependencyInfo,
                                                         String teamResource,
                                                         Long teamId,
                                                         ResourceService resourceService,
                                                         Logger log,
                                                         boolean errorOnFailure) {
        if (StringUtils.isBlank(teamResource)) {
            return dependencyInfo;
        }

        try {
            String[] resourceIds = JacksonUtils.read(teamResource, String[].class);

            List<Artifact> mvnArtifacts = new ArrayList<>();
            List<String> jarLibs = new ArrayList<>();

            Arrays.stream(resourceIds)
                .forEach(
                    resourceId -> {
                        Resource resource = resourceService.getById(resourceId);

                        if (resource.getResourceType() != ResourceTypeEnum.GROUP) {
                            mergeDependency(teamId, mvnArtifacts, jarLibs, resource);
                        } else {
                            try {
                                String[] groupElements =
                                    JacksonUtils.read(resource.getResource(), String[].class);
                                Arrays.stream(groupElements)
                                    .forEach(
                                        resourceIdInGroup -> mergeDependency(
                                            teamId,
                                            mvnArtifacts,
                                            jarLibs,
                                            resourceService.getById(resourceIdInGroup)));
                            } catch (JsonProcessingException e) {
                                throw new ApiAlertException("Parse resource group failed.", e);
                            }
                        }
                    });
            return dependencyInfo.merge(mvnArtifacts, jarLibs);
        } catch (Exception e) {
            if (errorOnFailure) {
                log.error("Merge team dependency failed.", e);
            } else {
                log.warn("Merge team dependency failed.", e);
            }
            return dependencyInfo;
        }
    }

    public static void recordReleaseFailure(
                                            Long appId,
                                            String jobDisplayName,
                                            PipelineSnapshot snapshot,
                                            ApplicationLog applicationLog,
                                            MessageService messageService) {
        Message message = new Message(
            ServiceHelper.getUserId(),
            appId,
            jobDisplayName.concat(" release failed"),
            ExceptionUtils.stringifyException(snapshot.error().exception()),
            NoticeTypeEnum.EXCEPTION);
        messageService.push(message);
        applicationLog.setException(ExceptionUtils.stringifyException(snapshot.error().exception()));
        applicationLog.setSuccess(false);
    }

    private static void mergeDependency(
                                        Long teamId,
                                        List<Artifact> mvnArtifacts,
                                        List<String> jarLibs,
                                        Resource resource) {
        Dependency dependency = Dependency.toDependency(resource.getResource());
        dependency
            .getPom()
            .forEach(
                pom -> mvnArtifacts.add(
                    new Artifact(
                        pom.getGroupId(),
                        pom.getArtifactId(),
                        pom.getVersion(),
                        pom.getClassifier())));
        dependency
            .getJar()
            .forEach(
                jar -> jarLibs.add(
                    String.format("%s/%d/%s", Workspace.local().APP_UPLOADS(), teamId, jar)));
    }
}
