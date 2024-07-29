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
  <PageWrapper>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate" v-auth="'gateway:add'">
          <Icon icon="ant-design:plus-outlined" />
          {{ t('common.add') }}
        </a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                auth: 'gateway:delete',
                tooltip: t('setting.flinkGateway.deleteGateway'),
                popConfirm: {
                  title: t('setting.flinkGateway.operation.deleteConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <FlinkGatewayDrawer @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import FlinkGatewayDrawer from './components/FlinkGatewayDrawer.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { fetchGatewayDelete, fetchGatewayList } from '/@/api/flink/flinkGateway';
  import { columns, searchFormSchema } from './index.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import Icon from '/@/components/Icon';
  import { PageWrapper } from '/@/components/Page';
  export default defineComponent({
    name: 'FlinkGateway',
    components: {
      BasicTable,
      FlinkGatewayDrawer: FlinkGatewayDrawer,
      TableAction,
      Icon,
      PageWrapper,
    },
    setup() {
      const { t } = useI18n();
      const { createMessage } = useMessage();
      const [registerDrawer, { openDrawer }] = useDrawer();
      const [registerTable, { reload, updateTableDataRecord }] = useTable({
        title: t('setting.flinkGateway.tableTitle'),
        api: fetchGatewayList,
        // beforeFetch: (params) => {
        //   if (params.user) {
        //     params.username = params.user;
        //     delete params.user;
        //   }
        //   return params;
        // },
        columns,
        formConfig: {
          baseColProps: { style: { paddingRight: '30px' } },
          schemas: searchFormSchema,
        },
        useSearchForm: false,
        showTableSetting: true,
        rowKey: 'id',
        showIndexColumn: false,
        canResize: false,
        actionColumn: {
          width: 200,
          title: t('component.table.operation'),
          dataIndex: 'action',
        },
      });

      function handleCreate() {
        openDrawer(true, {
          isUpdate: false,
        });
      }

      async function handleDelete(record: Recordable) {
        const resp = await fetchGatewayDelete(record.id);
        if (resp) {
          createMessage.success(t('setting.flinkGateway.operation.deleteSuccess'));
          reload();
        } else {
          createMessage.success(t('setting.flinkGateway.operation.deleteFailed'));
        }
      }

      function handleSuccess({ isUpdate, values }) {
        if (isUpdate) {
          createMessage.success(t('setting.flinkGateway.operation.updateSuccess'));
          updateTableDataRecord(values.id, values);
        } else {
          createMessage.success(t('setting.flinkGateway.operation.createSuccess'));
          reload();
        }
      }

      return {
        t,
        registerTable,
        registerDrawer,
        handleCreate,
        handleDelete,
        handleSuccess,
      };
    },
  });
</script>
