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
    <BasicTable @register="registerTable" :formConfig="formConfig">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate" v-auth="'member:add'">
          {{ t('system.member.addMember') }}
        </a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:edit-outlined',
                auth: 'member:update',
                tooltip: t('system.member.modifyMember'),
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: t('system.member.deleteMember'),
                auth: 'member:delete',
                popConfirm: {
                  title: t('system.member.deletePopConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <MemberDrawer
      @register="registerDrawer"
      @success="handleSuccess"
      :roleOptions="roleListOptions"
    />
  </div>
</template>
<script lang="ts">
  import { computed, defineComponent, onMounted, ref, unref } from 'vue';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { RoleListItem } from '/@/api/demo/model/systemModel';

  export default defineComponent({
    name: 'Member',
  });
</script>

<script setup lang="ts" name="member">
  import { BasicTable, useTable, TableAction, FormProps } from '/@/components/Table';
  import MemberDrawer from './MemberDrawer.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { getRoleListByPage } from '/@/api/demo/system';
  import { fetchMemberDelete, fetchMemberList } from '/@/api/sys/member';

  const roleListOptions = ref<Array<Partial<RoleListItem>>>([]);

  const [registerDrawer, { openDrawer }] = useDrawer();
  const { createMessage } = useMessage();
  const { close } = useTabs();
  const { t } = useI18n();
  const userStore = useUserStoreWithOut();
  const formConfig = computed((): Partial<FormProps> => {
    return {
      labelWidth: 120,
      schemas: [
        {
          field: 'userName',
          label: t('system.member.table.userName'),
          component: 'Input',
          colProps: { span: 6 },
        },
        {
          field: 'roleName',
          label: t('system.member.table.roleName'),
          component: 'Select',
          componentProps: {
            options: unref(roleListOptions),
            fieldNames: { label: 'roleName', value: 'roleName' },
          },
          colProps: { span: 6 },
        },
        {
          field: 'createTime',
          label: t('system.team.table.createTime'),
          component: 'RangePicker',
          colProps: { span: 6 },
        },
      ],
      fieldMapToTime: [['createTime', ['createTimeFrom', 'createTimeTo'], 'YYYY-MM-DD']],
    };
  });
  const [registerTable, { reload }] = useTable({
    title: t('system.member.table.title'),
    api: fetchMemberList,
    beforeFetch: (params) => {
      params.teamId = userStore.getTeamId;
      return params;
    },
    columns: [
      { title: t('system.member.table.userName'), dataIndex: 'userName', sorter: true },
      { title: t('system.member.table.roleName'), dataIndex: 'roleName', sorter: true },
      { title: t('system.member.table.createTime'), dataIndex: 'createTime', sorter: true },
      { title: t('system.member.table.modifyTime'), dataIndex: 'modifyTime', sorter: true },
    ],
    rowKey: 'id',
    pagination: true,
    useSearchForm: true,
    showTableSetting: false,
    showIndexColumn: false,
    canResize: false,
    actionColumn: {
      width: 120,
      title: t('component.table.operation'),
      dataIndex: 'action',
    },
    immediate: false,
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

  /* 删除成员 */
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
      close(undefined, { path: '/system/team' });
    } else {
      reload();
      const roleList = await getRoleListByPage({ page: 1, pageSize: 9999 });
      roleListOptions.value = roleList?.records;
    }
  });
</script>
