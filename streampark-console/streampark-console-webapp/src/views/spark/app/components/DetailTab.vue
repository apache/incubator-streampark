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
<script setup lang="ts" name="DetailTab">
  import { ref, toRefs, watch } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useDrawer } from '/@/components/Drawer';
  import Mergely from './Mergely.vue';
  import { Tabs, Descriptions, Tag } from 'ant-design-vue';

  import { useMonaco } from '/@/hooks/web/useMonaco';
  import { BasicTable, useTable, TableAction, ActionItem } from '/@/components/Table';
  import Icon from '/@/components/Icon';
  import {
    getBackupColumns,
    getConfColumns,
    getSparkSqlColumns,
    getOptionLogColumns,
  } from '../data/detail.data';
  import { getMonacoOptions } from '../data';
  import { useRoute } from 'vue-router';
  import { fetchGetSparkConf, fetchSparkConfList, fetchSparkConfRemove } from '/@/api/spark/conf';
  import {
    fetchSparkBackUps,
    fetchSparkDeleteOptLog,
    fetchSparkOptionLog,
    fetchSparkRemoveBackup,
  } from '/@/api/spark/app';
  import { decodeByBase64 } from '/@/utils/cipher';
  import { useModal } from '/@/components/Modal';
  import CompareModal from './CompareModal.vue';
  import ExecOptionModal from '/@/views/flink/app/components/AppDetail/ExecOptionModal.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useClipboard } from '@vueuse/core';
  import { CandidateTypeEnum, ConfigTypeEnum } from '/@/enums/flinkEnum';
  import { OperationEnum, JobTypeEnum } from '/@/enums/sparkEnum';
  import SparkSqlReview from './SparkSqlReview.vue';
  import SparkSqlCompareModal from './SparkSqlCompareModal.vue';
  import { fetchSparkSql, fetchSparkSqlList, fetchSparkSqlRemove } from '/@/api/spark/sql';
  import { SparkApplication } from '/@/api/spark/app.type';
  import { baseUrl } from '/@/api';
  const DescriptionItem = Descriptions.Item;
  const TabPane = Tabs.TabPane;

  const { createMessage } = useMessage();
  const { t } = useI18n();
  const { copy } = useClipboard({
    legacy: true,
  });
  const route = useRoute();
  const props = defineProps({
    app: {
      type: Object as PropType<SparkApplication>,
      default: () => ({}),
    },
    tabConf: {
      type: Object as PropType<Recordable<boolean>>,
      default: () => ({}),
    },
  });
  const { app, tabConf } = toRefs(props);
  const sparkSql = ref();

  const { setContent } = useMonaco(sparkSql, {
    language: 'sql',
    options: { minimap: { enabled: true }, ...(getMonacoOptions(true) as any) },
  });

  watch(
    () => props.app.sparkSql,
    (val) => {
      if (!val) return;
      setContent(decodeByBase64(props.app.sparkSql || ''));
    },
  );

  const tableCommonConf = {
    beforeFetch: (params) => Object.assign(params, { appId: route.query.appId }),
    fetchSetting: { listField: 'records', pageField: 'pageNum', sizeField: 'pageSize' },
    rowKey: 'id',
    canResize: false,
    showIndexColumn: false,
    showTableSetting: false,
    actionColumn: {
      title: t('component.table.operation'),
      dataIndex: 'operation',
      key: 'operation',
      width: 150,
    },
  };

  const [registerCompare, { openModal: openCompareModal }] = useModal();
  const [registerSparkSqlCompare, { openModal: openSparkSqlCompareModal }] = useModal();
  const [registerExecOption, { openModal: openExecOptionModal }] = useModal();
  const [registerDetailDrawer, { openDrawer: openDetailDrawer }] = useDrawer();
  const [registerSparkSqlDrawer, { openDrawer: openSparkDrawer }] = useDrawer();

  const [registerConfigTable, { getDataSource, reload: reloadConf }] = useTable({
    api: fetchSparkConfList,
    columns: getConfColumns(),
    ...tableCommonConf,
  });

  const [registerSparkSqlTable, { getDataSource: getSparkSql, reload: reloadSparkSql }] = useTable({
    api: fetchSparkSqlList,
    columns: getSparkSqlColumns(),
    ...tableCommonConf,
  });

  const [registerBackupTable, { reload: reloadBackup }] = useTable({
    api: fetchSparkBackUps,
    columns: getBackupColumns(),
    ...tableCommonConf,
  });

  const [registerLogsTable, { reload: reloadOperationLog }] = useTable({
    api: fetchSparkOptionLog,
    columns: getOptionLogColumns(),
    ...tableCommonConf,
  });

  function getConfAction(record: Recordable): ActionItem[] {
    return [
      {
        tooltip: { title: t('spark.app.detail.detailTab.configDetail') },
        type: 'link',
        icon: 'ant-design:eye-outlined',
        onClick: handleConfDetail.bind(null, record),
      },
      {
        tooltip: { title: t('spark.app.detail.compareConfig') },
        type: 'link',
        icon: 'ant-design:swap-outlined',
        onClick: handleCompare.bind(null, record),
        ifShow: getDataSource().length > 1,
      },
      {
        popConfirm: {
          title: t('spark.app.detail.detailTab.confDeleteTitle'),
          confirm: handleDeleteConf.bind(null, record),
        },
        auth: 'conf:delete',
        type: 'link',
        icon: 'ant-design:delete-outlined',
        color: 'error',
        ifShow: !record.effective,
      },
    ];
  }

  function getSparkSqlAction(record: Recordable): ActionItem[] {
    return [
      {
        tooltip: { title: t('spark.app.detail.detailTab.sqlDetail') },
        type: 'link',
        icon: 'ant-design:eye-outlined',
        onClick: handleSqlDetail.bind(null, record),
      },
      {
        tooltip: { title: t('spark.app.detail.compareSparkSql') },
        type: 'link',
        icon: 'ant-design:swap-outlined',
        onClick: handleCompareSparkSql.bind(null, record),
        ifShow: getSparkSql().length > 1,
      },
      {
        popConfirm: {
          title: t('spark.app.detail.detailTab.sqlDeleteTitle'),
          confirm: handleDeleteSparkSql.bind(null, record),
        },
        auth: 'conf:delete',
        type: 'link',
        icon: 'ant-design:delete-outlined',
        color: 'error',
        ifShow: !record.effective,
      },
    ];
  }

  async function handleConfDetail(record: Recordable) {
    const hide = createMessage.loading('loading');
    try {
      const res = await fetchGetSparkConf({
        id: record.id,
      });
      openDetailDrawer(true, {
        configOverride: decodeByBase64(res.content || ''),
      });
    } catch (error: unknown) {
      console.error(error);
    } finally {
      hide();
    }
  }

  async function handleSqlDetail(record: Recordable) {
    const hide = createMessage.loading('loading');
    try {
      const res = await fetchSparkSql({
        id: record.id,
        appId: record.appId,
      });
      openSparkDrawer(true, {
        sql: decodeByBase64(res.sql),
      });
    } catch (error: unknown) {
      console.error(error);
    } finally {
      hide();
    }
  }

  /* delete configuration */
  async function handleDeleteConf(record: Recordable) {
    await fetchSparkConfRemove({ id: record.id });
    await reloadConf();
  }

  /* delete spark sql */
  async function handleDeleteSparkSql(record: Recordable) {
    await fetchSparkSqlRemove({ id: record.id, appId: record.appId });
    await reloadSparkSql();
  }

  function handleCompare(record: Recordable) {
    openCompareModal(true, {
      id: record.id,
      version: record.version,
      createTime: record.createTime,
    });
  }

  function handleCompareSparkSql(record: Recordable) {
    openSparkSqlCompareModal(true, {
      id: record.id,
      version: record.version,
      createTime: record.createTime,
    });
  }

  async function handleYarnUrl(id: string) {
    window.open(baseUrl() + '/proxy/yarn/' + id + '/');
  }

  async function handleViewHistory(id: string) {
    window.open(baseUrl() + '/proxy/history/' + id + '/');
  }

  function getBackupAction(record: Recordable): ActionItem[] {
    return [
      {
        tooltip: { title: t('spark.app.detail.detailTab.copyPath') },
        shape: 'circle',
        icon: 'ant-design:copy-outlined',
        onClick: handleCopy.bind(null, record),
      },
      {
        popConfirm: {
          title: t('spark.app.detail.detailTab.confBackupTitle'),
          confirm: handleDeleteBackup.bind(null, record),
        },
        auth: 'app:delete',
        type: 'link',
        icon: 'ant-design:delete-outlined',
        color: 'error',
      },
    ];
  }

  function getOperationLogAction(record: Recordable): ActionItem[] {
    return [
      {
        tooltip: { title: t('spark.app.detail.detailTab.exception') },
        auth: 'app:detail',
        shape: 'circle',
        type: 'default',
        ifShow: !record.success,
        icon: 'ant-design:eye-outlined',
        onClick: handleException.bind(null, record),
      },
      {
        popConfirm: {
          title: t('spark.app.detail.detailTab.operationLogDeleteTitle'),
          confirm: handleDeleteOperationLog.bind(null, record),
        },
        auth: 'app:delete',
        type: 'link',
        icon: 'ant-design:delete-outlined',
        color: 'error',
      },
    ];
  }

  async function handleDeleteBackup(record: Recordable) {
    await fetchSparkRemoveBackup(record.id);
    await reloadBackup();
  }

  async function handleDeleteOperationLog(record: Recordable) {
    await fetchSparkDeleteOptLog(record.id);
    await reloadOperationLog();
  }

  /* copy path */
  function handleCopy(record: Recordable) {
    try {
      copy(record.path);
      createMessage.success(t('spark.app.detail.detailTab.copySuccess'));
    } catch (error) {
      console.error(error);
      createMessage.error(t('spark.app.detail.detailTab.copyFail'));
    }
  }

  function handleException(record: Recordable) {
    openExecOptionModal(true, {
      content: record.exception,
    });
  }
</script>
<template>
  <div>
    <Tabs :defaultActiveKey="1" class="mt-15px" :tab-bar-gutter="0">
      <TabPane key="1" :tab="t('spark.app.detail.detailTab.detailTabName.operationLog')">
        <BasicTable @register="registerLogsTable">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'optionName'">
              <Tag color="blue" v-if="record.optionName === OperationEnum.RELEASE"> Release </Tag>
              <Tag color="green" v-if="record.optionName === OperationEnum.START"> Start </Tag>
              <Tag color="orange" v-if="record.optionName === OperationEnum.STOP"> STOP </Tag>
            </template>
            <template v-if="column.dataIndex === 'clusterId'">
              <a type="link" @click="handleYarnUrl(record.clusterId)" target="_blank">
                {{ record.clusterId }}
              </a>
            </template>
            <template v-if="column.dataIndex === 'trackingUrl'">
              <a type="link" @click="handleViewHistory(record.id)" target="_blank">
                {{ record.trackingUrl }}
              </a>
            </template>
            <template v-if="column.dataIndex === 'optionTime'">
              <Icon icon="ant-design:clock-circle-outlined" />
              {{ record.optionTime }}
            </template>
            <template v-if="column.dataIndex === 'success'">
              <Tag class="bold-tag" color="#52c41a" v-if="record.success"> SUCCESS </Tag>
              <Tag class="bold-tag" color="#f5222d" v-else> FAILED </Tag>
            </template>
            <template v-if="column.dataIndex === 'operation'">
              <TableAction :actions="getOperationLogAction(record)" />
            </template>
          </template>
        </BasicTable>
      </TabPane>
      <TabPane
        key="2"
        :tab="t('spark.app.detail.detailTab.detailTabName.option')"
        v-if="app.options"
      >
        <Descriptions bordered size="middle" layout="vertical">
          <DescriptionItem v-for="(v, k) in JSON.parse(app.options || '{}')" :key="k" :label="k">
            {{ v }}
          </DescriptionItem>
        </Descriptions>
      </TabPane>
      <TabPane
        key="3"
        :tab="t('spark.app.detail.detailTab.detailTabName.configuration')"
        v-if="app && tabConf.showConf"
      >
        <BasicTable @register="registerConfigTable">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'format'">
              <Tag class="bold-tag" color="#2db7f5" v-if="record.format === ConfigTypeEnum.YAML">
                yaml
              </Tag>
              <Tag
                class="bold-tag"
                color="#108ee9"
                v-if="record.format === ConfigTypeEnum.PROPERTIES"
              >
                properties
              </Tag>
            </template>
            <template v-if="column.dataIndex === 'version'">
              <a-button type="primary" shape="circle" size="small" class="mr-10px">
                {{ record.version }}
              </a-button>
            </template>
            <template v-if="column.dataIndex === 'effective'">
              <Tag color="green" v-if="record.effective"> Effective </Tag>
              <Tag color="cyan" v-if="record.latest"> Latest </Tag>
            </template>
            <template v-if="column.dataIndex === 'createTime'">
              <Icon icon="ant-design:clock-circle-outlined" />
              {{ record.createTime }}
            </template>
            <template v-if="column.dataIndex === 'operation'">
              <TableAction :actions="getConfAction(record)" />
            </template>
          </template>
        </BasicTable>
      </TabPane>
      <TabPane
        key="4"
        :tab="t('spark.app.detail.detailTab.detailTabName.sparkSql')"
        v-if="app.jobType === JobTypeEnum.SQL"
      >
        <BasicTable @register="registerSparkSqlTable">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'candidate'">
              <Tag
                class="bold-tag"
                color="#52c41a"
                v-if="record.candidate === CandidateTypeEnum.NONE"
              >
                None
              </Tag>
              <Tag
                class="bold-tag"
                color="#2db7f5"
                v-if="record.candidate === CandidateTypeEnum.NEW"
              >
                New
              </Tag>
              <Tag
                class="bold-tag"
                color="#108ee9"
                v-if="record.candidate === CandidateTypeEnum.HISTORY"
              >
                History
              </Tag>
            </template>
            <template v-if="column.dataIndex === 'version'">
              <a-button type="primary" shape="circle" size="small" class="mr-10px">
                {{ record.version }}
              </a-button>
            </template>
            <template v-if="column.dataIndex === 'effective'">
              <Tag color="green" v-if="record.effective"> Effective </Tag>
            </template>
            <template v-if="column.dataIndex === 'createTime'">
              <Icon icon="ant-design:clock-circle-outlined" />
              {{ record.createTime }}
            </template>
            <template v-if="column.dataIndex === 'operation'">
              <TableAction :actions="getSparkSqlAction(record)" />
            </template>
          </template>
        </BasicTable>
      </TabPane>
      <TabPane
        key="6"
        :tab="t('spark.app.detail.detailTab.detailTabName.backup')"
        v-if="tabConf.showBackup"
      >
        <BasicTable @register="registerBackupTable">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'version'">
              <a-button type="primary" shape="circle" size="small">
                {{ record.version }}
              </a-button>
            </template>
            <template v-if="column.dataIndex === 'operation'">
              <TableAction :actions="getBackupAction(record)" />
            </template>
          </template>
        </BasicTable>
      </TabPane>
    </Tabs>

    <CompareModal @register="registerCompare" />
    <SparkSqlCompareModal @register="registerSparkSqlCompare" />
    <ExecOptionModal @register="registerExecOption" />
    <Mergely :read-only="true" @register="registerDetailDrawer" />
    <SparkSqlReview @register="registerSparkSqlDrawer" />
  </div>
</template>
