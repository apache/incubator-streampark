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
        <a-button type="primary" @click="handleCreate" v-auth="'catalog:add'">
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
                auth: 'catalog:delete',
                tooltip: t('setting.flinkCatalog.deleteCatalog'),
                popConfirm: {
                  title: t('setting.flinkCatalog.deleteConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <FlinkCatalogDrawer @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import FlinkCatalogDrawer from './components/FlinkCatalogDrawer.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { fetchCatalogDelete, fetchCatalogList } from '/@/api/flink/setting/flinkCatalog';
  import { columns, searchFormSchema } from './index.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import Icon from '/@/components/Icon';
  import { PageWrapper } from '/@/components/Page';
  export default defineComponent({
    name: 'FlinkCatalog',
    components: {
      BasicTable,
      FlinkCatalogDrawer: FlinkCatalogDrawer,
      TableAction,
      Icon,
      PageWrapper,
    },
    setup() {
      const { t } = useI18n();
      const { createMessage } = useMessage();
      const [registerDrawer, { openDrawer }] = useDrawer();
      const [registerTable, { reload, updateTableDataRecord }] = useTable({
        title: t('setting.flinkCatalog.tableTitle'),
        api: fetchCatalogList,
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
        const res = await fetchCatalogDelete(record.id);
        if (res) {
          createMessage.success(t('setting.flinkCatalog.operation.deleteSuccess'));
          reload();
        } else {
          createMessage.success(t('setting.flinkCatalog.operation.deleteFailed'));
        }
      }

      function handleSuccess({ isUpdate, values }) {
        if (isUpdate) {
          createMessage.success(t('setting.flinkCatalog.operation.updateSuccess'));
          updateTableDataRecord(values.id, values);
        } else {
          createMessage.success(t('setting.flinkCatalog.operation.createSuccess'));
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
