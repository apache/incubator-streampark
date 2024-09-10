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
        <a-button id="e2e-variable-create-btn" type="primary" @click="handleCreate" v-auth="'variable:add'">
          <Icon icon="ant-design:plus-outlined" />
          {{ t('common.add') }}
        </a-button>
      </template>
      <template #resetBefore> 1111 </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                class: 'e2e-variable-edit-btn',
                icon: 'clarity:note-edit-line',
                auth: 'variable:update',
                tooltip: t('flink.variable.modifyVariable'),
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'carbon:data-view-alt',
                tooltip: t('common.detail'),
                onClick: handleView.bind(null, record),
              },
              {
                icon: 'icon-park-outline:mind-mapping',
                tooltip: t('flink.variable.table.depend'),
                auth: 'variable:depend_apps',
                onClick: () =>
                  router.push('/resource/variable/depend_apps?id=' + record.variableCode),
              },
              {
                class: 'e2e-variable-delete-btn',
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: t('flink.variable.deleteVariable'),
                auth: 'variable:delete',
                popConfirm: {
                  okButtonProps: {
                    class: 'e2e-variable-delete-confirm',
                  },
                  title: t('flink.variable.deletePopConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <VariableDrawer @register="registerDrawer" @success="handleSuccess" />
    <VariableInfo @register="registerInfo" />
  </div>
</template>
<script lang="ts">
  export default defineComponent({
    name: 'Variable',
  });
</script>

<script lang="ts" setup>
  import { defineComponent } from 'vue';
  import { BasicTable, useTable, TableAction, SorterResult } from '/@/components/Table';
  import VariableDrawer from './components/VariableDrawer.vue';
  import VariableInfo from './components/VariableInfo.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { columns, searchFormSchema } from './variable.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { fetchVariableDelete, fetchVariableList } from '/@/api/resource/variable';
  import Icon from '/@/components/Icon';
  import { useRouter } from 'vue-router';

  const router = useRouter();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const [registerInfo, { openDrawer: openInfoDraw }] = useDrawer();
  const { createMessage } = useMessage();
  const { t } = useI18n();
  const [registerTable, { reload }] = useTable({
    title: t('flink.variable.table.title'),
    api: fetchVariableList,
    columns,
    formConfig: {
      baseColProps: { style: { paddingRight: '30px' } },
      schemas: searchFormSchema,
    },
    sortFn: (sortInfo: SorterResult) => {
      const { field, order } = sortInfo;
      if (field && order) {
        return {
          // The sort field passed to the backend you
          sortField: field,
          // Sorting method passed to the background asc/desc
          sortOrder: order === 'ascend' ? 'asc' : 'desc',
        };
      } else {
        return {};
      }
    },
    rowKey: 'id',
    pagination: true,
    useSearchForm: true,
    showTableSetting: true,
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

  function handleEdit(record: Recordable) {
    openDrawer(true, {
      record,
      isUpdate: true,
    });
  }
  function handleView(record: Recordable) {
    openInfoDraw(true, record);
  }
  /* Delete the organization */
  async function handleDelete(record: Recordable) {
    const { data } = await fetchVariableDelete({
      id: record.id,
      teamId: record.teamId,
      variableCode: record.variableCode,
      variableValue: record.variableValue,
    });
    if (data.status === 'success') {
      createMessage.success(t('flink.variable.deleteVariable') + t('flink.variable.success'));
      reload();
    } else {
      createMessage.error(t('flink.variable.deleteVariable') + t('flink.variable.fail'));
    }
  }

  function handleSuccess(isUpdate: boolean) {
    createMessage.success(
      `${isUpdate ? t('common.edit') : t('flink.variable.add')}${t('flink.variable.success')}`,
    );
    reload();
  }
</script>
