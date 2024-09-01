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
    <BasicTable @register="registerTable" class="flex flex-col">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate" v-auth="'team:add'">
          <Icon icon="ant-design:plus-outlined" />
          {{ t('common.add') }}
        </a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                auth: 'team:update',
                tooltip: t('system.team.modifyTeam'),
                onClick: handleTeamEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: t('system.team.deleteTeam'),
                auth: 'team:delete',
                popConfirm: {
                  title: t('system.team.deletePopConfirm'),
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <TeamDrawer okText="Submit" @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';

  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import TeamDrawer from './TeamDrawer.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { columns, searchFormSchema } from './team.data';
  import { fetTeamList, fetchTeamDelete } from '/@/api/system/team';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import Icon from '/@/components/Icon';
  import { PageWrapper } from '/@/components/Page';
  export default defineComponent({
    name: 'Team',
    components: { BasicTable, TeamDrawer, TableAction, Icon, PageWrapper },
    setup() {
      const [registerDrawer, { openDrawer }] = useDrawer();
      const { createMessage } = useMessage();
      const { t } = useI18n();
      const [registerTable, { reload }] = useTable({
        title: t('system.team.table.title'),
        api: fetTeamList,
        columns,
        formConfig: {
          baseColProps: { style: { paddingRight: '30px' } },
          schemas: searchFormSchema,
          fieldMapToTime: [['createTime', ['createTimeFrom', 'createTimeTo'], 'YYYY-MM-DD']],
        },
        rowKey: 'id',
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
      });

      function handleCreate() {
        openDrawer(true, {
          isUpdate: false,
        });
      }

      function handleTeamEdit(record: Recordable) {
        openDrawer(true, {
          record,
          isUpdate: true,
        });
      }

      /* Delete the organization */
      async function handleDelete(record: Recordable) {
        const { data } = await fetchTeamDelete({ id: record.id });
        if (data.status === 'success') {
          createMessage.success(`${t('system.team.deleteTeam')} ${t('system.team.success')}`);
          reload();
        } else {
          createMessage.error(`${t('system.team.deleteTeam')} ${t('system.team.fail')}`);
        }
      }

      function handleSuccess(isUpdate: boolean) {
        createMessage.success(
          `${isUpdate ? t('common.edit') : t('system.team.add')} ${t('system.team.success')}`,
        );
        reload();
      }

      function onChange(val) {
        console.log(val);
      }
      return {
        t,
        registerTable,
        registerDrawer,
        handleCreate,
        handleTeamEdit,
        handleDelete,
        handleSuccess,
        onChange,
      };
    },
  });
</script>
