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

import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.entity.ManagedFlinkStateEvent;
import org.apache.streampark.console.core.enums.CandidateTypeEnum;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ManagedJobRestoreMode;
import org.apache.streampark.console.core.managed.api.ManagedJobState;
import org.apache.streampark.console.core.managed.api.ManagedSnapshot;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotState;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.managed.model.CloudAccountCreateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountGrantRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationSaveRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentCreateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkLifecycleRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkSnapshotCreateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkStopRequest;
import org.apache.streampark.console.core.managed.support.FakeManagedFlinkProvider;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkStateEventMapper;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.alert.AlertService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Transactional
@TestPropertySource(properties = {
        "server.port=0",
        "streampark.managed-flink.enabled=true",
        "streampark.managed-flink.providers.volcengine.enabled=true",
        "streampark.managed-flink.credentials.active-key-version=1",
        "streampark.managed-flink.credentials.master-keys="
            + "1=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class ManagedFlinkReleaseServiceTest extends SpringUnitTestBase {

    private static final long USER_ID = 100000L;
    private static final long TEAM_ID = 100000L;

    @Autowired
    private CloudAccountService cloudAccountService;

    @Autowired
    private CloudAccountGrantService cloudAccountGrantService;

    @Autowired
    private ManagedFlinkEnvironmentService environmentService;

    @Autowired
    private ManagedFlinkApplicationService applicationService;

    @Autowired
    private ManagedFlinkReleaseService releaseService;

    @Autowired
    private ManagedFlinkReleaseExecutor releaseExecutor;

    @Autowired
    private ManagedFlinkReleaseReconcileService releaseReconcileService;

    @Autowired
    private ManagedFlinkOperationReconcileService operationReconcileService;

    @Autowired
    private ManagedFlinkDeployedDefinitionService deployedDefinitionService;

    @Autowired
    private ManagedFlinkLifecycleService lifecycleService;

    @Autowired
    private ManagedFlinkLifecycleExecutor lifecycleExecutor;

    @Autowired
    private ManagedFlinkJobSyncService jobSyncService;

    @Autowired
    private ManagedFlinkSnapshotService snapshotService;

    @Autowired
    private ManagedFlinkSnapshotExecutor snapshotExecutor;

    @Autowired
    private ManagedFlinkOperationService operationService;

    @Autowired
    private ManagedFlinkApplicationMapper managedApplicationMapper;

    @Autowired
    private FlinkApplicationMapper applicationMapper;

    @Autowired
    private ManagedFlinkStateEventMapper stateEventMapper;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @MockBean
    private ManagedFlinkAuditContext auditContext;

    @MockBean
    private ManagedFlinkProviderRegistry providerRegistry;

    @MockBean
    private ManagedFlinkReleaseDispatcher dispatcher;

    @MockBean
    private ManagedFlinkLifecycleDispatcher lifecycleDispatcher;

    @MockBean
    private ManagedFlinkSnapshotDispatcher snapshotDispatcher;

    @MockBean
    private AlertService alertService;

    private final FakeManagedFlinkProvider fakeProvider =
        new FakeManagedFlinkProvider(Clock.systemUTC(), Duration.ofHours(1));

    @BeforeEach
    void setUp() {
        when(auditContext.currentUserId()).thenReturn(USER_ID);
        when(providerRegistry.getRequired(ManagedFlinkProviderType.VOLCENGINE))
            .thenReturn(fakeProvider);
        when(dispatcher.dispatch(anyLong())).thenReturn(true);
        when(lifecycleDispatcher.dispatch(anyLong())).thenReturn(true);
        when(snapshotDispatcher.dispatch(anyLong())).thenReturn(true);
        when(alertService.alert(anyLong(), any())).thenReturn(true);
        fakeProvider.clearWriteFailures();
        fakeProvider.resetJobActions();
        fakeProvider.resetSnapshots();
    }

    @Test
    void shouldReleaseSqlSnapshotAndReplayCompletedOperation() {
        Long appId = createApplication("release-sql");
        ManagedFlinkReleaseRequest request = new ManagedFlinkReleaseRequest();
        request.setTeamId(TEAM_ID);
        request.setAppId(appId);
        request.setIdempotencyKey("managed-release-" + UUID.randomUUID());

        ManagedFlinkOperationView accepted = releaseService.release(request);
        assertThat(accepted.getState()).isEqualTo("ACCEPTED");
        assertThat(accepted.isIdempotentReplay()).isFalse();

        releaseExecutor.execute(accepted.getOperationId());

        ManagedFlinkOperation operation =
            operationService.getRequired(appId, accepted.getOperationId());
        ManagedFlinkApplication managed =
            managedApplicationMapper.selectById(appId);
        FlinkApplication application = applicationMapper.selectById(appId);
        assertThat(operation.getState()).isEqualTo("SUCCEEDED");
        assertThat(operation.getRequestJson())
            .contains("\"jobName\":\"release-sql\"")
            .doesNotContain("AKLT-", "secret-");
        assertThat(managed.getExternalDraftId()).startsWith("fake-draft-");
        assertThat(managed.getExternalApplicationId()).startsWith("fake-app-");
        assertThat(managed.getDeployedDefinitionHash())
            .isEqualTo(managed.getLocalDefinitionHash());
        assertThat(application.getRelease()).isEqualTo(ReleaseStateEnum.DONE.get());
        assertThat(flinkSqlService.getEffective(appId, false)).isNotNull();

        ManagedFlinkOperationView replay = releaseService.release(request);
        assertThat(replay.getOperationId()).isEqualTo(accepted.getOperationId());
        assertThat(replay.isIdempotentReplay()).isTrue();
        assertThat(replay.getState()).isEqualTo("SUCCEEDED");
        verify(dispatcher).dispatch(accepted.getOperationId());
    }

    @Test
    void shouldPersistKnownProviderRejectionAsFailed() {
        fakeProvider.failDraftWith(ProviderErrorCategory.VALIDATION);

        ManagedFlinkOperation operation =
            releaseAndExecute(createApplication("release-rejected"));

        assertThat(operation.getState()).isEqualTo("FAILED");
        assertThat(operation.getErrorCode()).isEqualTo("VALIDATION:FakeDraftFailure");
        assertThat(operation.getProviderRequestId()).isEqualTo("fake-draft-request-id");
        FlinkApplication application = applicationMapper.selectById(operation.getAppId());
        assertThat(application.getRelease()).isEqualTo(ReleaseStateEnum.FAILED.get());
    }

    @Test
    void shouldReconcileRetryableProviderWriteWithoutReplay() {
        fakeProvider.failDeploymentWith(ProviderErrorCategory.TRANSIENT, true);

        ManagedFlinkOperation operation =
            releaseAndExecute(createApplication("release-unknown"));

        assertThat(operation.getState()).isEqualTo("UNKNOWN");
        assertThat(operation.getErrorCode()).isEqualTo("TRANSIENT:FakeDeploymentFailure");
        assertThat(operation.getProviderRequestId()).isEqualTo("fake-deploy-request-id");
        ManagedFlinkApplication managed =
            managedApplicationMapper.selectById(operation.getAppId());
        assertThat(managed.getExternalDraftId()).startsWith("fake-draft-");
        FlinkApplication application = applicationMapper.selectById(operation.getAppId());
        assertThat(application.getRelease()).isEqualTo(ReleaseStateEnum.RELEASING.get());

        fakeProvider.clearWriteFailures();
        ManagedFlinkOperationView reconciled =
            releaseReconcileService.reconcile(
                TEAM_ID, operation.getAppId(), operation.getId());

        assertThat(reconciled.getState()).isEqualTo("SUCCEEDED");
        ManagedFlinkApplication reconciledManaged =
            managedApplicationMapper.selectById(operation.getAppId());
        assertThat(reconciledManaged.getExternalApplicationId()).startsWith("fake-app-");
        FlinkApplication reconciledApplication =
            applicationMapper.selectById(operation.getAppId());
        assertThat(reconciledApplication.getRelease()).isEqualTo(ReleaseStateEnum.DONE.get());
        assertThat(flinkSqlService.getEffective(operation.getAppId(), false)).isNotNull();
    }

    @Test
    void shouldActivateReleasedSqlButKeepConcurrentEditPending() {
        Long appId = createApplication("release-concurrent-edit");
        ManagedFlinkReleaseRequest release = releaseRequest(appId);
        ManagedFlinkOperationView accepted = releaseService.release(release);

        updateSql(appId, "SELECT 2");
        releaseExecutor.execute(accepted.getOperationId());

        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        FlinkApplication application = applicationMapper.selectById(appId);
        assertThat(operationService.getRequired(appId, accepted.getOperationId()).getState())
            .isEqualTo("SUCCEEDED");
        assertThat(managed.getDeployedDefinitionHash())
            .isNotEqualTo(managed.getLocalDefinitionHash());
        assertThat(application.getRelease()).isEqualTo(ReleaseStateEnum.NEED_RELEASE.get());
        assertThat(flinkSqlService.getEffective(appId, true).getSql()).isEqualTo("SELECT 1");
        assertThat(flinkSqlService.getLatestFlinkSql(appId, true).getSql()).isEqualTo("SELECT 2");
        assertThat(flinkSqlService.getCandidate(appId, CandidateTypeEnum.NEW)).isNotNull();
        assertThat(deployedDefinitionService.getRequired(TEAM_ID, appId).getSqlText())
            .isEqualTo("SELECT 1");
    }

    @Test
    void shouldKeepPreviousEffectiveSqlWhenNextReleaseFails() {
        Long appId = createApplication("release-preserve-effective");
        ManagedFlinkOperation first = releaseAndExecute(appId);
        assertThat(first.getState()).isEqualTo("SUCCEEDED");

        updateSql(appId, "SELECT 2");
        fakeProvider.failDraftWith(ProviderErrorCategory.VALIDATION);
        ManagedFlinkOperation failed = releaseAndExecute(appId);

        assertThat(failed.getState()).isEqualTo("FAILED");
        assertThat(flinkSqlService.getEffective(appId, true).getSql()).isEqualTo("SELECT 1");
        assertThat(flinkSqlService.getLatestFlinkSql(appId, true).getSql()).isEqualTo("SELECT 2");
        assertThat(flinkSqlService.getCandidate(appId, CandidateTypeEnum.NEW)).isNotNull();
        assertThat(applicationMapper.selectById(appId).getRelease())
            .isEqualTo(ReleaseStateEnum.FAILED.get());
    }

    @Test
    void shouldStartReleasedJobAndReplayTheSameIntent() {
        Long appId = createApplication("lifecycle-start");
        releaseAndExecute(appId);
        ManagedFlinkLifecycleRequest request =
            lifecycleRequest(appId, "start-intent-1", ManagedJobRestoreMode.FRESH);

        ManagedFlinkOperationView accepted = lifecycleService.start(request);
        lifecycleExecutor.execute(accepted.getOperationId());

        ManagedFlinkOperation operation =
            operationService.getRequired(appId, accepted.getOperationId());
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        FlinkApplication application = applicationMapper.selectById(appId);
        assertThat(operation.getState()).isEqualTo("SUCCEEDED");
        assertThat(operation.getRequestJson())
            .contains("\"operationType\":\"START\"")
            .doesNotContain("AKLT-", "secret-");
        assertThat(managed.getExternalInstanceId()).isEqualTo("fake-instance-1");
        assertThat(application.getState()).isEqualTo(FlinkAppStateEnum.STARTING.getValue());
        assertThat(application.getOptionState()).isEqualTo(OptionStateEnum.STARTING.getValue());
        assertThat(application.getTracking()).isEqualTo(1);

        ManagedFlinkOperationView replay = lifecycleService.start(request);
        assertThat(replay.getOperationId()).isEqualTo(accepted.getOperationId());
        assertThat(replay.isIdempotentReplay()).isTrue();
        assertThat(fakeProvider.getJobActionCount()).isEqualTo(1);
        assertThat(operationService.list(appId))
            .extracting(ManagedFlinkOperationView::getType)
            .containsExactly("START", "RELEASE");
        assertThat(operationService.list(appId))
            .allSatisfy(
                item -> assertThat(item.getCreateUserId()).isEqualTo(USER_ID));
    }

    @Test
    void shouldStopRunningInstanceAndRestartRunningJob() {
        Long appId = createApplication("lifecycle-stop-restart");
        releaseAndExecute(appId);
        ManagedFlinkLifecycleRequest start =
            lifecycleRequest(appId, "start-intent-2", ManagedJobRestoreMode.FRESH);
        ManagedFlinkOperationView startOperation = lifecycleService.start(start);
        lifecycleExecutor.execute(startOperation.getOperationId());
        setApplicationState(appId, FlinkAppStateEnum.RUNNING);

        ManagedFlinkStopRequest stop = new ManagedFlinkStopRequest();
        stop.setTeamId(TEAM_ID);
        stop.setAppId(appId);
        stop.setIdempotencyKey("stop-intent-1");
        ManagedFlinkOperationView stopOperation = lifecycleService.stop(stop);
        lifecycleExecutor.execute(stopOperation.getOperationId());

        assertThat(operationService.getRequired(appId, stopOperation.getOperationId()).getState())
            .isEqualTo("SUCCEEDED");
        assertThat(applicationMapper.selectById(appId).getState())
            .isEqualTo(FlinkAppStateEnum.CANCELLING.getValue());
        assertThat(fakeProvider.getJobAction("STOP", managedJobId(appId)).getInstanceId())
            .isEqualTo("fake-instance-1");

        setApplicationState(appId, FlinkAppStateEnum.RUNNING);
        ManagedFlinkLifecycleRequest restart =
            lifecycleRequest(
                appId, "restart-intent-1", ManagedJobRestoreMode.LATEST_STATE);
        ManagedFlinkOperationView restartOperation = lifecycleService.restart(restart);
        lifecycleExecutor.execute(restartOperation.getOperationId());

        assertThat(
            operationService.getRequired(appId, restartOperation.getOperationId()).getState())
                .isEqualTo("SUCCEEDED");
        assertThat(applicationMapper.selectById(appId).getState())
            .isEqualTo(FlinkAppStateEnum.RESTARTING.getValue());
        assertThat(fakeProvider.getJobAction("RESTART", managedJobId(appId))).isNotNull();
    }

    @Test
    void shouldRejectUnsafeLifecycleModesBeforeOperationAdmission() {
        Long appId = createApplication("lifecycle-validation");
        releaseAndExecute(appId);

        assertThatThrownBy(
            () -> lifecycleService.start(
                lifecycleRequest(
                    appId, "unsafe-first-start", ManagedJobRestoreMode.LATEST_STATE)))
                        .hasMessageContaining("first managed Flink start");

        ManagedFlinkLifecycleRequest snapshotRestore =
            lifecycleRequest(
                appId, "snapshot-before-b13", ManagedJobRestoreMode.SPECIFIED_SNAPSHOT);
        snapshotRestore.setSnapshotId("savepoint-1");
        assertThatThrownBy(() -> lifecycleService.start(snapshotRestore))
            .hasMessageContaining("not available for restore");

        setApplicationState(appId, FlinkAppStateEnum.RUNNING);
        ManagedFlinkStopRequest stop = new ManagedFlinkStopRequest();
        stop.setTeamId(TEAM_ID);
        stop.setAppId(appId);
        stop.setIdempotencyKey("snapshot-stop-before-b13");
        stop.setWithSnapshot(true);
        assertThatThrownBy(() -> lifecycleService.stop(stop))
            .hasMessageContaining("snapshot lifecycle slice");
    }

    @Test
    void shouldRestoreOnlyCompletedSnapshotOwnedByCurrentJob() {
        Long appId = createApplication("lifecycle-snapshot-restore");
        releaseAndExecute(appId);
        ManagedFlinkOperationView start =
            lifecycleService.start(
                lifecycleRequest(
                    appId, "snapshot-restore-start", ManagedJobRestoreMode.FRESH));
        lifecycleExecutor.execute(start.getOperationId());
        setApplicationState(appId, FlinkAppStateEnum.RUNNING);
        fakeProvider.putSnapshot(
            managedJobId(appId),
            ManagedSnapshot.builder()
                .snapshotId("restore-savepoint-1")
                .instanceId("fake-instance-1")
                .snapshotType("MANUAL")
                .state(ManagedSnapshotState.COMPLETED)
                .providerState("COMPLETED")
                .location("tos://managed/restore-savepoint-1")
                .build());

        ManagedFlinkLifecycleRequest restart =
            lifecycleRequest(
                appId,
                "snapshot-restore-restart",
                ManagedJobRestoreMode.SPECIFIED_SNAPSHOT);
        restart.setSnapshotId("restore-savepoint-1");
        ManagedFlinkOperationView accepted = lifecycleService.restart(restart);
        lifecycleExecutor.execute(accepted.getOperationId());

        ManagedFlinkOperation operation =
            operationService.getRequired(appId, accepted.getOperationId());
        assertThat(operation.getState()).isEqualTo("SUCCEEDED");
        assertThat(operation.getRequestJson())
            .contains("\"snapshotId\":\"restore-savepoint-1\"")
            .contains("\"snapshotSourceInstanceId\":\"fake-instance-1\"");

        Long otherAppId = createApplication("lifecycle-cross-job-snapshot");
        releaseAndExecute(otherAppId);
        ManagedFlinkOperationView otherStart =
            lifecycleService.start(
                lifecycleRequest(
                    otherAppId, "cross-job-start", ManagedJobRestoreMode.FRESH));
        lifecycleExecutor.execute(otherStart.getOperationId());
        setApplicationState(otherAppId, FlinkAppStateEnum.RUNNING);
        ManagedFlinkLifecycleRequest crossJobRestore =
            lifecycleRequest(
                otherAppId,
                "cross-job-restore",
                ManagedJobRestoreMode.SPECIFIED_SNAPSHOT);
        crossJobRestore.setSnapshotId("restore-savepoint-1");

        assertThatThrownBy(() -> lifecycleService.restart(crossJobRestore))
            .hasMessageContaining("not available for restore");
    }

    @Test
    void shouldRestoreCompletedSnapshotWithoutSourceInstanceId() {
        Long appId = createApplication("lifecycle-snapshot-without-instance");
        releaseAndExecute(appId);
        ManagedFlinkOperationView start =
            lifecycleService.start(
                lifecycleRequest(
                    appId, "snapshot-without-instance-start", ManagedJobRestoreMode.FRESH));
        lifecycleExecutor.execute(start.getOperationId());
        setApplicationState(appId, FlinkAppStateEnum.CANCELED);
        fakeProvider.putSnapshot(
            managedJobId(appId),
            ManagedSnapshot.builder()
                .snapshotId("restore-savepoint-without-instance")
                .snapshotType("MANUAL")
                .state(ManagedSnapshotState.COMPLETED)
                .providerState("AVAILABLE")
                .location("tos://managed/restore-savepoint-without-instance")
                .build());

        ManagedFlinkLifecycleRequest restore =
            lifecycleRequest(
                appId,
                "snapshot-without-instance-restore",
                ManagedJobRestoreMode.SPECIFIED_SNAPSHOT);
        restore.setSnapshotId("restore-savepoint-without-instance");
        ManagedFlinkOperationView accepted = lifecycleService.start(restore);
        lifecycleExecutor.execute(accepted.getOperationId());

        ManagedFlinkOperation operation =
            operationService.getRequired(appId, accepted.getOperationId());
        assertThat(operation.getState()).isEqualTo("SUCCEEDED");
        assertThat(operation.getRequestJson())
            .contains("\"snapshotId\":\"restore-savepoint-without-instance\"");
    }

    @Test
    void shouldKeepRetryableLifecycleWriteUnknownWithoutBlindReplay() {
        Long appId = createApplication("lifecycle-unknown");
        releaseAndExecute(appId);
        fakeProvider.failJobActionWith(ProviderErrorCategory.TRANSIENT, true);
        ManagedFlinkLifecycleRequest request =
            lifecycleRequest(appId, "unknown-start-intent", ManagedJobRestoreMode.FRESH);

        ManagedFlinkOperationView accepted = lifecycleService.start(request);
        lifecycleExecutor.execute(accepted.getOperationId());

        ManagedFlinkOperation operation =
            operationService.getRequired(appId, accepted.getOperationId());
        assertThat(operation.getState()).isEqualTo("UNKNOWN");
        assertThat(operation.getErrorCode())
            .isEqualTo("TRANSIENT:FakeJobActionFailure");
        assertThat(managedApplicationMapper.selectById(appId).getSyncState())
            .isEqualTo("PENDING");
        assertThat(fakeProvider.getJobActionCount()).isEqualTo(1);

        ManagedFlinkOperationView replay = lifecycleService.start(request);
        assertThat(replay.getOperationId()).isEqualTo(accepted.getOperationId());
        assertThat(replay.getState()).isEqualTo("UNKNOWN");
        assertThat(replay.isIdempotentReplay()).isTrue();
        assertThat(fakeProvider.getJobActionCount()).isEqualTo(1);

        fakeProvider.clearWriteFailures();
        ManagedFlinkOperationView reconciled =
            operationReconcileService.reconcile(
                TEAM_ID, appId, accepted.getOperationId());
        assertThat(reconciled.getState()).isEqualTo("SUCCEEDED");
        assertThat(managedApplicationMapper.selectById(appId).getExternalInstanceId())
            .isEqualTo("fake-instance-1");
        assertThat(applicationMapper.selectById(appId).getState())
            .isEqualTo(FlinkAppStateEnum.STARTING.getValue());
        assertThat(fakeProvider.getJobActionCount()).isEqualTo(1);
    }

    @Test
    void shouldSynchronizeRunningAndStoppedStatesWithoutRegressingCancellation() {
        Long appId = createApplication("watcher-lifecycle");
        releaseAndExecute(appId);
        ManagedFlinkOperationView start =
            lifecycleService.start(
                lifecycleRequest(
                    appId, "watcher-start", ManagedJobRestoreMode.FRESH));
        lifecycleExecutor.execute(start.getOperationId());
        String jobId = managedJobId(appId);

        fakeProvider.setJobStatus(
            jobId, "fake-instance-1", ManagedJobState.RUNNING);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        assertThat(applicationMapper.selectById(appId).getState())
            .isEqualTo(FlinkAppStateEnum.RUNNING.getValue());
        assertThat(applicationMapper.selectById(appId).getOptionState())
            .isEqualTo(OptionStateEnum.NONE.getValue());
        assertThat(managedApplicationMapper.selectById(appId).getSyncState())
            .isEqualTo("HEALTHY");
        ManagedFlinkApplicationView runningView =
            applicationService.get(TEAM_ID, appId);
        assertThat(runningView.getState()).isEqualTo(FlinkAppStateEnum.RUNNING.getValue());
        assertThat(runningView.getExternalInstanceId()).isEqualTo("fake-instance-1");
        assertThat(runningView.getProviderRawState()).isEqualTo("RUNNING");
        assertThat(runningView.getLastSyncTime()).isNotNull();

        ManagedFlinkStopRequest stop = new ManagedFlinkStopRequest();
        stop.setTeamId(TEAM_ID);
        stop.setAppId(appId);
        stop.setIdempotencyKey("watcher-stop");
        ManagedFlinkOperationView stopping = lifecycleService.stop(stop);
        lifecycleExecutor.execute(stopping.getOperationId());

        fakeProvider.setJobStatus(
            jobId, "fake-instance-1", ManagedJobState.RUNNING);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        assertThat(applicationMapper.selectById(appId).getState())
            .isEqualTo(FlinkAppStateEnum.CANCELLING.getValue());
        assertThat(applicationMapper.selectById(appId).getOptionState())
            .isEqualTo(OptionStateEnum.CANCELLING.getValue());

        fakeProvider.setJobStatus(
            jobId, "fake-instance-1", ManagedJobState.STOPPED);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        FlinkApplication stopped = applicationMapper.selectById(appId);
        ManagedFlinkApplication managed =
            managedApplicationMapper.selectById(appId);
        assertThat(stopped.getState()).isEqualTo(FlinkAppStateEnum.CANCELED.getValue());
        assertThat(stopped.getOptionState()).isEqualTo(OptionStateEnum.NONE.getValue());
        assertThat(stopped.getTracking()).isZero();
        assertThat(managed.getNextSyncTime()).isNull();
        assertThat(managed.getSyncOwner()).isNull();
        assertThat(managed.getSyncLeaseUntil()).isNull();
    }

    @Test
    void shouldDegradeThenLoseAndRecoverAfterProviderLookupFailures() {
        Long appId = createApplication("watcher-failure");
        releaseAndExecute(appId);
        ManagedFlinkOperationView start =
            lifecycleService.start(
                lifecycleRequest(
                    appId, "watcher-failure-start", ManagedJobRestoreMode.FRESH));
        lifecycleExecutor.execute(start.getOperationId());
        fakeProvider.failJobLookupWith(ProviderErrorCategory.TRANSIENT);

        for (int failures = 1; failures <= 2; failures++) {
            forceSyncDue(appId);
            assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
            ManagedFlinkApplication managed =
                managedApplicationMapper.selectById(appId);
            assertThat(managed.getSyncState()).isEqualTo("DEGRADED");
            assertThat(managed.getConsecutiveSyncFailures()).isEqualTo(failures);
            assertThat(managed.getSyncOwner()).isNull();
            assertThat(applicationMapper.selectById(appId).getState())
                .isEqualTo(FlinkAppStateEnum.STARTING.getValue());
        }

        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        assertThat(applicationMapper.selectById(appId).getState())
            .isEqualTo(FlinkAppStateEnum.LOST.getValue());

        fakeProvider.clearJobLookupFailure();
        fakeProvider.setJobStatus(
            managedJobId(appId), "fake-instance-1", ManagedJobState.RUNNING);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        ManagedFlinkApplication recovered =
            managedApplicationMapper.selectById(appId);
        assertThat(applicationMapper.selectById(appId).getState())
            .isEqualTo(FlinkAppStateEnum.RUNNING.getValue());
        assertThat(recovered.getSyncState()).isEqualTo("HEALTHY");
        assertThat(recovered.getConsecutiveSyncFailures()).isZero();
    }

    @Test
    void shouldAuditAndAlertEachProblemTransitionOnce() {
        Long appId = createApplication("watcher-alert-deduplication");
        releaseAndExecute(appId);
        ManagedFlinkOperationView start =
            lifecycleService.start(
                lifecycleRequest(
                    appId, "watcher-alert-start", ManagedJobRestoreMode.FRESH));
        lifecycleExecutor.execute(start.getOperationId());
        setApplicationState(appId, FlinkAppStateEnum.RUNNING);
        FlinkApplication application = applicationMapper.selectById(appId);
        application.setAlertId(1L);
        applicationMapper.updateById(application);
        String jobId = managedJobId(appId);

        fakeProvider.setJobStatus(
            jobId, "fake-instance-1", ManagedJobState.FAILED);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);

        fakeProvider.setJobStatus(
            jobId, "fake-instance-1", ManagedJobState.RUNNING);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);

        verify(alertService, times(2)).alert(anyLong(), any());
        assertThat(
            stateEventMapper.selectList(
                new LambdaQueryWrapper<ManagedFlinkStateEvent>()
                    .eq(ManagedFlinkStateEvent::getAppId, appId)
                    .orderByAsc(ManagedFlinkStateEvent::getId)))
                        .extracting(
                            ManagedFlinkStateEvent::getFromState,
                            ManagedFlinkStateEvent::getToState,
                            ManagedFlinkStateEvent::getExternalInstanceId,
                            ManagedFlinkStateEvent::getAlertState)
                        .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(
                                "RUNNING",
                                "FAILED",
                                "fake-instance-1",
                                "SENT"),
                            org.assertj.core.groups.Tuple.tuple(
                                "FAILED",
                                "RUNNING",
                                "fake-instance-1",
                                "SENT"));
    }

    @Test
    void shouldRetryFailedManagedAlertWithoutDuplicatingTheEvent() {
        when(alertService.alert(anyLong(), any())).thenReturn(false, true);
        Long appId = createApplication("watcher-alert-retry");
        releaseAndExecute(appId);
        ManagedFlinkOperationView start =
            lifecycleService.start(
                lifecycleRequest(
                    appId, "watcher-alert-retry-start", ManagedJobRestoreMode.FRESH));
        lifecycleExecutor.execute(start.getOperationId());
        setApplicationState(appId, FlinkAppStateEnum.RUNNING);
        FlinkApplication application = applicationMapper.selectById(appId);
        application.setAlertId(1L);
        applicationMapper.updateById(application);
        fakeProvider.setJobStatus(
            managedJobId(appId), "fake-instance-1", ManagedJobState.FAILED);

        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);

        verify(alertService, times(2)).alert(anyLong(), any());
        assertThat(
            stateEventMapper.selectList(
                new LambdaQueryWrapper<ManagedFlinkStateEvent>()
                    .eq(ManagedFlinkStateEvent::getAppId, appId)))
                        .singleElement()
                        .satisfies(
                            event -> {
                                assertThat(event.getAlertState()).isEqualTo("SENT");
                                assertThat(event.getAlertAttempts()).isEqualTo(2);
                            });
    }

    @Test
    void shouldWaitForNewRuntimeInstanceBeforeCompletingRestart() {
        Long appId = createApplication("watcher-restart");
        releaseAndExecute(appId);
        ManagedFlinkOperationView start =
            lifecycleService.start(
                lifecycleRequest(
                    appId, "watcher-restart-start", ManagedJobRestoreMode.FRESH));
        lifecycleExecutor.execute(start.getOperationId());
        String jobId = managedJobId(appId);
        fakeProvider.setJobStatus(
            jobId, "fake-instance-1", ManagedJobState.RUNNING);
        forceSyncDue(appId);
        jobSyncService.synchronizeDue();

        ManagedFlinkOperationView restart =
            lifecycleService.restart(
                lifecycleRequest(
                    appId,
                    "watcher-restart-operation",
                    ManagedJobRestoreMode.LATEST_STATE));
        lifecycleExecutor.execute(restart.getOperationId());
        fakeProvider.setJobStatus(
            jobId, "fake-instance-1", ManagedJobState.RUNNING);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        assertThat(applicationMapper.selectById(appId).getState())
            .isEqualTo(FlinkAppStateEnum.RESTARTING.getValue());
        assertThat(managedApplicationMapper.selectById(appId).getExternalInstanceId())
            .isEqualTo("fake-instance-1");

        fakeProvider.setJobStatus(
            jobId, "fake-instance-2", ManagedJobState.RUNNING);
        forceSyncDue(appId);
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        assertThat(applicationMapper.selectById(appId).getState())
            .isEqualTo(FlinkAppStateEnum.RUNNING.getValue());
        assertThat(managedApplicationMapper.selectById(appId).getExternalInstanceId())
            .isEqualTo("fake-instance-2");
    }

    @Test
    void shouldRespectActiveDatabaseLeaseAndMarkMissingProviderJob() {
        Long appId = createApplication("watcher-lease");
        releaseAndExecute(appId);
        managedApplicationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkApplication>()
                .eq(ManagedFlinkApplication::getAppId, appId)
                .set(ManagedFlinkApplication::getNextSyncTime, new Date(0))
                .set(ManagedFlinkApplication::getSyncOwner, "another-node")
                .set(
                    ManagedFlinkApplication::getSyncLeaseUntil,
                    new Date(System.currentTimeMillis() + 60000)));

        assertThat(jobSyncService.synchronizeDue()).isZero();
        assertThat(fakeProvider.getJobLookupCount()).isZero();

        managedApplicationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkApplication>()
                .eq(ManagedFlinkApplication::getAppId, appId)
                .set(ManagedFlinkApplication::getSyncLeaseUntil, new Date(0)));
        assertThat(jobSyncService.synchronizeDue()).isEqualTo(1);
        assertThat(fakeProvider.getJobLookupCount()).isEqualTo(1);
        ManagedFlinkApplication missing =
            managedApplicationMapper.selectById(appId);
        assertThat(missing.getSyncState()).isEqualTo("NOT_FOUND");
        assertThat(missing.getProviderRawState()).isEqualTo("NOT_FOUND");
        assertThat(missing.getSyncOwner()).isNull();
        assertThat(missing.getSyncLeaseUntil()).isNull();
    }

    @Test
    void shouldRefreshSnapshotsAndExpireMissingProviderEntries() {
        Long appId = createApplication("snapshot-refresh");
        releaseAndExecute(appId);
        String jobId = managedJobId(appId);
        fakeProvider.putSnapshot(
            jobId,
            ManagedSnapshot.builder()
                .snapshotId("savepoint-1")
                .instanceId("fake-instance-1")
                .snapshotType("MANUAL")
                .state(ManagedSnapshotState.COMPLETED)
                .providerState("COMPLETED")
                .location("tos://managed/savepoint-1")
                .triggerTime("2026-07-30T10:00:00Z")
                .completionTime("2026-07-30T10:00:05Z")
                .build());

        assertThat(snapshotService.refreshAndList(TEAM_ID, appId))
            .singleElement()
            .satisfies(
                snapshot -> {
                    assertThat(snapshot.getSnapshotId()).isEqualTo("savepoint-1");
                    assertThat(snapshot.getState()).isEqualTo("COMPLETED");
                    assertThat(snapshot.isLatest()).isTrue();
                    assertThat(snapshot.getLocation())
                        .isEqualTo("tos://managed/savepoint-1");
                });

        fakeProvider.removeSnapshot(jobId, "savepoint-1");
        assertThat(snapshotService.refreshAndList(TEAM_ID, appId))
            .singleElement()
            .satisfies(
                snapshot -> {
                    assertThat(snapshot.getState()).isEqualTo("EXPIRED");
                    assertThat(snapshot.isLatest()).isFalse();
                });
    }

    @Test
    void shouldCreateSnapshotOnceAndCompleteThroughReadOnlyReconciliation() {
        Long appId = createApplication("snapshot-create");
        releaseAndExecute(appId);
        ManagedFlinkOperationView start =
            lifecycleService.start(
                lifecycleRequest(
                    appId, "snapshot-create-start", ManagedJobRestoreMode.FRESH));
        lifecycleExecutor.execute(start.getOperationId());
        setApplicationState(appId, FlinkAppStateEnum.RUNNING);

        ManagedFlinkSnapshotCreateRequest request =
            new ManagedFlinkSnapshotCreateRequest();
        request.setTeamId(TEAM_ID);
        request.setAppId(appId);
        request.setIdempotencyKey("snapshot-create-once");
        request.setDescription("manual snapshot");
        ManagedFlinkOperationView accepted = snapshotService.create(request);
        assertThat(accepted.getState()).isEqualTo("ACCEPTED");

        snapshotExecutor.execute(accepted.getOperationId());
        assertThat(
            operationService.getRequired(appId, accepted.getOperationId()).getState())
                .isEqualTo("UNKNOWN");
        assertThat(fakeProvider.getSnapshotCreateCount()).isEqualTo(1);

        ManagedFlinkOperationView replay = snapshotService.create(request);
        assertThat(replay.getOperationId()).isEqualTo(accepted.getOperationId());
        assertThat(replay.isIdempotentReplay()).isTrue();
        assertThat(fakeProvider.getSnapshotCreateCount()).isEqualTo(1);

        ManagedFlinkOperationView reconciled =
            operationReconcileService.reconcile(
                TEAM_ID, appId, accepted.getOperationId());
        assertThat(reconciled.getState()).isEqualTo("SUCCEEDED");
        assertThat(fakeProvider.getSnapshotCreateCount()).isEqualTo(1);
        assertThat(snapshotService.refreshAndList(TEAM_ID, appId))
            .singleElement()
            .satisfies(
                snapshot -> {
                    assertThat(snapshot.getSnapshotId()).isEqualTo("fake-savepoint-1");
                    assertThat(snapshot.getState()).isEqualTo("COMPLETED");
                    assertThat(snapshot.isLatest()).isTrue();
                    assertThat(snapshot.getDescription())
                        .isEqualTo("manual snapshot");
                });

        request.setDescription("different intent");
        assertThatThrownBy(() -> snapshotService.create(request))
            .hasMessageContaining("idempotency key");
        assertThat(fakeProvider.getSnapshotCreateCount()).isEqualTo(1);
    }

    private ManagedFlinkOperation releaseAndExecute(Long appId) {
        ManagedFlinkOperationView accepted = releaseService.release(releaseRequest(appId));
        releaseExecutor.execute(accepted.getOperationId());
        return operationService.getRequired(appId, accepted.getOperationId());
    }

    private ManagedFlinkReleaseRequest releaseRequest(Long appId) {
        ManagedFlinkReleaseRequest request = new ManagedFlinkReleaseRequest();
        request.setTeamId(TEAM_ID);
        request.setAppId(appId);
        request.setIdempotencyKey("managed-release-" + UUID.randomUUID());
        return request;
    }

    private ManagedFlinkLifecycleRequest lifecycleRequest(
                                                          Long appId,
                                                          String idempotencyKey,
                                                          ManagedJobRestoreMode restoreMode) {
        ManagedFlinkLifecycleRequest request = new ManagedFlinkLifecycleRequest();
        request.setTeamId(TEAM_ID);
        request.setAppId(appId);
        request.setIdempotencyKey(idempotencyKey);
        request.setRestoreMode(restoreMode);
        return request;
    }

    private void setApplicationState(Long appId, FlinkAppStateEnum state) {
        FlinkApplication application = applicationMapper.selectById(appId);
        application.setState(state.getValue());
        application.setOptionState(OptionStateEnum.NONE.getValue());
        applicationMapper.updateById(application);
    }

    private void forceSyncDue(Long appId) {
        managedApplicationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkApplication>()
                .eq(ManagedFlinkApplication::getAppId, appId)
                .set(ManagedFlinkApplication::getNextSyncTime, new Date(0))
                .set(ManagedFlinkApplication::getSyncOwner, null)
                .set(ManagedFlinkApplication::getSyncLeaseUntil, null));
    }

    private String managedJobId(Long appId) {
        return managedApplicationMapper.selectById(appId).getExternalApplicationId();
    }

    private void updateSql(Long appId, String sql) {
        FlinkApplication application = applicationMapper.selectById(appId);
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        ManagedFlinkApplicationSaveRequest request =
            ManagedFlinkApplicationValidatorTest.request();
        request.setAppId(appId);
        request.setVersion(managed.getVersion());
        request.setManagedEnvironmentId(managed.getManagedEnvId());
        request.setJobName(application.getJobName());
        request.setSql(sql);
        applicationService.update(request);
    }

    private Long createApplication(String name) {
        Long environmentId = createEnvironment(name + "-env");
        ManagedFlinkApplicationSaveRequest request =
            ManagedFlinkApplicationValidatorTest.request();
        request.setManagedEnvironmentId(environmentId);
        request.setJobName(name);
        request.setSql("SELECT 1");
        return applicationService.create(request);
    }

    private Long createEnvironment(String name) {
        CloudAccountCreateRequest account = new CloudAccountCreateRequest();
        account.setAccountName(name + "-account");
        account.setProviderType("VOLCENGINE");
        account.setRegion("cn-beijing");
        account.setAccessKey("AKLT-" + name);
        account.setSecretKey("secret-" + name);
        Long accountId = cloudAccountService.create(account);

        CloudAccountGrantRequest grant = new CloudAccountGrantRequest();
        grant.setAccountId(accountId);
        grant.setAccountVersion(0);
        grant.setTeamIds(Collections.singletonList(TEAM_ID));
        cloudAccountGrantService.replaceGrants(grant);

        ManagedFlinkEnvironmentCreateRequest environment =
            new ManagedFlinkEnvironmentCreateRequest();
        environment.setTeamId(TEAM_ID);
        environment.setClusterName(name);
        environment.setCloudAccountId(accountId);
        environment.setProjectId("fake-project");
        environment.setResourcePoolId("fake-pool");
        environment.setDraftDirectoryId(1L);
        Long environmentId = environmentService.create(environment);
        environmentService.probe(TEAM_ID, environmentId);
        return environmentId;
    }
}
