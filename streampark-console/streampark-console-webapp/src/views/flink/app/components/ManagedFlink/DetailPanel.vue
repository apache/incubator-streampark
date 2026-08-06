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
<script setup lang="ts" name="ManagedFlinkDetailPanel">
  import {
    Alert,
    Button,
    Descriptions,
    Empty,
    Space,
    Spin,
    Table,
    Tabs,
    Tag,
    Typography,
  } from 'ant-design-vue';
  import dayjs from 'dayjs';
  import { computed, ref, unref, watch } from 'vue';
  import {
    fetchManagedApplication,
    fetchManagedEnvironment,
    fetchManagedOperations,
    fetchManagedSnapshots,
    fetchReconcileManagedOperation,
  } from '/@/api/flink/managedFlink';
  import type {
    ManagedFlinkApplication,
    ManagedFlinkEnvironment,
    ManagedFlinkOperation,
    ManagedFlinkSnapshot,
  } from '/@/api/flink/managedFlink.type';
  import { parseVolcengineEnvironmentConfig } from '/@/api/flink/managedFlink.type';
  import type { AppListRecord } from '/@/api/flink/app.type';
  import { useI18n } from '/@/hooks/web/useI18n';

  const props = defineProps({
    app: {
      type: Object as PropType<Partial<AppListRecord>>,
      required: true,
    },
  });
  const { t } = useI18n();
  const DescriptionItem = Descriptions.Item;
  const TabPane = Tabs.TabPane;
  const TypographyText = Typography.Text;

  const application = ref<ManagedFlinkApplication>();
  const environment = ref<ManagedFlinkEnvironment>();
  const providerConfig = computed(() => parseVolcengineEnvironmentConfig(environment.value));
  const snapshots = ref<ManagedFlinkSnapshot[]>([]);
  const operations = ref<ManagedFlinkOperation[]>([]);
  const loading = ref(false);
  const reconcilingOperationId = ref<string>();

  const identity = computed(() => {
    if (!props.app.id || !props.app.teamId) {
      return undefined;
    }
    return { appId: String(props.app.id), teamId: String(props.app.teamId) };
  });
  const syncState = computed(
    () => props.app.managedSyncState || unref(application)?.syncState || 'PENDING',
  );
  const definitionDrifted = computed(() => {
    const current = unref(application);
    return Boolean(
      current?.providerDefinitionHash &&
        current.deployedDefinitionHash &&
        current.providerDefinitionHash !== current.deployedDefinitionHash,
    );
  });
  const hasCandidateChanges = computed(() => {
    const current = unref(application);
    return Boolean(
      current?.deployedDefinitionHash &&
        current.localDefinitionHash !== current.deployedDefinitionHash,
    );
  });

  const snapshotColumns = computed(() => [
    {
      title: t('flink.app.managed.snapshotId'),
      dataIndex: 'snapshotId',
      width: 240,
    },
    {
      title: t('flink.app.managed.snapshotType'),
      dataIndex: 'snapshotType',
      width: 130,
    },
    {
      title: t('flink.app.status'),
      dataIndex: 'state',
      width: 120,
    },
    {
      title: t('flink.app.managed.latestSnapshot'),
      dataIndex: 'latest',
      width: 90,
    },
    {
      title: t('flink.app.managed.snapshotCompletedAt'),
      dataIndex: 'completionTime',
      width: 180,
    },
    {
      title: t('common.description'),
      dataIndex: 'description',
      ellipsis: true,
    },
  ]);
  const operationColumns = computed(() => [
    {
      title: t('flink.app.managed.operationType'),
      dataIndex: 'type',
      width: 110,
    },
    {
      title: t('flink.app.status'),
      dataIndex: 'state',
      width: 120,
    },
    {
      title: t('flink.app.managed.operationId'),
      dataIndex: 'operationId',
      width: 210,
      ellipsis: true,
    },
    {
      title: t('flink.app.managed.providerRequestId'),
      dataIndex: 'providerRequestId',
      width: 190,
      ellipsis: true,
    },
    {
      title: t('flink.app.managed.acceptedAt'),
      dataIndex: 'createTime',
      width: 180,
    },
    {
      title: t('flink.app.managed.finishedAt'),
      dataIndex: 'finishTime',
      width: 180,
    },
    {
      title: t('flink.app.managed.operationError'),
      dataIndex: 'errorMessage',
      ellipsis: true,
    },
    {
      title: t('flink.app.managed.operationAction'),
      dataIndex: 'action',
      width: 190,
      fixed: 'right',
    },
  ]);

  watch(identity, (value) => value && load(), { immediate: true });

  async function load() {
    const currentIdentity = unref(identity);
    if (!currentIdentity) {
      return;
    }
    loading.value = true;
    try {
      const managedApplication = await fetchManagedApplication(currentIdentity);
      application.value = managedApplication;
      environment.value = await fetchManagedEnvironment({
        teamId: currentIdentity.teamId,
        clusterId: managedApplication.managedEnvironmentId,
      });
      operations.value = await fetchManagedOperations(currentIdentity);
      try {
        snapshots.value = await fetchManagedSnapshots(currentIdentity);
      } catch (error) {
        snapshots.value = [];
        console.error(error);
      }
    } catch (error) {
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  function syncColor(state: string) {
    return (
      {
        HEALTHY: 'success',
        DEGRADED: 'warning',
        DRIFTED: 'error',
        NOT_FOUND: 'error',
        PENDING: 'processing',
      }[state] || 'default'
    );
  }

  function operationColor(state: string) {
    return (
      {
        ACCEPTED: 'blue',
        RUNNING: 'processing',
        SUCCEEDED: 'success',
        FAILED: 'error',
        UNKNOWN: 'warning',
      }[state] || 'default'
    );
  }

  function snapshotColor(state: string) {
    return (
      {
        COMPLETED: 'success',
        CREATING: 'processing',
        FAILED: 'error',
        EXPIRED: 'default',
      }[state] || 'default'
    );
  }

  function formatTime(value?: string) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '—';
  }

  async function handleReconcile(record: ManagedFlinkOperation) {
    const currentIdentity = unref(identity);
    if (!currentIdentity || record.state !== 'UNKNOWN') {
      return;
    }
    reconcilingOperationId.value = String(record.operationId);
    try {
      const reconciled = await fetchReconcileManagedOperation({
        ...currentIdentity,
        operationId: String(record.operationId),
      });
      operations.value = operations.value.map((operation) =>
        operation.operationId === reconciled.operationId ? reconciled : operation,
      );
      await load();
    } finally {
      reconcilingOperationId.value = undefined;
    }
  }
</script>

<template>
  <Spin :spinning="loading">
    <template v-if="application">
      <Alert
        v-if="syncState === 'DEGRADED'"
        type="warning"
        show-icon
        class="mb-16px"
        :message="t('flink.app.managed.statusMayBeStale')"
        :description="t('flink.app.managed.statusMayBeStaleDescription')"
      />
      <Alert
        v-else-if="syncState === 'NOT_FOUND'"
        type="error"
        show-icon
        class="mb-16px"
        :message="t('flink.app.managed.providerApplicationNotFound')"
      />
      <Alert
        v-if="definitionDrifted || syncState === 'DRIFTED'"
        type="error"
        show-icon
        class="mb-16px"
        :message="t('flink.app.managed.providerDefinitionDrifted')"
      />
      <Alert
        v-if="hasCandidateChanges"
        type="info"
        show-icon
        class="mb-16px"
        :message="t('flink.app.managed.localCandidateChanged')"
      />

      <div class="flex justify-between items-center mb-12px">
        <Space>
          <span class="text-lg font-medium">{{ t('flink.app.managed.managedDetail') }}</span>
          <Tag color="purple">{{ application.providerType }}</Tag>
          <Tag :color="syncColor(syncState)">
            {{ t(`flink.app.managed.syncStates.${syncState}`) }}
          </Tag>
        </Space>
        <Button :loading="loading" @click="load">{{ t('common.redo') }}</Button>
      </div>

      <Descriptions bordered size="small" :column="2">
        <DescriptionItem :label="t('flink.app.managed.environment')">
          {{ environment?.clusterName || application.managedEnvironmentId }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.provider')">
          {{ application.providerType }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.project')">
          {{ providerConfig.projectName || providerConfig.projectId || '—' }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.resourcePool')">
          {{ providerConfig.resourcePoolName || providerConfig.resourcePoolId || '—' }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.engineVersion')">
          {{ application.runtimeConfig.engineVersion }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.executionMode')">
          {{ application.runtimeConfig.executionMode }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.estimatedCu')">
          {{ application.estimatedCu }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.liveMetrics')">—</DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.externalApplicationId')">
          <TypographyText v-if="application.externalApplicationId" copyable>
            {{ application.externalApplicationId }}
          </TypographyText>
          <span v-else>—</span>
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.externalInstanceId')">
          <TypographyText v-if="application.externalInstanceId" copyable>
            {{ application.externalInstanceId }}
          </TypographyText>
          <span v-else>—</span>
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.lastSyncTime')">
          {{ formatTime(app.managedLastSyncTime || application.lastSyncTime) }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.nextSyncTime')">
          {{ formatTime(app.managedNextSyncTime || application.nextSyncTime) }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.syncFailures')">
          {{ app.managedConsecutiveSyncFailures ?? application.consecutiveSyncFailures ?? 0 }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.providerRawState')">
          {{ application.providerRawState || '—' }}
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.candidateHash')" :span="2">
          <TypographyText code copyable>{{ application.localDefinitionHash }}</TypographyText>
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.deployedHash')" :span="2">
          <TypographyText v-if="application.deployedDefinitionHash" code copyable>
            {{ application.deployedDefinitionHash }}
          </TypographyText>
          <span v-else>—</span>
        </DescriptionItem>
        <DescriptionItem :label="t('flink.app.managed.providerDefinitionHash')" :span="2">
          <TypographyText v-if="application.providerDefinitionHash" code copyable>
            {{ application.providerDefinitionHash }}
          </TypographyText>
          <span v-else>—</span>
        </DescriptionItem>
      </Descriptions>

      <Tabs class="mt-16px">
        <TabPane key="snapshots" :tab="t('flink.app.managed.snapshots')">
          <Table
            size="small"
            row-key="snapshotId"
            :columns="snapshotColumns"
            :data-source="snapshots"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 1050 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'snapshotId'">
                <TypographyText copyable>{{ record.snapshotId }}</TypographyText>
              </template>
              <template v-else-if="column.dataIndex === 'state'">
                <Tag :color="snapshotColor(record.state)">{{ record.state }}</Tag>
              </template>
              <template v-else-if="column.dataIndex === 'latest'">
                <Tag v-if="record.latest" color="success">
                  {{ t('flink.app.managed.latestSnapshot') }}
                </Tag>
                <span v-else>—</span>
              </template>
              <template v-else-if="column.dataIndex === 'completionTime'">
                {{ formatTime(record.completionTime || record.triggerTime) }}
              </template>
            </template>
          </Table>
        </TabPane>
        <TabPane key="operations" :tab="t('flink.app.managed.operations')">
          <Table
            size="small"
            row-key="operationId"
            :columns="operationColumns"
            :data-source="operations"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 1250 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'state'">
                <Tag :color="operationColor(record.state)">{{ record.state }}</Tag>
              </template>
              <template
                v-else-if="['operationId', 'providerRequestId'].includes(String(column.dataIndex))"
              >
                <TypographyText v-if="record[column.dataIndex]" copyable>
                  {{ record[column.dataIndex] }}
                </TypographyText>
                <span v-else>—</span>
              </template>
              <template v-else-if="column.dataIndex === 'createTime'">
                {{ formatTime(record.createTime) }}
              </template>
              <template v-else-if="column.dataIndex === 'finishTime'">
                {{ formatTime(record.finishTime) }}
              </template>
              <template v-else-if="column.dataIndex === 'errorMessage'">
                {{ record.errorMessage || record.errorCode || '—' }}
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <Button
                  v-if="record.state === 'UNKNOWN'"
                  type="link"
                  size="small"
                  :loading="reconcilingOperationId === String(record.operationId)"
                  @click="handleReconcile(record)"
                >
                  {{ t('flink.app.managed.reconcileOperation') }}
                </Button>
                <span v-else>—</span>
              </template>
            </template>
          </Table>
        </TabPane>
      </Tabs>
    </template>
    <Empty
      v-else-if="!loading"
      :description="t('flink.app.managed.lifecycleApplicationUnavailable')"
    />
  </Spin>
</template>
