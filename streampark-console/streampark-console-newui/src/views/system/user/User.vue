<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate"> Add User </a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: 'modify',
              auth: 'user:update',
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
    </BasicTable>
    <UserDrawer @register="registerDrawer" @success="handleSuccess" />
  </div>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import UserDrawer from './UserDrawer.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { deleteUser, getUserList, resetPassword } from '/@/api/sys/user';
  import { columns, searchFormSchema } from './user.data';
  import { FormTypeEnum } from '/@/enums/formEnum';
  import { useMessage } from '/@/hooks/web/useMessage';

  export default defineComponent({
    name: 'User',
    components: { BasicTable, UserDrawer, TableAction },
    setup() {
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
        isTreeTable: true,
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
          slots: { customRender: 'action' },
          fixed: 'right',
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
