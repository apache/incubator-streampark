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

import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.application.FlinkApplicationManageService;
import org.apache.streampark.console.core.service.application.impl.FlinkApplicationActionServiceImpl;
import org.apache.streampark.console.core.service.application.impl.FlinkApplicationBuildPipelineServiceImpl;
import org.apache.streampark.console.core.service.application.impl.FlinkApplicationManageServiceImpl;
import org.apache.streampark.console.core.service.impl.FlinkClusterServiceImpl;
import org.apache.streampark.console.core.service.impl.FlinkSavepointServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagedFlinkLegacyRouteIsolationTest {

    private static final long APP_ID = 100L;

    @Mock
    private FlinkApplicationMapper applicationMapper;

    @Mock
    private FlinkApplicationManageService applicationManageService;

    @Mock
    private FlinkClusterMapper clusterMapper;

    @Mock
    private FlinkEnvService flinkEnvService;

    private FlinkApplication managedApplication;

    @BeforeEach
    void setUp() {
        managedApplication = new FlinkApplication();
        managedApplication.setId(APP_ID);
        managedApplication.setDeployMode(FlinkDeployMode.MANAGED_APPLICATION.getMode());
    }

    @Test
    void shouldRejectManagedLifecycleBeforeLegacyDependencies() {
        FlinkApplicationActionServiceImpl actionService = new FlinkApplicationActionServiceImpl();
        ReflectionTestUtils.setField(actionService, "baseMapper", applicationMapper);
        when(applicationMapper.selectById(APP_ID)).thenReturn(managedApplication);
        when(applicationMapper.selectApp(APP_ID)).thenReturn(managedApplication);

        FlinkApplication appParam = new FlinkApplication();
        appParam.setId(APP_ID);

        assertManagedRouteRejected(() -> actionService.start(appParam, false));
        assertManagedRouteRejected(() -> actionService.cancel(appParam));
        assertManagedRouteRejected(() -> actionService.revoke(APP_ID));
        assertManagedRouteRejected(() -> actionService.abort(APP_ID));
    }

    @Test
    void shouldRejectManagedBuildBeforeFlinkEnvironmentLookup() {
        FlinkApplicationBuildPipelineServiceImpl buildService =
            new FlinkApplicationBuildPipelineServiceImpl();
        ReflectionTestUtils.setField(
            buildService, "applicationManageService", applicationManageService);
        ReflectionTestUtils.setField(buildService, "flinkEnvService", flinkEnvService);
        when(applicationManageService.getById(APP_ID)).thenReturn(managedApplication);

        assertManagedRouteRejected(() -> buildService.buildApplication(APP_ID, false));

        verifyNoInteractions(flinkEnvService);
    }

    @Test
    void shouldRejectManagedSavepointBeforeFlinkEnvironmentLookup() {
        FlinkSavepointServiceImpl savepointService = new FlinkSavepointServiceImpl();
        ReflectionTestUtils.setField(
            savepointService, "applicationManageService", applicationManageService);
        ReflectionTestUtils.setField(savepointService, "flinkEnvService", flinkEnvService);
        when(applicationManageService.getById(APP_ID)).thenReturn(managedApplication);

        FlinkApplication appParam = new FlinkApplication();
        appParam.setId(APP_ID);
        assertManagedRouteRejected(() -> savepointService.getSavePointPath(appParam));
        assertManagedRouteRejected(() -> savepointService.trigger(APP_ID, null, null));

        verifyNoInteractions(flinkEnvService);
    }

    @Test
    void shouldRejectManagedMappingAndRemovalBeforeLegacyMutation() {
        FlinkApplicationManageServiceImpl manageService = new FlinkApplicationManageServiceImpl();
        ReflectionTestUtils.setField(manageService, "baseMapper", applicationMapper);
        when(applicationMapper.selectById(APP_ID)).thenReturn(managedApplication);

        FlinkApplication appParam = new FlinkApplication();
        appParam.setId(APP_ID);
        assertManagedRouteRejected(() -> manageService.mapping(appParam));
        assertManagedRouteRejected(() -> manageService.remove(APP_ID));
    }

    @Test
    void shouldRejectManagedEnvironmentFromEveryLegacyClusterMutation() {
        FlinkCluster managedCluster = new FlinkCluster();
        managedCluster.setId(APP_ID);
        managedCluster.setClusterName("managed-environment");
        managedCluster.setDeployMode(FlinkDeployMode.MANAGED_APPLICATION.getMode());

        FlinkClusterServiceImpl clusterService = new FlinkClusterServiceImpl();
        ReflectionTestUtils.setField(clusterService, "baseMapper", clusterMapper);
        when(clusterMapper.selectById(APP_ID)).thenReturn(managedCluster);

        assertManagedClusterRouteRejected(() -> clusterService.check(managedCluster));
        assertManagedClusterRouteRejected(() -> clusterService.internalCreate(managedCluster));
        assertManagedClusterRouteRejected(() -> clusterService.update(managedCluster));
        assertManagedClusterRouteRejected(() -> clusterService.start(managedCluster));
        assertManagedClusterRouteRejected(() -> clusterService.shutdown(managedCluster));
        assertManagedClusterRouteRejected(
            () -> clusterService.allowShutdownCluster(managedCluster));
        assertManagedClusterRouteRejected(
            () -> clusterService.updateClusterState(APP_ID, ClusterState.STARTING));
        assertManagedClusterRouteRejected(() -> clusterService.remove(APP_ID));
    }

    private static void assertManagedRouteRejected(ThrowingOperation operation) {
        assertThatThrownBy(operation::run)
            .isInstanceOf(ApiAlertException.class)
            .hasMessageContaining("ManagedFlinkProvider")
            .hasMessageContaining("FlinkClient/FsOperator");
    }

    private static void assertManagedClusterRouteRejected(ThrowingOperation operation) {
        assertThatThrownBy(operation::run)
            .isInstanceOf(ApiAlertException.class)
            .hasMessageContaining("ManagedFlinkEnvironmentService")
            .hasMessageContaining("legacy cluster route");
    }

    @FunctionalInterface
    private interface ThrowingOperation {

        void run() throws Exception;
    }
}
