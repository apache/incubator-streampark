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
<script setup lang="ts" name="ManagedFlinkLifecycleModal">
  import {
    Alert,
    Button,
    Descriptions,
    Empty,
    Form,
    Input,
    Progress,
    Radio,
    Select,
    Space,
    Spin,
    Switch,
    Tag,
    Typography,
  } from 'ant-design-vue';
  import dayjs from 'dayjs';
  import { computed, onUnmounted, reactive, ref, unref, watch } from 'vue';
  import {
    fetchCreateManagedSnapshot,
    fetchManagedApplication,
    fetchManagedEnvironment,
    fetchManagedOperation,
    fetchManagedOperations,
    fetchManagedSnapshots,
    fetchReconcileManagedOperation,
    fetchRestartManagedApplication,
    fetchStartManagedApplication,
    fetchStopManagedApplication,
  } from '/@/api/flink/managedFlink';
  import type {
    ManagedFlinkApplication,
    ManagedFlinkCapability,
    ManagedFlinkOperation,
    ManagedFlinkOperationState,
    ManagedFlinkSnapshot,
    ManagedJobRestoreMode,
  } from '/@/api/flink/managedFlink.type';
  import type { AppListRecord } from '/@/api/flink/app.type';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';

  type LifecycleAction = 'START' | 'RESTART' | 'STOP' | 'SNAPSHOT' | 'PROGRESS';

  const emit = defineEmits(['register', 'operationChange']);
  const { t } = useI18n();
  const { createMessage } = useMessage();
  const DescriptionItem = Descriptions.Item;
  const TypographyText = Typography.Text;
  const TextArea = Input.TextArea;

  const action = ref<LifecycleAction>('START');
  const sourceApplication = ref<AppListRecord>();
  const application = ref<ManagedFlinkApplication>();
  const capability = ref<ManagedFlinkCapability>();
  const snapshots = ref<ManagedFlinkSnapshot[]>([]);
  const operation = ref<ManagedFlinkOperation>();
  const loading = ref(false);
  const snapshotsLoading = ref(false);
  const submitting = ref(false);
  const reconciling = ref(false);
  const pollError = ref('');
  const idempotencyKey = ref('');
  const form = reactive({
    restoreMode: 'FRESH' as ManagedJobRestoreMode,
    snapshotId: undefined as string | undefined,
    withSnapshot: false,
    description: '',
  });
  let pollTimer: number | undefined;
  let pollGeneration = 0;

  const [registerModal] = useModalInner(async (data) => {
    stopPolling();
    sourceApplication.value = data.application;
    action.value = data.action || 'START';
    application.value = undefined;
    capability.value = undefined;
    snapshots.value = [];
    operation.value = undefined;
    pollError.value = '';
    form.restoreMode = 'FRESH';
    form.snapshotId = undefined;
    form.withSnapshot = false;
    form.description = '';
    idempotencyKey.value = createIdempotencyKey(action.value);
    await load();
  });

  const operationActive = computed(() =>
    ['ACCEPTED', 'RUNNING'].includes(unref(operation)?.state || ''),
  );
  const isFirstStart = computed(
    () => unref(action) === 'START' && !unref(application)?.externalInstanceId,
  );
  const supportsStopWithSnapshot = computed(
    () => unref(capability)?.supportsStopWithSnapshot === true,
  );
  const supportsCreateSnapshot = computed(() => unref(capability)?.supportsCreateSnapshot === true);
  const restoreModeOptions = computed(() => {
    const supported = new Set(unref(capability)?.startModes || ['FRESH']);
    const options = [
      {
        label: t('flink.app.managed.restoreFresh'),
        value: 'FRESH',
      },
      {
        label: t('flink.app.managed.restoreLatest'),
        value: 'LATEST_STATE',
      },
      {
        label: t('flink.app.managed.restoreSpecified'),
        value: 'SPECIFIED_SNAPSHOT',
      },
    ];
    return options.filter(
      (item) => supported.has(item.value) && (!unref(isFirstStart) || item.value === 'FRESH'),
    );
  });
  const restorableSnapshots = computed(() =>
    unref(snapshots).filter((item) => item.state === 'COMPLETED'),
  );
  const snapshotOptions = computed(() =>
    unref(restorableSnapshots).map((item) => ({
      label: snapshotLabel(item),
      value: item.snapshotId,
    })),
  );
  const canSubmit = computed(() => {
    if (unref(operation) || !unref(application)) {
      return false;
    }
    if (['START', 'RESTART'].includes(unref(action))) {
      return form.restoreMode !== 'SPECIFIED_SNAPSHOT' || Boolean(form.snapshotId);
    }
    if (unref(action) === 'SNAPSHOT') {
      return unref(supportsCreateSnapshot);
    }
    return true;
  });
  const modalTitle = computed(() => t(`flink.app.managed.lifecycleTitles.${unref(action)}`));
  const confirmText = computed(() => t(`flink.app.managed.lifecycleConfirm.${unref(action)}`));
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

  watch(
    () => form.restoreMode,
    async (value) => {
      if (value === 'SPECIFIED_SNAPSHOT' && !unref(snapshots).length) {
        await loadSnapshots();
      }
      if (value !== 'SPECIFIED_SNAPSHOT') {
        form.snapshotId = undefined;
      }
    },
  );

  function requestIdentity() {
    const app = unref(sourceApplication);
    if (!app) {
      throw new Error(t('flink.app.managed.lifecycleApplicationUnavailable'));
    }
    return { teamId: String(app.teamId), appId: String(app.id) };
  }

  async function load() {
    loading.value = true;
    try {
      const identity = requestIdentity();
      const managedApplication = await fetchManagedApplication(identity);
      application.value = managedApplication;
      const environment = await fetchManagedEnvironment({
        teamId: identity.teamId,
        clusterId: managedApplication.managedEnvironmentId,
      });
      capability.value = parseCapability(environment.capabilityJson);

      if (unref(action) === 'PROGRESS') {
        const operations = await fetchManagedOperations(identity);
        operation.value = operations.find((item) => item.type !== 'RELEASE');
        if (unref(operation)) {
          action.value = operationAction(unref(operation)!.type);
        }
      }
      if (unref(isFirstStart)) {
        form.restoreMode = 'FRESH';
      } else {
        form.restoreMode =
          (unref(restoreModeOptions)[0]?.value as ManagedJobRestoreMode | undefined) || 'FRESH';
      }
      if (unref(operationActive)) {
        schedulePoll(0);
      }
    } catch (error) {
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  async function loadSnapshots() {
    snapshotsLoading.value = true;
    try {
      snapshots.value = await fetchManagedSnapshots(requestIdentity());
      if (
        form.snapshotId &&
        !unref(restorableSnapshots).some((item) => item.snapshotId === form.snapshotId)
      ) {
        form.snapshotId = undefined;
      }
    } catch (error) {
      console.error(error);
    } finally {
      snapshotsLoading.value = false;
    }
  }

  async function handleSubmit() {
    if (!unref(canSubmit)) {
      return;
    }
    submitting.value = true;
    try {
      const identity = requestIdentity();
      const common = { ...identity, idempotencyKey: unref(idempotencyKey) };
      switch (unref(action)) {
        case 'START':
          operation.value = await fetchStartManagedApplication({
            ...common,
            restoreMode: form.restoreMode,
            snapshotId: form.snapshotId,
          });
          break;
        case 'RESTART':
          operation.value = await fetchRestartManagedApplication({
            ...common,
            restoreMode: form.restoreMode,
            snapshotId: form.snapshotId,
          });
          break;
        case 'STOP':
          operation.value = await fetchStopManagedApplication({
            ...common,
            withSnapshot: unref(supportsStopWithSnapshot) && form.withSnapshot,
          });
          break;
        case 'SNAPSHOT':
          operation.value = await fetchCreateManagedSnapshot({
            ...common,
            description: form.description.trim() || undefined,
          });
          break;
      }
      createMessage.success(t('flink.app.managed.lifecycleAccepted'));
      emit('operationChange', {
        ...unref(operation),
        lifecycleAction: unref(action),
      });
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
      emit('operationChange', {
        ...unref(operation),
        lifecycleAction: unref(action),
      });
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
      emit('operationChange', {
        ...unref(operation),
        lifecycleAction: unref(action),
      });
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

  function createIdempotencyKey(value: LifecycleAction) {
    const random =
      typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    return `managed-${value.toLowerCase()}-${random}`;
  }

  function parseCapability(value?: string): ManagedFlinkCapability | undefined {
    if (!value) {
      return undefined;
    }
    try {
      return JSON.parse(value);
    } catch (error) {
      console.error(error);
      return undefined;
    }
  }

  function operationAction(type: string): LifecycleAction {
    return ['START', 'RESTART', 'STOP', 'SNAPSHOT'].includes(type)
      ? (type as LifecycleAction)
      : 'PROGRESS';
  }

  function snapshotLabel(snapshot: ManagedFlinkSnapshot) {
    const latest = snapshot.latest ? ` · ${t('flink.app.managed.latestSnapshot')}` : '';
    return `${snapshot.snapshotId}${latest} · ${formatTime(snapshot.completionTime)}`;
  }

  function formatTime(value?: string) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
  }

  function stateLabel(state: ManagedFlinkOperationState) {
    return t(`flink.app.managed.lifecycleOperationStates.${state}`);
  }

  onUnmounted(stopPolling);
</script>

<template>
  <BasicModal
    @register="registerModal"
    @ok="handleSubmit"
    @visible-change="handleVisibleChange"
    :title="modalTitle"
    :width="760"
    :min-height="430"
    :confirm-loading="submitting"
    :show-ok-btn="action !== 'PROGRESS' && !operation"
    :ok-button-props="{ disabled: !canSubmit }"
    :ok-text="confirmText"
    :cancel-text="t('common.closeText')"
  >
    <Spin :spinning="loading">
      <template v-if="application">
        <Descriptions bordered size="small" :column="2">
          <DescriptionItem :label="t('flink.app.appName')">
            {{ application.jobName }}
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.managed.environment')">
            {{ application.managedEnvironmentId }}
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.managed.externalApplicationId')">
            {{ application.externalApplicationId || '-' }}
          </DescriptionItem>
          <DescriptionItem :label="t('flink.app.managed.externalInstanceId')">
            {{ application.externalInstanceId || '-' }}
          </DescriptionItem>
        </Descriptions>

        <Form
          v-if="!operation && ['START', 'RESTART'].includes(action)"
          class="mt-20px"
          layout="vertical"
        >
          <Alert
            v-if="isFirstStart"
            type="info"
            show-icon
            class="mb-16px"
            :message="t('flink.app.managed.firstStartFreshOnly')"
          />
          <Form.Item :label="t('flink.app.managed.restoreMode')" required>
            <Radio.Group v-model:value="form.restoreMode">
              <Radio v-for="item in restoreModeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item
            v-if="form.restoreMode === 'SPECIFIED_SNAPSHOT'"
            :label="t('flink.app.managed.snapshot')"
            required
          >
            <Select
              v-model:value="form.snapshotId"
              show-search
              :loading="snapshotsLoading"
              :options="snapshotOptions"
              :placeholder="t('flink.app.managed.selectSnapshot')"
              :filter-option="
                (input, option) =>
                  String(option?.label || '')
                    .toLowerCase()
                    .includes(input.toLowerCase())
              "
            />
            <div class="mt-8px flex justify-between">
              <span class="form-hint">
                {{ t('flink.app.managed.snapshotExternalIdNotice') }}
              </span>
              <Button type="link" size="small" :loading="snapshotsLoading" @click="loadSnapshots">
                {{ t('common.redo') }}
              </Button>
            </div>
            <Empty
              v-if="!snapshotsLoading && !restorableSnapshots.length"
              :description="t('flink.app.managed.noRestorableSnapshot')"
            />
          </Form.Item>
        </Form>

        <section v-if="!operation && action === 'STOP'" class="operation-form">
          <Alert
            v-if="!supportsStopWithSnapshot"
            type="warning"
            show-icon
            class="mb-16px"
            :message="t('flink.app.managed.stopWithSnapshotUnavailable')"
            :description="t('flink.app.managed.stopWithSnapshotUnavailableDescription')"
          />
          <div v-else class="flex items-center justify-between">
            <div>
              <div class="font-medium">{{ t('flink.app.managed.stopWithSnapshot') }}</div>
              <div class="form-hint">{{ t('flink.app.managed.stopWithSnapshotDescription') }}</div>
            </div>
            <Switch v-model:checked="form.withSnapshot" />
          </div>
        </section>

        <Form v-if="!operation && action === 'SNAPSHOT'" class="mt-20px" layout="vertical">
          <Alert
            v-if="!supportsCreateSnapshot"
            type="warning"
            show-icon
            class="mb-16px"
            :message="t('flink.app.managed.createSnapshotUnavailable')"
          />
          <Form.Item :label="t('common.description')">
            <TextArea
              v-model:value="form.description"
              :maxlength="400"
              show-count
              :rows="4"
              :placeholder="t('flink.app.managed.snapshotDescriptionPlaceholder')"
            />
          </Form.Item>
        </Form>

        <section v-if="operation" class="operation-panel">
          <div class="flex items-center justify-between mb-12px">
            <Space>
              <span class="font-medium">{{ t('flink.app.managed.lifecycleOperation') }}</span>
              <Tag>{{ operation.type }}</Tag>
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
            :message="operation.errorCode || t('flink.app.managed.lifecycleFailed')"
            :description="operation.errorMessage"
          />
          <Alert
            v-if="operation.state === 'UNKNOWN'"
            type="warning"
            show-icon
            class="mb-12px"
            :message="t('flink.app.managed.operationUnknown')"
            :description="t('flink.app.managed.lifecycleUnknownDescription')"
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
          v-else-if="action === 'PROGRESS'"
          class="mt-20px"
          :description="t('flink.app.managed.noLifecycleOperation')"
        />
      </template>
      <Empty
        v-else-if="!loading"
        :description="t('flink.app.managed.lifecycleApplicationUnavailable')"
      />
    </Spin>
  </BasicModal>
</template>

<style scoped lang="less">
  .operation-form,
  .operation-panel {
    margin-top: 20px;
    padding: 16px;
    border: 1px solid @border-color-base;
    border-radius: 4px;
    background: @background-color-light;
  }

  .operation-hint,
  .form-hint {
    color: @text-color-secondary;
  }

  .operation-hint {
    margin: 4px 0 12px;
  }
</style>
