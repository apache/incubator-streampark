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
        <a-button type="primary" @click="handleCreate" v-auth="'user:add'">
          <Icon icon="ant-design:plus-outlined" />
          {{ t('common.add') }}
        </a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction :actions="getUserAction(record)" />
        </template>
      </template>
    </BasicTable>
    <UserDrawer @register="registerDrawer" @success="handleSuccess" />
    <UserModal @register="registerModal" />
    <Modal
      :visible="transferModalVisible"
      :confirm-loading="transferModalLoading"
      :ok-text="t('common.okText')"
      centered
      @ok="handleLockAndTransfer"
      @cancel="handleCancelTransfer"
    >
      <template #title>
        <Icon icon="ant-design:swap-outlined" />
        {{ t('system.user.form.notice') }}
      </template>
      <BasicForm @register="transferForm" class="!mt-30px !ml-36px" />
    </Modal>
  </div>
</template>
<script lang="ts">
  import { computed, defineComponent, nextTick, ref } from 'vue';

  import { ActionItem, BasicTable, TableAction, useTable } from '/@/components/Table';
  import UserDrawer from './components/UserDrawer.vue';
  import UserModal from './components/UserModal.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { getUserList, lockUser, resetPassword, unlockUser } from '/@/api/system/user';
  import { columns, searchFormSchema } from './user.data';
  import { FormTypeEnum } from '/@/enums/formEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { useModal } from '/@/components/Modal';
  import { UserListRecord } from '/@/api/system/model/userModel';
  import { useI18n } from '/@/hooks/web/useI18n';
  import Icon from '/@/components/Icon';
  import { LoginTypeEnum } from '/@/views/base/login/useLogin';
  import { BasicForm, useForm } from '/@/components/Form';
  import { Modal } from 'ant-design-vue';

  export default defineComponent({
    name: 'User',
    components: { BasicForm, Modal, BasicTable, UserModal, UserDrawer, TableAction, Icon },
    setup() {
      const transferModalVisible = ref(false);
      const transferModalLoading = ref(false);
      const curUserId = ref();
      const { t } = useI18n();
      const userStore = useUserStoreWithOut();
      const userName = computed(() => {
        return userStore.getUserInfo?.username;
      });
      const [registerDrawer, { openDrawer }] = useDrawer();
      const [registerModal, { openModal }] = useModal();
      const { createMessage, Swal } = useMessage();
      const [registerTable, { reload }] = useTable({
        title: t('system.user.table.title'),
        api: getUserList,
        columns,
        formConfig: {
          // labelWidth: 120,
          baseColProps: { style: { paddingRight: '30px' } },
          schemas: searchFormSchema,
          fieldMapToTime: [['createTime', ['createTimeFrom', 'createTimeTo'], 'YYYY-MM-DD']],
        },
        rowKey: 'userId',
        pagination: true,
        striped: false,
        useSearchForm: true,
        showTableSetting: true,
        bordered: false,
        showIndexColumn: false,
        canResize: false,
        actionColumn: {
          width: 200,
          title: t('component.table.operation'),
          dataIndex: 'action',
        },
      });

      const [transferForm, { resetFields: resetTransferFields, validate: transferValidate }] =
        useForm({
          layout: 'vertical',
          showActionButtonGroup: false,
          baseColProps: { lg: 22, md: 22 },
          schemas: [
            {
              field: 'userId',
              label: t('system.user.form.transferResource'),
              component: 'ApiSelect',
              componentProps: {
                api: async () => {
                  let { records } = await getUserList({
                    page: 1,
                    pageSize: 999999,
                    teamId: userStore.getTeamId || '',
                  });
                  return records.filter((user) => user.username !== userName.value);
                },
                labelField: 'username',
                valueField: 'userId',
                showSearch: false,
                optionFilterGroup: 'username',
                placeholder: t('system.member.userNameRequire'),
              },
              rules: [
                {
                  required: true,
                  message: t('system.member.userNameRequire'),
                  trigger: 'blur',
                },
              ],
            },
          ],
        });

      function getUserAction(record: UserListRecord): ActionItem[] {
        return [
          {
            icon: 'clarity:note-edit-line',
            tooltip: t('system.user.table.modify'),
            auth: 'user:update',
            ifShow: () => record.username !== 'admin' || userName.value === 'admin',
            onClick: handleEdit.bind(null, record),
          },
          {
            icon: 'carbon:data-view-alt',
            tooltip: t('common.detail'),
            onClick: handleView.bind(null, record),
          },
          {
            icon: 'bx:reset',
            auth: 'user:reset',
            tooltip: t('system.user.table.reset'),
            ifShow: () =>
              (record.username !== 'admin' || userName.value === 'admin') &&
              record.loginType == LoginTypeEnum[LoginTypeEnum.PASSWORD],
            popConfirm: {
              title: t('system.user.table.resetTip'),
              confirm: handleReset.bind(null, record),
            },
          },
          {
            icon: 'ant-design:lock-outlined',
            color: 'error',
            auth: 'user:delete',
            ifShow: /*record.username !== 'admin' &&*/ record.status === '1',
            tooltip: t('system.user.table.lock'),
            popConfirm: {
              title: t('system.user.table.lockTip'),
              confirm: handleLock.bind(null, record),
            },
          },
          {
            icon: 'ant-design:unlock-outlined',
            auth: 'user:delete',
            ifShow: /*record.username !== 'admin' &&*/ record.status === '0',
            tooltip: t('system.user.table.unlock'),
            onClick: handleUnLock.bind(null, record),
          },
        ];
      }
      // user create
      function handleCreate() {
        openDrawer(true, { formType: FormTypeEnum.Create });
      }
      // edit user
      function handleEdit(record: UserListRecord) {
        openDrawer(true, {
          record,
          formType: FormTypeEnum.Edit,
        });
      }

      // see detail
      function handleView(record: UserListRecord) {
        openModal(true, record);
      }

      // lock current user
      async function handleLock(record: UserListRecord) {
        const hide = createMessage.loading('locking');
        try {
          const resp = await lockUser({
            userId: record.userId,
            transferToUserId: null,
          });
          if (resp.needTransferResource) {
            curUserId.value = record.userId;
            transferModalVisible.value = true;
            nextTick(resetTransferFields);
            return;
          }
          createMessage.success(t('system.user.table.lockSuccess'));
          reload();
        } catch (error) {
          console.error('user lock fail:', error);
        } finally {
          hide();
        }
      }

      async function handleLockAndTransfer() {
        try {
          const { userId } = await transferValidate();
          transferModalLoading.value = true;
          await lockUser({
            userId: curUserId.value,
            transferToUserId: userId,
          });
          curUserId.value = null;
          createMessage.success(t('system.user.table.lockSuccess'));
          reload();
          transferModalVisible.value = false;
        } catch (e) {
          console.error(e);
        } finally {
          transferModalLoading.value = false;
        }
      }

      async function handleUnLock(record: UserListRecord) {
        try {
          await unlockUser({
            userId:record.userId
          });
          createMessage.success(t('system.user.table.unlockSuccess'));
          reload();
        } catch (e) {
          console.error(e);
        } finally {
        }
      }

      async function handleCancelTransfer() {
        transferModalVisible.value = false;
        curUserId.value = null;
      }

      async function handleReset(record: Recordable) {
        const hide = createMessage.loading('reseting');
        try {
          const resp = await resetPassword({ username: record.username });
          if (resp.data.code == 200) {
            Swal.fire({
              icon: 'success',
              title: t('system.user.resetSucceeded'),
              text: t('system.user.newPasswordTip') + resp.data.data,
            });
          }
        } catch (error) {
          console.error('user password fail:', error);
        } finally {
          hide();
        }
      }

      // add/edit user success
      function handleSuccess() {
        createMessage.success('success');
        reload();
      }

      return {
        t,
        userName,
        registerTable,
        registerDrawer,
        registerModal,
        transferForm,
        handleCreate,
        handleEdit,
        handleSuccess,
        handleView,
        handleReset,
        getUserAction,
        handleLockAndTransfer,
        handleCancelTransfer,
        transferModalVisible,
        transferModalLoading,
      };
    },
  });
</script>
