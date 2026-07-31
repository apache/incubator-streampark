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

package org.apache.streampark.console.core.managed.controller;

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationIdRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationSaveRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentCreateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentIdRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentListRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentUpdateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentVersionedIdRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkLifecycleRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkMetadataRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationIdRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkResourcePoolRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkSnapshotCreateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkStopRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkTeamRequest;
import org.apache.streampark.console.core.managed.service.ManagedFlinkApplicationService;
import org.apache.streampark.console.core.managed.service.ManagedFlinkEnvironmentService;
import org.apache.streampark.console.core.managed.service.ManagedFlinkLifecycleService;
import org.apache.streampark.console.core.managed.service.ManagedFlinkMetadataService;
import org.apache.streampark.console.core.managed.service.ManagedFlinkOperationReconcileService;
import org.apache.streampark.console.core.managed.service.ManagedFlinkOperationService;
import org.apache.streampark.console.core.managed.service.ManagedFlinkReleaseService;
import org.apache.streampark.console.core.managed.service.ManagedFlinkSnapshotService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** Team-authorized managed Flink metadata and environment endpoints. */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("flink/managed")
public class ManagedFlinkMetadataController {

    private final ManagedFlinkMetadataService metadataService;
    private final ManagedFlinkEnvironmentService environmentService;
    private final ManagedFlinkApplicationService applicationService;
    private final ManagedFlinkReleaseService releaseService;
    private final ManagedFlinkLifecycleService lifecycleService;
    private final ManagedFlinkOperationReconcileService operationReconcileService;
    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkSnapshotService snapshotService;

    @PostMapping("capability")
    @Permission(team = "#request.teamId")
    public RestResponse capability(@Valid ManagedFlinkMetadataRequest request) {
        return RestResponse.success(metadataService.capability(request));
    }

    @PostMapping("projects")
    @Permission(team = "#request.teamId")
    public RestResponse projects(@Valid ManagedFlinkMetadataRequest request) {
        return RestResponse.success(metadataService.projects(request));
    }

    @PostMapping("resource-pools")
    @Permission(team = "#request.teamId")
    public RestResponse resourcePools(@Valid ManagedFlinkResourcePoolRequest request) {
        return RestResponse.success(metadataService.resourcePools(request));
    }

    @PostMapping("environment/list")
    @Permission(team = "#request.teamId")
    public RestResponse listEnvironments(
                                         @Valid ManagedFlinkEnvironmentListRequest request) {
        return RestResponse.success(environmentService.list(request));
    }

    @PostMapping("environment/get")
    @Permission(team = "#request.teamId")
    public RestResponse getEnvironment(@Valid ManagedFlinkEnvironmentIdRequest request) {
        return RestResponse.success(
            environmentService.get(request.getTeamId(), request.getClusterId()));
    }

    @PostMapping("environment/create")
    @RequiresPermissions("cluster:create")
    @Permission(team = "#request.teamId")
    public RestResponse createEnvironment(
                                          @Valid ManagedFlinkEnvironmentCreateRequest request) {
        return RestResponse.success(environmentService.create(request));
    }

    @PostMapping("environment/update")
    @RequiresPermissions("cluster:update")
    @Permission(team = "#request.teamId")
    public RestResponse updateEnvironment(
                                          @Valid ManagedFlinkEnvironmentUpdateRequest request) {
        environmentService.update(request);
        return RestResponse.success();
    }

    @PostMapping("environment/delete")
    @RequiresPermissions("cluster:delete")
    @Permission(team = "#request.teamId")
    public RestResponse deleteEnvironment(
                                          @Valid ManagedFlinkEnvironmentVersionedIdRequest request) {
        environmentService.delete(request);
        return RestResponse.success();
    }

    @PostMapping("environment/probe")
    @RequiresPermissions("cluster:update")
    @Permission(team = "#request.teamId")
    public RestResponse probeEnvironment(@Valid ManagedFlinkEnvironmentIdRequest request) {
        return RestResponse.success(
            environmentService.probe(request.getTeamId(), request.getClusterId()));
    }

    @PostMapping("application/get")
    @RequiresPermissions("app:detail")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse getApplication(@Valid ManagedFlinkApplicationIdRequest request) {
        return RestResponse.success(
            applicationService.get(request.getTeamId(), request.getAppId()));
    }

    @PostMapping("application/statistics")
    @RequiresPermissions("app:view")
    @Permission(team = "#request.teamId")
    public RestResponse applicationStatistics(@Valid ManagedFlinkTeamRequest request) {
        return RestResponse.success(applicationService.statistics(request.getTeamId()));
    }

    @PostMapping("application/create")
    @RequiresPermissions("app:create")
    @Permission(team = "#request.teamId")
    public RestResponse createApplication(
                                          @RequestBody @Valid ManagedFlinkApplicationSaveRequest request) {
        return RestResponse.success(applicationService.create(request));
    }

    @PostMapping("application/update")
    @RequiresPermissions("app:update")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse updateApplication(
                                          @RequestBody @Valid ManagedFlinkApplicationSaveRequest request) {
        applicationService.update(request);
        return RestResponse.success();
    }

    @PostMapping("application/release")
    @RequiresPermissions("app:release")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse releaseApplication(
                                           @Valid ManagedFlinkReleaseRequest request) {
        return RestResponse.success(releaseService.release(request));
    }

    @PostMapping("application/start")
    @RequiresPermissions("app:start")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse startApplication(
                                         @Valid ManagedFlinkLifecycleRequest request) {
        return RestResponse.success(lifecycleService.start(request));
    }

    @PostMapping("application/stop")
    @RequiresPermissions("app:cancel")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse stopApplication(@Valid ManagedFlinkStopRequest request) {
        return RestResponse.success(lifecycleService.stop(request));
    }

    @PostMapping("application/restart")
    @RequiresPermissions("app:start")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse restartApplication(
                                           @Valid ManagedFlinkLifecycleRequest request) {
        return RestResponse.success(lifecycleService.restart(request));
    }

    @PostMapping("snapshot/list")
    @RequiresPermissions("app:detail")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse listSnapshots(@Valid ManagedFlinkApplicationIdRequest request) {
        return RestResponse.success(
            snapshotService.refreshAndList(request.getTeamId(), request.getAppId()));
    }

    @PostMapping("snapshot/create")
    @RequiresPermissions("savepoint:trigger")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse createSnapshot(
                                       @Valid ManagedFlinkSnapshotCreateRequest request) {
        return RestResponse.success(snapshotService.create(request));
    }

    @PostMapping("operation/get")
    @RequiresPermissions("app:detail")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse getOperation(@Valid ManagedFlinkOperationIdRequest request) {
        return RestResponse.success(
            operationService.getView(
                request.getAppId(), request.getOperationId()));
    }

    @PostMapping("operation/list")
    @RequiresPermissions("app:detail")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse listOperations(@Valid ManagedFlinkApplicationIdRequest request) {
        applicationService.get(request.getTeamId(), request.getAppId());
        return RestResponse.success(operationService.list(request.getAppId()));
    }

    @PostMapping("operation/reconcile")
    @RequiresPermissions("app:release")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse reconcileOperation(
                                           @Valid ManagedFlinkOperationIdRequest request) {
        return RestResponse.success(
            operationReconcileService.reconcile(
                request.getTeamId(), request.getAppId(), request.getOperationId()));
    }
}
