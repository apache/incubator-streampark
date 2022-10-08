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
        <a-button type="primary" @click="handleCreate"> Add Team </a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              tooltip: 'delete Team',
              popConfirm: {
                title: 'delete Team, are you sure',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <TeamDrawer @register="registerDrawer" @success="handleSuccess" />
  </div>
</template>
<script lang="ts">
  import { defineComponent, nextTick } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import TeamDrawer from './TeamDrawer.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { columns, searchFormSchema } from './team.data';
  import { getTeamList, deleteTeam } from '/@/api/sys/team';
  import { useMessage } from '/@/hooks/web/useMessage';

  export default defineComponent({
    name: 'User',
    components: { BasicTable, TeamDrawer, TableAction },
    setup() {
      const [registerDrawer, { openDrawer }] = useDrawer();
      const { createMessage } = useMessage();
      const [registerTable, { reload, expandAll }] = useTable({
        title: 'Team List',
        api: getTeamList,
        columns,
        formConfig: {
          labelWidth: 120,
          schemas: searchFormSchema,
          fieldMapToTime: [['createTime', ['createTimeFrom', 'createTimeTo'], 'YYYY-MM-DD']],
        },
        rowKey: 'teamId',
        isTreeTable: true,
        pagination: true,
        striped: false,
        useSearchForm: true,
        showTableSetting: false,
        bordered: true,
        showIndexColumn: false,
        canResize: false,
        actionColumn: {
          width: 120,
          title: 'Operation',
          dataIndex: 'action',
          slots: { customRender: 'action' },
          fixed: 'right',
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
        console.log(record);
      }

      function handleDelete(record: Recordable) {
        deleteTeam({ teamId: record.teamId }).then((_) => {
          createMessage.success('delete successful');
          reload();
        });
      }

      function handleSuccess() {
        createMessage.success('add successful');
        reload();
      }

      function onFetchSuccess() {
        // 演示默认展开所有表项
        nextTick(expandAll);
      }

      function onChange(val) {
        console.log(val);
      }
      return {
        registerTable,
        registerDrawer,
        handleCreate,
        handleEdit,
        handleDelete,
        handleSuccess,
        onFetchSuccess,
        handleView,
        onChange,
      };
    },
  });
</script>
