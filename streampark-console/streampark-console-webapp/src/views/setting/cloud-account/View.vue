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
<script setup lang="ts">
  import { ref } from 'vue';
  import { Tag, Tooltip } from 'ant-design-vue';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import Icon from '/@/components/Icon';
  import {
    fetchCloudAccountDelete,
    fetchCloudAccountDisable,
    fetchCloudAccountPage,
    fetchCloudAccountTest,
  } from '/@/api/setting/cloudAccount';
  import type { CloudAccount } from '/@/api/setting/cloudAccount.type';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';
  import CloudAccountModal from './CloudAccountModal.vue';
  import CloudAccountGrantModal from './CloudAccountGrantModal.vue';
  import { columns, searchFormSchema } from './index.data';

  defineOptions({ name: 'CloudAccount' });
  const { t } = useI18n();
  const { createMessage } = useMessage();
  const testingId = ref<string>();
  const [registerAccountModal, { openModal: openAccountModal }] = useModal();
  const [registerGrantModal, { openModal: openGrantModal }] = useModal();
  const [registerTable, { reload }] = useTable({
    api: fetchCloudAccountPage,
    columns,
    formConfig: {
      schemas: searchFormSchema,
      submitOnChange: true,
      showActionButtonGroup: false,
    },
    rowKey: 'id',
    pagination: true,
    useSearchForm: true,
    showTableSetting: false,
    showIndexColumn: false,
    canResize: false,
    actionColumn: {
      width: 250,
      title: t('component.table.operation'),
      dataIndex: 'action',
      fixed: 'right',
    },
  });

  const connectivity = {
    0: { color: 'default', text: 'setting.cloudAccount.untested' },
    1: { color: 'success', text: 'setting.cloudAccount.connected' },
    2: { color: 'error', text: 'setting.cloudAccount.failed' },
  };

  function handleCreate() {
    openAccountModal(true, { isUpdate: false });
  }

  function handleEdit(record: CloudAccount) {
    openAccountModal(true, { isUpdate: true, record });
  }

  function handleGrant(record: CloudAccount) {
    openGrantModal(true, { record });
  }

  async function handleTest(record: CloudAccount) {
    testingId.value = record.id;
    try {
      const result = await fetchCloudAccountTest({ id: record.id, version: record.version });
      const requestId = result.providerRequestId
        ? ` (${t('setting.cloudAccount.requestId')}: ${result.providerRequestId})`
        : '';
      if (result.connectivityState === 1) {
        createMessage.success(`${t('setting.cloudAccount.testSuccess')}${requestId}`);
      } else {
        const error = [result.lastErrorCode, result.lastErrorMessage].filter(Boolean).join(': ');
        createMessage.error(`${error || t('setting.cloudAccount.testFailed')}${requestId}`);
      }
      await reload();
    } catch {
      createMessage.error(t('setting.cloudAccount.testFailed'));
    } finally {
      testingId.value = undefined;
    }
  }

  async function handleDisable(record: CloudAccount) {
    await fetchCloudAccountDisable({ id: record.id, version: record.version });
    createMessage.success(t('setting.cloudAccount.disabled'));
    await reload();
  }

  async function handleDelete(record: CloudAccount) {
    await fetchCloudAccountDelete({ id: record.id, version: record.version });
    createMessage.success(t('common.delSuccess'));
    await reload();
  }
</script>

<template>
  <PageWrapper content-full-height fixed-height>
    <BasicTable @register="registerTable" class="flex flex-col">
      <template #form-formFooter>
        <div class="text-right">
          <a-button type="primary" v-auth="'cloud-account:create'" @click="handleCreate">
            <Icon icon="ant-design:plus-outlined" />
            {{ t('setting.cloudAccount.create') }}
          </a-button>
        </div>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'providerType'">
          <Tag color="blue">{{
            record.providerType === 'VOLCENGINE' ? 'Volcengine' : record.providerType
          }}</Tag>
        </template>
        <template v-else-if="column.dataIndex === 'connectivityState'">
          <Tooltip
            :title="
              record.lastErrorCode
                ? `${record.lastErrorCode}: ${record.lastErrorMessage || ''}`
                : undefined
            "
          >
            <Tag :color="connectivity[record.connectivityState]?.color">
              {{
                t(connectivity[record.connectivityState]?.text || 'setting.cloudAccount.untested')
              }}
            </Tag>
          </Tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <Tag :color="record.status === 1 ? 'success' : 'default'">
            {{
              t(
                record.status === 1
                  ? 'setting.cloudAccount.enabled'
                  : 'setting.cloudAccount.disabled',
              )
            }}
          </Tag>
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:thunderbolt-outlined',
                auth: 'cloud-account:update',
                tooltip: t('setting.cloudAccount.test'),
                loading: testingId === record.id,
                disabled: record.status !== 1,
                onClick: handleTest.bind(null, record),
              },
              {
                icon: 'clarity:note-edit-line',
                auth: 'cloud-account:update',
                tooltip: t('common.edit'),
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:team-outlined',
                auth: 'cloud-account:grant',
                tooltip: t('setting.cloudAccount.grant'),
                onClick: handleGrant.bind(null, record),
              },
              {
                icon: 'ant-design:stop-outlined',
                auth: 'cloud-account:update',
                tooltip: t('setting.cloudAccount.disable'),
                disabled: record.status !== 1,
                popConfirm: {
                  title: t('setting.cloudAccount.disableConfirm'),
                  confirm: handleDisable.bind(null, record),
                },
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                auth: 'cloud-account:delete',
                tooltip: t('common.delText'),
                popConfirm: {
                  title: t('setting.cloudAccount.deleteConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <CloudAccountModal @register="registerAccountModal" @success="reload" />
    <CloudAccountGrantModal @register="registerGrantModal" @success="reload" />
  </PageWrapper>
</template>
