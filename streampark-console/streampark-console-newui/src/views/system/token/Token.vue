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
    <BasicTable @register="registerTable" @fetch-success="onFetchSuccess">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate"> Add Token</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:copy-outlined',
                tooltip: 'Copy Token',
                onClick: handleCopy.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
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
  import { defineComponent, nextTick, unref } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import TokenDrawer from './TokenDrawer.vue';
  import { useCopyToClipboard } from '/@/hooks/web/useCopyToClipboard';
  import { useDrawer } from '/@/components/Drawer';
  import { deleteToken, getTokenList } from '/@/api/sys/token';
  import { columns, searchFormSchema } from './token.data';
  import { useMessage } from '/@/hooks/web/useMessage';

  export default defineComponent({
    name: 'User',
    components: { BasicTable, TokenDrawer, TableAction },
    setup() {
      const { notification, createMessage } = useMessage();
      const [registerDrawer, { openDrawer }] = useDrawer();
      const { clipboardRef, copiedRef } = useCopyToClipboard();
      const [registerTable, { reload, expandAll, updateTableDataRecord }] = useTable({
        title: '',
        api: getTokenList,
        columns,
        formConfig: {
          labelWidth: 120,
          schemas: searchFormSchema,
          resetButtonOptions: {
            text: 'reset',
          },
          submitButtonOptions: {
            text: 'search',
          },
        },
        isTreeTable: true,
        pagination: true,
        striped: false,
        useSearchForm: true,
        showTableSetting: false,
        bordered: true,
        rowKey: 'tokenId',
        showIndexColumn: false,
        canResize: false,
        actionColumn: {
          width: 120,
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

      function handleView(record: Recordable) {
        console.log(record);
      }

      function handleDelete(record: Recordable) {
        deleteToken({ tokenId: record.id });
      }

      // function handleSuccess() {
      //   reload();
      // }

      function handleSuccess({ isUpdate, values }) {
        notification.success({
          message: 'Tip',
          description: 'Success',
        });
        isUpdate ? updateTableDataRecord(values.tokenId, values) : reload();
      }

      function onFetchSuccess(res) {
        console.log(res);
        // Demo expands all table items by default
        nextTick(expandAll);
      }

      return {
        registerTable,
        registerDrawer,
        handleCreate,
        handleCopy,
        handleDelete,
        handleSuccess,
        onFetchSuccess,
        handleView,
      };
    },
  });
</script>
