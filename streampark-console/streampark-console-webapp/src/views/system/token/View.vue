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
            id="e2e-token-create-btn"
            type="primary"
            @click="handleCreate"
            v-auth="'token:add'"
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
                class: 'e2e-token-copy-btn',
                icon: 'ant-design:copy-outlined',
                tooltip: t('system.token.copyToken'),
                auth: 'token:view',
                onClick: handleCopy.bind(null, record),
              },
              {
                class: 'e2e-token-delete-btn',
                icon: 'ant-design:delete-outlined',
                color: 'error',
                auth: 'token:delete',
                tooltip: t('system.token.deleteToken'),
                popConfirm: {
                  okButtonProps: {
                    class: 'e2e-token-delete-confirm',
                  },
                  title: t('system.token.operation.deleteTokenConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <TokenModal @register="registerModal" @success="handleSuccess" />
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent, unref } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import TokenModal from './components/TokenModal.vue';
  import { useCopyToClipboard } from '/@/hooks/web/useCopyToClipboard';
  import { useModal } from '/@/components/Modal';
  import { fetchTokenDelete, fetTokenList } from '/@/api/system/token';
  import { columns, searchFormSchema } from './token.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import Icon from '/@/components/Icon';
  import { PageWrapper } from '/@/components/Page';
  import { Col } from 'ant-design-vue';
  export default defineComponent({
    name: 'UserToken',
    components: { Col, BasicTable, TokenModal, TableAction, Icon, PageWrapper },
    setup() {
      const { t } = useI18n();
      const { createMessage } = useMessage();
      const [registerModal, { openModal }] = useModal();
      const { clipboardRef, copiedRef } = useCopyToClipboard();
      const [registerTable, { reload, updateTableDataRecord }] = useTable({
        rowKey: 'id',
        api: fetTokenList,
        // beforeFetch: (params) => {
        //   if (params.user) {
        //     params.username = params.user;
        //     delete params.user;
        //   }
        //   return params;
        // },
        columns,
        formConfig: {
          schemas: searchFormSchema,
          rowProps: {
            gutter: 14,
          },
          submitOnChange: true,
          showActionButtonGroup: false,
        },
        pagination: true,
        striped: false,
        useSearchForm: true,
        showTableSetting: false,
        bordered: false,
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

      function handleCopy(record: Recordable) {
        clipboardRef.value = record.token;
        unref(copiedRef) && createMessage.success(t('system.token.operation.copySuccess'));
      }

      async function handleDelete(record: Recordable) {
        const res = await fetchTokenDelete({ tokenId: record.id });
        if (res) {
          createMessage.success(t('system.token.operation.deleteSuccess'));
          reload();
        } else {
          createMessage.success(t('system.token.operation.deleteFailed'));
        }
      }

      function handleSuccess({ isUpdate, values }) {
        if (isUpdate) {
          createMessage.success(t('system.token.operation.updateSuccess'));
          updateTableDataRecord(values.tokenId, values);
        } else {
          createMessage.success(t('system.token.operation.createSuccess'));
          reload();
        }
      }

      return {
        t,
        registerTable,
        registerModal,
        handleCreate,
        handleCopy,
        handleDelete,
        handleSuccess,
      };
    },
  });
</script>
