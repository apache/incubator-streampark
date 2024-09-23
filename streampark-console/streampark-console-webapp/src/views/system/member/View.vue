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
  <PageWrapper content-full-height fixed-height>
    <BasicTable @register="registerTable" :formConfig="formConfig" class="flex flex-col">
      <template #form-formFooter>
        <Col :span="5" :offset="7" class="text-right">
          <a-button
            id="e2e-member-create-btn"
            type="primary"
            @click="handleCreate"
            v-auth="'member:add'"
          >
            <Icon icon="ant-design:plus-outlined" />
            {{ t('common.add') }}
          </a-button>
        </Col>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                class: 'e2e-member-edit-btn',
                icon: 'clarity:note-edit-line',
                auth: 'member:update',
                tooltip: t('system.member.modifyMember'),
                onClick: handleEdit.bind(null, record),
              },
              {
                class: 'e2e-member-delete-btn',
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: t('system.member.deleteMember'),
                auth: 'member:delete',
                popConfirm: {
                  okButtonProps: {
                    class: 'e2e-member-delete-confirm',
                  },
                  title: t('system.member.deletePopConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <MemberModal
      @register="registerModal"
      @success="handleSuccess"
      :roleOptions="roleListOptions"
    />
  </PageWrapper>
</template>

<script setup lang="ts" name="member">
  import { computed, onMounted, ref, unref } from 'vue';
  import { Col } from 'ant-design-vue';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { RoleListItem } from '/@/api/base/model/systemModel';
  import { useGo } from '/@/hooks/web/usePage';
  import { BasicTable, useTable, TableAction, FormProps } from '/@/components/Table';
  import MemberModal from './MemberModal.vue';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { getRoleListByPage } from '/@/api/base/system';
  import { fetchMemberDelete, fetchMemberList } from '/@/api/system/member';
  import Icon from '/@/components/Icon';
  import { PageWrapper } from '/@/components/Page';
  defineOptions({
    name: 'Member',
  });
  const roleListOptions = ref<Array<Partial<RoleListItem>>>([]);

  const [registerModal, { openModal }] = useModal();
  const { createMessage } = useMessage();
  const go = useGo();
  const { t } = useI18n();
  const userStore = useUserStoreWithOut();
  const formConfig = computed((): Partial<FormProps> => {
    return {
      schemas: [
        {
          field: 'userName',
          label: '',
          component: 'Input',
          componentProps: {
            placeholder: t('system.member.searchByUser'),
            allowClear: true,
          },
          colProps: { span: 6 },
        },
        {
          field: 'roleName',
          label: '',
          component: 'Select',
          componentProps: {
            placeholder: t('system.member.searchByRole'),
            options: unref(roleListOptions),
            fieldNames: { label: 'roleName', value: 'roleName' },
            allowClear: true,
          },
          colProps: { span: 6 },
        },
      ],
      rowProps: {
        gutter: 14,
      },
      submitOnChange: true,
      showActionButtonGroup: false,
    };
  });
  const [registerTable, { reload }] = useTable({
    rowKey: 'id',
    api: fetchMemberList,
    columns: [
      { title: t('system.member.table.userName'), dataIndex: 'userName', sorter: true },
      { title: t('system.member.table.roleName'), dataIndex: 'roleName', sorter: true },
      { title: t('common.createTime'), dataIndex: 'createTime', sorter: true },
      { title: t('common.modifyTime'), dataIndex: 'modifyTime', sorter: true },
    ],
    pagination: true,
    useSearchForm: true,
    showTableSetting: false,
    showIndexColumn: false,
    canResize: false,
    actionColumn: {
      width: 200,
      title: t('component.table.operation'),
      dataIndex: 'action',
    },
    immediate: false,
  });

  function handleCreate() {
    openModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  /* Delete members */
  async function handleDelete(record: Recordable) {
    const { data } = await fetchMemberDelete({ id: record.id });
    if (data.status === 'success') {
      createMessage.success(t('system.member.deleteMember') + t('system.member.success'));
      reload();
    } else {
      createMessage.error(t('system.member.deleteMember') + t('system.member.fail'));
    }
  }

  function handleSuccess(isUpdate: boolean) {
    createMessage.success(
      `${isUpdate ? t('common.edit') : t('system.member.addMember')} ${t('system.member.success')}`,
    );
    reload();
  }
  onMounted(async () => {
    if (!userStore.getTeamId) {
      createMessage.warning('Please select Team first!!!');
      go('/system/team');
    } else {
      reload();
      const roleList = await getRoleListByPage({ page: 1, pageSize: 9999 });
      roleListOptions.value = roleList?.records;
    }
  });
</script>
