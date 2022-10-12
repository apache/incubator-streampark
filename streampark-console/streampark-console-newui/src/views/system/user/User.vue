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
        <a-button type="primary" @click="handleCreate" v-auth="'user:add'"> Add User </a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                tooltip: 'modify',
                auth: 'user:update',
                ifShow: () => record.username !== 'admin' || userName === 'admin',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'carbon:data-view-alt',
                tooltip: 'view detail',
                onClick: handleView.bind(null, record),
              },
              {
                icon: 'bx:reset',
                auth: 'user:reset',
                tooltip: 'reset password',
                ifShow: () => record.username !== 'admin' || userName === 'admin',
                popConfirm: {
                  title: 'reset password, are you sure',
                  confirm: handleReset.bind(null, record),
                },
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                auth: 'user:delete',
                ifShow: record.username !== 'admin',
                tooltip: 'delete user',
                popConfirm: {
                  title: 'delete user, are you sure',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <UserDrawer @register="registerDrawer" @success="handleSuccess" />
  </div>
</template>
<script lang="ts">
  import { computed, defineComponent } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import UserDrawer from './UserDrawer.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { deleteUser, getUserList, resetPassword } from '/@/api/sys/user';
  import { columns, searchFormSchema } from './user.data';
  import { FormTypeEnum } from '/@/enums/formEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useUserStoreWithOut } from '/@/store/modules/user';

  export default defineComponent({
    name: 'User',
    components: { BasicTable, UserDrawer, TableAction },
    setup() {
      const userStore = useUserStoreWithOut();
      const userName = computed(() => {
        return userStore.getUserInfo?.username;
      });
      const [registerDrawer, { openDrawer }] = useDrawer();
      const { createMessage, createSuccessModal } = useMessage();
      const [registerTable, { reload }] = useTable({
        title: '',
        api: getUserList,
        columns,
        formConfig: {
          labelWidth: 120,
          schemas: searchFormSchema,
          fieldMapToTime: [['createTime', ['createTimeFrom', 'createTimeTo'], 'YYYY-MM']],
        },
        rowKey: 'userId',
        pagination: true,
        striped: false,
        useSearchForm: true,
        showTableSetting: false,
        bordered: false,
        showIndexColumn: false,
        canResize: false,
        actionColumn: {
          width: 140,
          title: 'Operation',
          dataIndex: 'action',
        },
      });

      function handleCreate() {
        openDrawer(true, { formType: FormTypeEnum.Create });
      }

      function handleEdit(record: Recordable) {
        openDrawer(true, {
          record,
          formType: FormTypeEnum.Edit,
        });
      }

      // see detail
      function handleView(record: Recordable) {
        openDrawer(true, {
          record,
          formType: FormTypeEnum.View,
        });
      }

      // delete current user
      function handleDelete(record: Recordable) {
        deleteUser({ userId: record.userId }).then((_) => {
          createMessage.success('success');
          reload();
        });
      }

      function handleReset(record: Recordable) {
        resetPassword({ usernames: record.username }).then((_) => {
          createSuccessModal({
            title: 'reset password successful',
            content: `user [${record.username}] new password is streamx666`,
          });
        });
      }

      // add/edit user success
      function handleSuccess() {
        createMessage.success('success');
        reload();
      }

      return {
        userName,
        registerTable,
        registerDrawer,
        handleCreate,
        handleEdit,
        handleDelete,
        handleSuccess,
        handleView,
        handleReset,
      };
    },
  });
</script>
