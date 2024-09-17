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
<template>
  <PageWrapper content-full-height fixed-height>
    <BasicTable @register="registerTable" class="flex flex-col">
      <template #form-formFooter>
        <Col :span="5" :offset="13" class="text-right">
          <a-button
            id="e2e-yarnqueue-create-btn"
            type="primary"
            @click="handleCreate"
            v-auth="'yarnQueue:create'"
          >
            <Icon icon="ant-design:plus-outlined" />
            {{ t('common.add') }}
          </a-button>
        </Col>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                class: 'e2e-yarnqueue-edit-btn',
                icon: 'clarity:note-edit-line',
                auth: 'yarnQueue:update',
                tooltip: t('common.edit'),
                onClick: handleYarnQueueEdit.bind(null, record),
              },
              {
                class: 'e2e-yarnqueue-delete-btn',
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: t('common.delText'),
                auth: 'yarnQueue:delete',
                popConfirm: {
                  okButtonProps: {
                    class: 'e2e-yarnqueue-delete-confirm',
                  },
                  title: t('setting.yarnQueue.deleteConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <YarnQueueModal @register="registerModal" @success="handleSuccess" />
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';
  import { Col } from 'ant-design-vue';
  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import YarnQueueModal from './YarnQueueModal.vue';
  import { useModal } from '/@/components/Modal';
  import Icon from '/@/components/Icon';
  import { columns, searchFormSchema } from './index.data';
  import { fetchYarnQueueList, fetchYarnQueueDelete } from '/@/api/setting/yarnQueue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { PageWrapper } from '/@/components/Page';

  export default defineComponent({
    name: 'YarnQueue',
    components: { BasicTable, YarnQueueModal, TableAction, Icon, PageWrapper, Col },
    setup() {
      const [registerModal, { openModal }] = useModal();
      const { createMessage } = useMessage();
      const { t } = useI18n();
      const [registerTable, { reload }] = useTable({
        api: fetchYarnQueueList,
        columns,
        formConfig: {
          schemas: searchFormSchema,
          rowProps: {
            gutter: 14,
          },
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
          width: 200,
          title: t('component.table.operation'),
          dataIndex: 'action',
        },
      });

      function handleCreate() {
        openModal(true, {
          isUpdate: false,
        });
      }

      function handleYarnQueueEdit(record: Recordable) {
        openModal(true, {
          record,
          isUpdate: true,
        });
      }

      /* Delete the organization */
      async function handleDelete(record: Recordable) {
        const { data } = await fetchYarnQueueDelete({ id: record.id });
        if (data.status === 'success') {
          createMessage.success(
            `${t('setting.yarnQueue.deleteYarnQueue')} ${t('setting.yarnQueue.success')}`,
          );
          reload();
        } else {
          createMessage.error(`${t('setting.yarnQueue.deleteYarnQueue')} ${t('common.failed')}`);
        }
      }

      function handleSuccess(isUpdate: boolean) {
        createMessage.success(
          `${isUpdate ? t('common.edit') : t('setting.yarnQueue.createQueue')} ${t(
            'setting.yarnQueue.success',
          )}`,
        );
        reload();
      }

      return {
        t,
        registerTable,
        registerModal,
        handleCreate,
        handleYarnQueueEdit,
        handleDelete,
        handleSuccess,
      };
    },
  });
</script>
