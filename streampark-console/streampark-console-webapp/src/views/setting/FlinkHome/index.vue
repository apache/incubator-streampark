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
<script lang="ts" setup name="FlinkEnvSetting">
  import { ref } from 'vue';
  import { useModal } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { SvgIcon } from '/@/components/Icon';
  import { Switch } from 'ant-design-vue';
  import { CheckOutlined, CloseOutlined, PlusOutlined } from '@ant-design/icons-vue';
  import { FlinkEnvModal, FlinkEnvDrawer } from './components';
  import {
    fetchValidity,
    fetchDefaultSet,
    fetchFlinkEnv,
    fetchFlinkEnvRemove,
    fetchFlinkInfo,
  } from '/@/api/flink/setting/flinkEnv';
  import { FlinkEnv } from '/@/api/flink/setting/types/flinkEnv.type';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useDrawer } from '/@/components/Drawer';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  defineOptions({
    name: 'FlinkEnvSetting',
  });

  const { t } = useI18n();
  const versionId = ref<string | null>(null);
  const { Swal, createMessage } = useMessage();
  const [registerModal, { openModal: openFlinkModal }] = useModal();
  const [registerFlinkDraw, { openDrawer: openEnvDrawer }] = useDrawer();
  const [registerTable, { reload, getDataSource }] = useTable({
    title: t('setting.flinkHome.title'),
    api: fetchFlinkEnv,
    columns: [
      { dataIndex: 'flinkName', title: t('setting.flinkHome.flinkName') },
      { dataIndex: 'flinkHome', title: t('setting.flinkHome.flinkHome') },
      { dataIndex: 'default', title: 'Default' },
      { dataIndex: 'description', title: t('setting.flinkHome.description') },
    ],
    useSearchForm: false,
    striped: false,
    canResize: false,
    bordered: false,
    showIndexColumn: false,
    pagination: false,
    actionColumn: {
      width: 200,
      title: t('component.table.operation'),
      dataIndex: 'action',
    },
  });

  /* Edit button */
  async function handleEditFlink(item: FlinkEnv) {
    const resp = await fetchValidity(item.id);
    if (resp.data.code == 200) {
      versionId.value = item.id;
      openFlinkModal(true, {
        versionId: item.id,
        flinkName: item.flinkName,
        flinkHome: item.flinkHome,
        description: item.description || null,
      });
    }
  }

  /* View configuration */
  async function handleFlinkConf(item: FlinkEnv) {
    const res = await fetchFlinkInfo(item.id);
    openEnvDrawer(true, res);
  }

  /* delete flink home */
  async function handleDelete(item: FlinkEnv) {
    const resp = await fetchFlinkEnvRemove(item.id);
    if (resp.data.code == 200) {
      reload();
      createMessage.success('The current flink home is removed.');
    }
  }

  /* set as default environment */
  async function handleSetDefault(item: FlinkEnv) {
    if (item.isDefault) {
      await fetchDefaultSet(item.id);
      Swal.fire({
        icon: 'success',
        title: item.flinkName.concat(' set default successful!'),
        showConfirmButton: false,
        timer: 2000,
      });
      reload();
    }
  }
</script>
<template>
  <PageWrapper contentFullHeight fixed-height content-class="flex flex-col">
    <BasicTable @register="registerTable" class="flex flex-col">
      <template #toolbar>
        <div v-auth="'project:create'">
          <a-button type="primary" class="w-full mt-10px" @click="openFlinkModal(true, {})">
            <PlusOutlined />
            {{ t('common.add') }}
          </a-button>
        </div>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'flinkName'">
          <svg-icon class="avatar" name="flink" :size="20" />
          {{ record.flinkName }}
        </template>
        <template v-if="column.dataIndex === 'default'">
          <Switch
            :disabled="record.isDefault"
            @click="handleSetDefault(record)"
            v-model:checked="record.isDefault"
          >
            <template #checkedChildren>
              <CheckOutlined />
            </template>
            <template #unCheckedChildren>
              <CloseOutlined />
            </template>
          </Switch>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                auth: 'project:build',
                tooltip: t('setting.flinkHome.edit'),
                onClick: handleEditFlink.bind(null, record),
              },
              {
                icon: 'ant-design:eye-outlined',
                auth: 'project:build',
                tooltip: t('setting.flinkHome.conf'),
                onClick: handleFlinkConf.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: t('common.delText'),
                disabled: record.isDefault && getDataSource()?.length > 1,
                popConfirm: {
                  title: t('setting.flinkHome.delete'),
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <FlinkEnvModal @register="registerModal" @reload="reload" />
    <FlinkEnvDrawer @register="registerFlinkDraw" width="60%" />
  </PageWrapper>
</template>
<style lang="less" scoped>
  .home-card-list {
    background-color: @component-background;
    height: 100%;
  }
</style>
