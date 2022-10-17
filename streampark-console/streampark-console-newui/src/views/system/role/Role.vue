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
        <a-button type="primary" @click="handleCreate" v-auth="'role:add'"> Add Role</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                auth: 'role:update',
                ifShow: record.roleName !== 'admin' || userName === 'admin',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'carbon:data-view-alt',
                tooltip: 'view detail',
                onClick: handleView.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                auth: 'role:delete',
                ifShow: record.roleName !== 'admin',
                popConfirm: {
                  title: 'Are you sure delete this Role',
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <RoleDrawer @register="registerDrawer" @success="handleSuccess" />
    <RoleInfo @register="registerInfo" />
  </div>
</template>

<script lang="ts">
  import { defineComponent } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import { getRoleListByPage } from '/@/api/base/system';

  import { useDrawer } from '/@/components/Drawer';
  import RoleDrawer from './components/RoleDrawer.vue';
  import RoleInfo from './components/RoleInfo.vue';

  import { columns, searchFormSchema } from './role.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { FormTypeEnum } from '/@/enums/formEnum';
  import { deleteRole } from '/@/api/system/role';
  import { useUserStoreWithOut } from '/@/store/modules/user';

  export default defineComponent({
    name: 'RoleManagement',
    components: { BasicTable, RoleInfo, RoleDrawer, TableAction },
    setup() {
      const [registerDrawer, { openDrawer }] = useDrawer();
      const [registerInfo, { openDrawer: openInfoDraw }] = useDrawer();
      const { createMessage } = useMessage();
      const useStore = useUserStoreWithOut();
      const [registerTable, { reload }] = useTable({
        title: '',
        api: getRoleListByPage,
        columns,
        formConfig: {
          labelWidth: 120,
          schemas: searchFormSchema,
        },
        useSearchForm: true,
        showTableSetting: false,
        bordered: true,
        showIndexColumn: false,
        actionColumn: {
          width: 120,
          title: 'Operation',
          dataIndex: 'action',
        },
      });

      function handleCreate() {
        openDrawer(true, { formType: FormTypeEnum.Create });
      }

      function handleEdit(record: Recordable) {
        openDrawer(true, { record, formType: FormTypeEnum.Edit });
      }

      function handleDelete(record: Recordable) {
        deleteRole({ roleId: record.roleId }).then((_) => {
          createMessage.success('success');
          reload();
        });
      }

      function handleSuccess() {
        createMessage.success('success');
        reload();
      }

      function handleView(record: Recordable) {
        openInfoDraw(true, record);
      }

      return {
        registerTable,
        registerInfo,
        registerDrawer,
        handleCreate,
        handleEdit,
        handleDelete,
        handleSuccess,
        handleView,
        userName: useStore.getUserInfo?.username,
      };
    },
  });
</script>
