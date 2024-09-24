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
<script lang="ts" setup>
  import { useI18n } from '/@/hooks/web/useI18n';
  import { ref } from 'vue';
  import { useModal } from '/@/components/Modal';
  import { SvgIcon } from '/@/components/Icon';
  import { Col, Switch } from 'ant-design-vue';
  import { CheckOutlined, CloseOutlined, PlusOutlined } from '@ant-design/icons-vue';
  import { SparkEnvModal } from './components';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { PageWrapper } from '/@/components/Page';
  import { fetchSetDefault, fetchSparkEnvList, fetchSparkEnvRemove } from '/@/api/spark/home';
  import { SparkEnv } from '/@/api/spark/home.type';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  // import { useDrawer } from '/@/components/Drawer';
  // import SparkEnvDrawer from './components/Drawer.vue';
  defineOptions({
    name: 'SparkEnvSetting',
  });

  const { t } = useI18n();
  const versionId = ref<string | null>(null);
  const { Swal, createMessage } = useMessage();
  const [registerModal, { openModal: openSparkModal }] = useModal();
  // const [registerSparkDraw, { openDrawer: openEnvDrawer }] = useDrawer();
  const [registerTable, { reload, getDataSource }] = useTable({
    rowKey: 'id',
    api: fetchSparkEnvList,
    columns: [
      { dataIndex: 'sparkName', title: t('spark.home.form.sparkName') },
      { dataIndex: 'sparkHome', title: t('spark.home.form.sparkHome') },
      { dataIndex: 'version', title: t('spark.home.sparkVersion') },
      { dataIndex: 'default', title: 'Default' },
      { dataIndex: 'description', title: t('spark.home.form.description') },
    ],
    formConfig: {
      schemas: [
        {
          field: 'sparkName',
          label: '',
          component: 'Input',
          componentProps: {
            placeholder: t('spark.home.searchByName'),
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
    },
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

  /* Edit button */
  async function handleEditSpark(item: SparkEnv) {
    versionId.value = item.id;
    openSparkModal(true, {
      versionId: item.id,
      sparkName: item.sparkName,
      sparkHome: item.sparkHome,
      description: item.description || null,
    });
  }

  /* delete spark home */
  async function handleDelete(item: SparkEnv) {
    try {
      await fetchSparkEnvRemove(item.id);
      reload();
      createMessage.success(t('spark.home.tips.remove'));
    } catch (error) {
      console.error(error);
    }
  }

  /* View configuration */
  // async function handleSparkConf(item: SparkEnv) {
  //   openEnvDrawer(true, item);
  // }

  /* set as default environment */
  async function handleSetDefault(item: SparkEnv) {
    if (item.isDefault) {
      await fetchSetDefault(item.id);
      Swal.fire({
        icon: 'success',
        title: item.sparkName.concat(t('spark.home.tips.setDefault')),
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
      <template #form-formFooter>
        <Col :span="5" :offset="13" class="text-right">
          <a-button type="primary" @click="openSparkModal(true, {})">
            <PlusOutlined />
            {{ t('common.add') }}
          </a-button>
        </Col>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'sparkName'">
          <svg-icon class="avatar" name="spark" :size="20" />
          {{ record.sparkName }}
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
                tooltip: t('spark.home.edit'),
                onClick: handleEditSpark.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: t('common.delText'),
                disabled: record.isDefault && getDataSource()?.length > 1,
                popConfirm: {
                  title: t('spark.home.delete'),
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <SparkEnvModal @register="registerModal" @reload="reload" />
    <!-- <SparkEnvDrawer @register="registerSparkDraw" /> -->
  </PageWrapper>
</template>
<style lang="less"></style>
