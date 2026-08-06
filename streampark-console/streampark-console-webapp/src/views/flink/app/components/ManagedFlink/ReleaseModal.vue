<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<script setup lang="ts" name="ManagedFlinkReleaseModal">
  import {
    Alert,
    Button,
    Descriptions,
    Empty,
    Progress,
    Space,
    Spin,
    Tag,
    Typography,
  } from 'ant-design-vue';
  import dayjs from 'dayjs';
  import { computed, onUnmounted, ref, unref } from 'vue';
  import {
    fetchManagedApplication,
    fetchManagedOperation,
    fetchManagedOperations,
    fetchReconcileManagedOperation,
    fetchReleaseManagedApplication,
  } from '/@/api/flink/managedFlink';
  import type {
    ManagedFlinkApplication,
    ManagedFlinkOperation,
    ManagedFlinkOperationState,
  } from '/@/api/flink/managedFlink.type';
  import type { AppListRecord } from '/@/api/flink/app.type';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';

  type ModalMode = 'release' | 'progress';

  const emit = defineEmits(['register', 'operationChange']);
  const { t } = useI18n();
  const { createMessage } = useMessage();
  const DescriptionItem = Descriptions.Item;
  const TypographyText = Typography.Text;

  const mode = ref<ModalMode>('release');
  const sourceApplication = ref<AppListRecord>();
  const application = ref<ManagedFlinkApplication>();
  const operation = ref<ManagedFlinkOperation>();
  const loading = ref(false);
  const submitting = ref(false);
  const reconciling = ref(false);
  const pollError = ref('');
  const idempotencyKey = ref('');
  let pollTimer: number | undefined;
  let pollGeneration = 0;

  const [registerModal] = useModalInner(async (data) => {
    stopPolling();
    sourceApplication.value = data.application;
    mode.value = data.mode || 'release';
    application.value = undefined;
    operation.value = undefined;
    pollError.value = '';
    idempotencyKey.value = createIdempotencyKey();
    await load();
  });

  const canRelease = computed(() => Boolean(unref(application)) && !unref(operation));
  const operationActive = computed(() =>
    ['ACCEPTED', 'RUNNING'].includes(unref(operation)?.state || ''),
  );
  const progressPercent = computed(() => {
    switch (unref(operation)?.state) {
      case 'ACCEPTED':
        return 15;
      case 'RUNNING':
        return 60;
      case 'UNKNOWN':
        return 80;
      case 'SUCCEEDED':
      case 'FAILED':
        return 100;
      default:
        return 0;
    }
  });
  const progressStatus = computed(() => {
    switch (unref(operation)?.state) {
      case 'RUNNING':
        return 'active';
      case 'SUCCEEDED':
        return 'success';
      case 'FAILED':
        return 'exception';
      default:
        return 'normal';
    }
  });
  const operationColor = computed(() => {
    const colors: Record<ManagedFlinkOperationState, string> = {
      ACCEPTED: 'blue',
      RUNNING: 'processing',
      SUCCEEDED: 'success',
      FAILED: 'error',
      UNKNOWN: 'warning',
    };
    return unref(operation) ? colors[unref(operation)!.state] : 'default';
  });
  const hasDefinitionChanges = computed(
    () =>
      !unref(application)?.deployedDefinitionHash ||
      unref(application)?.localDefinitionHash !== unref(application)?.deployedDefinitionHash,
  );

  function requestIdentity() {
    const app = unref(sourceApplication);
    if (!app) {
      throw new Error(t('flink.app.managed.releaseApplicationUnavailable'));
    }
    return { teamId: String(app.teamId), appId: String(app.id) };
  }

  async function load() {
    loading.value = true;
    try {
      const identity = requestIdentity();
      const requests: [Promise<ManagedFlinkApplication>, Promise<ManagedFlinkOperation[]>?] = [
        fetchManagedApplication(identity),
      ];
      if (unref(mode) === 'progress') {
        requests.push(fetchManagedOperations(identity));
      }
      const [managedApplication, operations] = await Promise.all(requests);
      application.value = managedApplication;
      operation.value = operations?.find((item) => item.type === 'RELEASE');
      if (unref(operationActive)) {
        schedulePoll(0);
      }
    } catch (error) {
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleRelease() {
    if (!unref(canRelease)) {
      return;
    }
    submitting.value = true;
    try {
      operation.value = await fetchReleaseManagedApplication({
        ...requestIdentity(),
        idempotencyKey: unref(idempotencyKey),
      });
      mode.value = 'progress';
      createMessage.success(t('flink.app.managed.releaseAccepted'));
      emit('operationChange', unref(operation));
      if (unref(operationActive)) {
        schedulePoll(0);
      }
    } catch (error) {
      console.error(error);
    } finally {
      submitting.value = false;
    }
  }

  async function refreshOperation() {
    const current = unref(operation);
    if (!current) {
      return;
    }
    pollError.value = '';
    try {
      operation.value = await fetchManagedOperation({
        ...requestIdentity(),
        operationId: String(current.operationId),
      });
      emit('operationChange', unref(operation));
    } catch (error) {
      pollError.value = t('flink.app.managed.operationRefreshFailed');
      console.error(error);
    }
  }

  async function handleReconcile() {
    const current = unref(operation);
    if (!current || current.state !== 'UNKNOWN') {
      return;
    }
    reconciling.value = true;
    pollError.value = '';
    try {
      operation.value = await fetchReconcileManagedOperation({
        ...requestIdentity(),
        operationId: String(current.operationId),
      });
      emit('operationChange', unref(operation));
      if (unref(operationActive)) {
        schedulePoll(500);
      }
    } catch (error) {
      console.error(error);
    } finally {
      reconciling.value = false;
    }
  }

  function schedulePoll(delay = 1500) {
    const generation = ++pollGeneration;
    if (pollTimer !== undefined) {
      window.clearTimeout(pollTimer);
    }
    pollTimer = window.setTimeout(async () => {
      if (generation !== pollGeneration) {
        return;
      }
      await refreshOperation();
      if (generation === pollGeneration && unref(operationActive)) {
        schedulePoll();
      }
    }, delay);
  }

  function stopPolling() {
    pollGeneration += 1;
    if (pollTimer !== undefined) {
      window.clearTimeout(pollTimer);
      pollTimer = undefined;
    }
  }

  function handleVisibleChange(visible: boolean) {
    if (!visible) {
      stopPolling();
    }
  }

  function createIdempotencyKey() {
    const random =
      typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    return `managed-release-${random}`;
  }

  function formatTime(value?: string) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
  }

  function stateLabel(state: ManagedFlinkOperationState) {
    return t(`flink.app.managed.operationStates.${state}`);
  }

  onUnmounted(stopPolling);
</script>

<template>
  <BasicModal
    @register="registerModal"
    @ok="handleRelease"
    @visible-change="handleVisibleChange"
    :title="t('flink.app.managed.releaseTitle')"
    :width="760"
    :min-height="420"
    :confirm-loading="submitting"
    :show-ok-btn="mode === 'release' && !operation"
    :ok-button-props="{ disabled: !canRelease }"
    :ok-text="t('flink.app.managed.confirmRelease')"
    :cancel-text="t('common.closeText')"
  >
    <Spin :spinning="loading">
      <template v-if="application">
        <Alert
          v-if="mode === 'release'"
          type="info"
          show-icon
          class="mb-16px"
          :message="t('flink.app.managed.releaseSnapshotNotice')"
        />

        <Descriptions bordered size="small" :column="2">
          <DescriptionItem :label="t('flink.app.appName')">
            {{ application.jobName }}
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.jobType')">
            {{ application.jobType }}
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.managed.environment')">
            {{ application.managedEnvironmentId }}
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.managed.engineVersion')">
            {{ application.runtimeConfig.engineVersion }}
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.managed.estimatedCu')">
            {{ application.estimatedCu }}
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.managed.schedulingStrategy')">
            {{ application.releaseConfig.schedulingStrategy }}
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.managed.candidateHash')" :span="2">
            <TypographyText code copyable>
              {{ application.localDefinitionHash }}
            </TypographyText>
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.managed.deployedHash')" :span="2">
            <TypographyText v-if="application.deployedDefinitionHash" code copyable>
              {{ application.deployedDefinitionHash }}
            </TypographyText>
            <span v-else>-</span>
          </DescriptionItem>
        </Descriptions>

        <Alert
          v-if="mode === 'release' && !hasDefinitionChanges"
          type="info"
          show-icon
          class="mt-16px"
          :message="t('flink.app.managed.noDefinitionChanges')"
        />

        <section v-if="operation" class="operation-panel">
          <div class="flex items-center justify-between mb-12px">
            <Space>
              <span class="font-medium">{{ t('flink.app.managed.operationProgress') }}</span>
              <Tag :color="operationColor">{{ stateLabel(operation.state) }}</Tag>
              <Tag v-if="operation.idempotentReplay" color="blue">
                {{ t('flink.app.managed.idempotentReplay') }}
              </Tag>
            </Space>
            <Button size="small" :loading="operationActive" @click="refreshOperation">
              {{ t('common.redo') }}
            </Button>
          </div>

          <Progress
            :percent="progressPercent"
            :status="progressStatus"
            :stroke-color="operation.state === 'UNKNOWN' ? '#faad14' : undefined"
          />
          <p class="operation-hint">{{ stateLabel(operation.state) }}</p>

          <Alert v-if="pollError" type="warning" show-icon class="mb-12px" :message="pollError" />
          <Alert
            v-if="operation.state === 'FAILED'"
            type="error"
            show-icon
            class="mb-12px"
            :message="operation.errorCode || t('flink.app.managed.releaseFailed')"
            :description="operation.errorMessage"
          />
          <Alert
            v-if="operation.state === 'UNKNOWN'"
            type="warning"
            show-icon
            class="mb-12px"
            :message="t('flink.app.managed.operationUnknown')"
            :description="t('flink.app.managed.operationUnknownDescription')"
          />

          <Descriptions size="small" :column="2">
            <DescriptionItem :label="t('flink.app.managed.operationId')">
              <TypographyText copyable>{{ operation.operationId }}</TypographyText>
            </DescriptionItem>
            <DescriptionItem :label="t('flink.app.managed.providerRequestId')">
              <TypographyText v-if="operation.providerRequestId" copyable>
                {{ operation.providerRequestId }}
              </TypographyText>
              <span v-else>-</span>
            </DescriptionItem>
            <DescriptionItem :label="t('flink.app.managed.acceptedAt')">
              {{ formatTime(operation.createTime) }}
            </DescriptionItem>
            <DescriptionItem :label="t('flink.app.managed.finishedAt')">
              {{ formatTime(operation.finishTime) }}
            </DescriptionItem>
          </Descriptions>

          <Button
            v-if="operation.state === 'UNKNOWN'"
            type="primary"
            class="mt-12px"
            :loading="reconciling"
            @click="handleReconcile"
          >
            {{ t('flink.app.managed.reconcileOperation') }}
          </Button>
        </section>
        <Empty
          v-else-if="mode === 'progress'"
          class="mt-20px"
          :description="t('flink.app.managed.noReleaseOperation')"
        />
      </template>
      <Empty
        v-else-if="!loading"
        :description="t('flink.app.managed.releaseApplicationUnavailable')"
      />
    </Spin>
  </BasicModal>
</template>

<style scoped lang="less">
  .operation-panel {
    margin-top: 20px;
    padding: 16px;
    border: 1px solid @border-color-base;
    border-radius: 4px;
    background: @background-color-light;
  }

  .operation-hint {
    margin: 4px 0 12px;
    color: @text-color-secondary;
  }
</style>
