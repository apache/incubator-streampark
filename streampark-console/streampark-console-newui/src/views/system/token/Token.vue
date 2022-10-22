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
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate" v-auth="'token:add'">
          <Icon icon="ant-design:plus-outlined" />
          {{ t('common.add') }}
        </a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:copy-outlined',
                tooltip: 'Copy Token',
                auth: 'token:view',
                onClick: handleCopy.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                auth: 'token:delete',
                tooltip: 'Delete Token',
                popConfirm: {
                  title: 'are you sure delete this token ?',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <TokenDrawer @register="registerDrawer" @success="handleSuccess" />
  </div>
</template>
<script lang="ts">
  import { defineComponent, unref } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import TokenDrawer from './components/TokenDrawer.vue';
  import { useCopyToClipboard } from '/@/hooks/web/useCopyToClipboard';
  import { useDrawer } from '/@/components/Drawer';
  import { fetchTokenDelete, fetTokenList } from '/@/api/system/token';
  import { columns, searchFormSchema } from './token.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import Icon from '/@/components/Icon';

  export default defineComponent({
    name: 'UserToken',
    components: { BasicTable, TokenDrawer, TableAction, Icon },
    setup() {
      const { t } = useI18n();
      const { createMessage } = useMessage();
      const [registerDrawer, { openDrawer }] = useDrawer();
      const { clipboardRef, copiedRef } = useCopyToClipboard();
      const [registerTable, { reload, updateTableDataRecord }] = useTable({
        api: fetTokenList,
        columns,
        formConfig: { labelWidth: 120, schemas: searchFormSchema },
        useSearchForm: true,
        showTableSetting: false,
        rowKey: 'tokenId',
        showIndexColumn: false,
        canResize: false,
        actionColumn: {
          width: 200,
          title: 'Operation',
          dataIndex: 'action',
        },
      });

      function handleCreate() {
        openDrawer(true, {
          isUpdate: false,
        });
      }

      function handleCopy(record: Recordable) {
        clipboardRef.value = record.token;
        unref(copiedRef) && createMessage.success('copy success！');
      }

      async function handleDelete(record: Recordable) {
        const res = await fetchTokenDelete({ tokenId: record.id });
        if (res) {
          createMessage.success('delete token successfully');
          reload();
        } else {
          createMessage.success('delete token failed');
        }
      }

      function handleSuccess({ isUpdate, values }) {
        if (isUpdate) {
          createMessage.success('update token successfully');
          updateTableDataRecord(values.tokenId, values);
        } else {
          createMessage.success('create token successfully');
          reload();
        }
      }

      return {
        t,
        registerTable,
        registerDrawer,
        handleCreate,
        handleCopy,
        handleDelete,
        handleSuccess,
      };
    },
  });
</script>
