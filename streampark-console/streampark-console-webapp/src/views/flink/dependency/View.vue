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
        <a-button type="primary" @click="handleCreate" v-auth="'dependency:add'">
          <Icon icon="ant-design:plus-outlined" />
          {{ t('common.add') }}
        </a-button>
      </template>
      <template #resetBefore> 1111 </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'resourceType'">
          <Tag
            class="bold-tag"
            color="#52c41a"
            v-if="record.resourceType == ResourceTypeEnum.APP"
          >
            APP
          </Tag>
          <Tag
            class="bold-tag"
            color="#2db7f5"
            v-if="record.resourceType == ResourceTypeEnum.COMMON"
          >
            COMMON
          </Tag>
          <Tag
            class="bold-tag"
            color="#108ee9"
            v-if="record.resourceType == ResourceTypeEnum.CONNECTOR"
          >
            CONNECTOR
          </Tag>
          <Tag
            class="bold-tag"
            color="#102541"
            v-if="record.resourceType == ResourceTypeEnum.FORMAT"
          >
            FORMAT
          </Tag>
          <Tag
            class="bold-tag"
            color="#2db7f5"
            v-if="record.resourceType == ResourceTypeEnum.UDF"
          >
            UDF
          </Tag>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                auth: 'dependency:update',
                tooltip: t('flink.dependency.modifyDependency'),
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: t('flink.dependency.deleteDependency'),
                auth: 'dependency:delete',
                popConfirm: {
                  title: t('flink.dependency.deletePopConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <DependencyDrawer @register="registerDrawer" @success="handleSuccess" />
  </div>
</template>
<script lang="ts">
  export default defineComponent({
    name: 'Dependency',
  });
</script>

<script lang="ts" setup>
  import {defineComponent, ref} from 'vue';
  import { BasicTable, useTable, TableAction, SorterResult } from '/@/components/Table';
  import DependencyDrawer from './components/DependencyDrawer.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { columns, searchFormSchema } from './dependency.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import Icon from '/@/components/Icon';
  import { useRouter } from 'vue-router';
  import { fetchDependencyDelete, fetchDependencyList } from "/@/api/flink/dependency";
  import { ResourceTypeEnum } from "/@/views/flink/dependency/dependency.data";
  import { Tag } from 'ant-design-vue';

  const router = useRouter();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const [registerInfo, { openDrawer: openInfoDraw }] = useDrawer();
  const { createMessage } = useMessage();
  const { t } = useI18n();
  const [registerTable, { reload }] = useTable({
    title: t('flink.dependency.table.title'),
    api: fetchDependencyList,
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

  /* Delete the dependency */
  async function handleDelete(record: Recordable) {
    const { data } = await fetchDependencyDelete({
      id: record.id,
      teamId: record.teamId,
      dependencyName: record.dependencyName,
    });
    if (data.status === 'success') {
      createMessage.success(t('flink.dependency.deleteDependency') + t('flink.dependency.success'));
      reload();
    } else {
      createMessage.error(t('flink.dependency.deleteDependency') + t('flink.dependency.fail'));
    }
  }

  function handleSuccess(isUpdate: boolean) {
    createMessage.success(
      `${isUpdate ? t('common.edit') : t('flink.dependency.add')}${t('flink.dependency.success')}`,
    );
    reload();
  }

</script>
