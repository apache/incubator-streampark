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
<script lang="ts" setup name="FlinkClusterSetting">
  import { nextTick, onUnmounted } from 'vue';
  import { useTimeoutFn } from '@vueuse/core';
  import { SvgIcon } from '/@/components/Icon';
  import { Col, Tag } from 'ant-design-vue';
  import { ClusterStateEnum, ExecModeEnum } from '/@/enums/flinkEnum';
  import { PlusOutlined } from '@ant-design/icons-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    fetchClusterRemove,
    fetchClusterShutdown,
    fetchClusterStart,
    pageFlinkCluster,
  } from '/@/api/flink/setting/flinkCluster';
  import { FlinkCluster } from '/@/api/flink/setting/types/flinkCluster.type';
  import { useGo } from '/@/hooks/web/usePage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  defineOptions({
    name: 'FlinkClusterSetting',
  });
  const executionModeMap = {
    [ExecModeEnum.STANDALONE]: {
      color: '#2db7f5',
      text: 'standalone',
    },
    [ExecModeEnum.YARN_SESSION]: {
      color: '#87d068',
      text: 'yarn session',
    },
    [ExecModeEnum.KUBERNETES_SESSION]: {
      color: '#108ee9',
      text: 'k8s session',
    },
  };

  const go = useGo();
  const { t } = useI18n();
  const { Swal, createMessage } = useMessage();
  const [registerTable, { reload, getLoading }] = useTable({
    api: pageFlinkCluster,
    columns: [
      { dataIndex: 'clusterName', title: t('setting.flinkCluster.form.clusterName') },
      { dataIndex: 'executionMode', title: t('setting.flinkCluster.form.executionMode') },
      { dataIndex: 'address', title: t('setting.flinkCluster.form.address') },
      { dataIndex: 'description', title: t('setting.flinkHome.description') },
    ],
    formConfig: {
      schemas: [
        {
          field: 'clusterName',
          label: '',
          component: 'Input',
          componentProps: {
            placeholder: t('setting.flinkCluster.searchByName'),
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
  function handleIsStart(item) {
    return item.clusterState === ClusterStateEnum.STARTED;
  }

  /* Go to edit cluster */
  function handleEditCluster(item: FlinkCluster) {
    go(`/setting/edit_cluster?clusterId=${item.id}`);
  }
  /* deploy */
  async function handleDeployCluster(item: FlinkCluster) {
    const hide = createMessage.loading(
      t('setting.flinkCluster.operateMessage.flinkClusterIsStarting'),
      0,
    );
    try {
      await fetchClusterStart(item.id);
      await Swal.fire({
        icon: 'success',
        title: t('setting.flinkCluster.operateMessage.flinkClusterHasStartedSuccessful'),
        showConfirmButton: false,
        timer: 2000,
      });
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }
  /* delete */
  async function handleDelete(item: FlinkCluster) {
    const hide = createMessage.loading('Deleting', 0);
    try {
      await fetchClusterRemove(item.id);
      reload();
      createMessage.success('The current cluster is remove');
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }
  /* shutdown */
  async function handleShutdownCluster(item: FlinkCluster) {
    const hide = createMessage.loading('The current cluster is canceling', 0);
    try {
      await fetchClusterShutdown(item.id);
      createMessage.success('The current cluster is shutdown');
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }

  function handlePageDataReload(polling = false) {
    nextTick(() => {
      reload({ polling });
    });
  }
  const { start, stop } = useTimeoutFn(() => {
    // Prevent another request from being initiated while the previous request is pending
    if (!getLoading()) {
      handlePageDataReload(true);
    }
    start();
  }, 1000 * 3);

  onUnmounted(() => {
    stop();
  });
</script>
<template>
  <PageWrapper contentFullHeight fixed-height content-class="flex flex-col">
    <BasicTable @register="registerTable" class="flex flex-col">
      <template #form-formFooter>
        <Col :span="5" :offset="13" class="text-right">
          <a-button type="primary" @click="() => go('/setting/add_cluster')">
            <PlusOutlined />
            {{ t('common.add') }}
          </a-button>
        </Col>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'clusterName'">
          <svg-icon class="avatar" name="flink" :size="20" />
          {{ record.clusterName }}
        </template>
        <template v-if="column.dataIndex === 'executionMode'">
          <Tag
            v-if="executionModeMap[record.executionMode]"
            :color="executionModeMap[record.executionMode]?.color"
          >
            {{ executionModeMap[record.executionMode]?.text }}
          </Tag>
        </template>
        <template v-if="column.dataIndex === 'address'">
          <a
            :href="`/proxy/cluster/${record.id}/`"
            target="_blank"
            v-if="
              record.executionMode === ExecModeEnum.STANDALONE ||
              record.executionMode === ExecModeEnum.YARN_SESSION
            "
          >
            {{ record.address }}
          </a>
          <span v-else> - </span>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                auth: 'cluster:update',
                tooltip: t('setting.flinkCluster.edit'),
                disabled: handleIsStart(record),
                onClick: handleEditCluster.bind(null, record),
              },
              {
                icon: 'ant-design:pause-circle-outlined',
                auth: 'cluster:create',
                ifShow: handleIsStart(record),
                disabled: record.executionMode === ExecModeEnum.STANDALONE,
                tooltip: t('setting.flinkCluster.stop'),
                onClick: handleShutdownCluster.bind(null, record),
              },
              {
                icon: 'ant-design:play-circle-outlined',
                auth: 'cluster:create',
                ifShow: !handleIsStart(record),
                disabled: record.executionMode === ExecModeEnum.STANDALONE,
                tooltip: t('setting.flinkCluster.start'),
                onClick: handleDeployCluster.bind(null, record),
              },
              {
                icon: 'ant-design:eye-outlined',
                auth: 'app:detail',
                disabled: !handleIsStart(record),
                tooltip: t('setting.flinkCluster.detail'),
                href: `/proxy/cluster/${record.id}/`,
                target: '_blank',
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: t('common.delText'),
                popConfirm: {
                  title: t('setting.flinkCluster.delete'),
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
  </PageWrapper>
</template>
<style lang="less" scoped>
  .cluster-card-list {
    background-color: @component-background;
    height: 100%;
  }
</style>
