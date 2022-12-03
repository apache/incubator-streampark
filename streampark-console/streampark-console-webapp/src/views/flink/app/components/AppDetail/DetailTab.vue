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
<script lang="ts">
  export default {
    name: 'DetailTab',
  };
</script>
<script setup lang="ts" name="DetailTab">
  import { ref, toRefs, watch } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useDrawer } from '/@/components/Drawer';
  import Mergely from '../Mergely.vue';
  import { Tabs, Descriptions, Tag } from 'ant-design-vue';

  import { useMonaco } from '/@/hooks/web/useMonaco';
  import { BasicTable, useTable, TableAction, ActionItem } from '/@/components/Table';
  import Icon from '/@/components/Icon';
  import {
    getBackupColumns,
    getConfColumns,
    getOptionLogColumns,
    getSavePointColumns,
  } from '../../data/detail.data';
  import { getMonacoOptions } from '../../data';
  import { handleView } from '../../utils';
  import { useRoute } from 'vue-router';
  import { fetchGetVer, fetchListVer, fetchRemoveConf } from '/@/api/flink/config';
  import { fetchRemoveSavePoint, fetchSavePonitHistory } from '/@/api/flink/app/savepoint';

  import { fetchBackUps, fetchOptionLog } from '/@/api/flink/app/app';
  import { decodeByBase64 } from '/@/utils/cipher';
  import { useModal } from '/@/components/Modal';
  import CompareModal from './CompareModal.vue';
  import ExecOptionModal from './ExecOptionModal.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useClipboard } from '@vueuse/core';
  import { AppTypeEnum, JobTypeEnum, SavePointEnum } from '/@/enums/flinkEnum';

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
      type: Object as PropType<Recordable>,
      default: () => ({}),
    },
    tabConf: {
      type: Object as PropType<Recordable<boolean>>,
      default: () => ({}),
    },
  });
  const { app, tabConf } = toRefs(props);
  const flinkSql = ref();

  const { setContent } = useMonaco(flinkSql, {
    language: 'sql',
    options: { minimap: { enabled: true }, ...(getMonacoOptions(true) as any) },
  });

  watch(
    () => props.app.flinkSql,
    (val) => {
      if (!val) return;
      setContent(decodeByBase64(props.app.flinkSql));
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
  const [registerExecOption, { openModal: openExecOptionModal }] = useModal();
  const [registerDetailDrawer, { openDrawer: openDetailDrawer }] = useDrawer();

  const [registerConfigTable, { getDataSource, reload: reloadConf }] = useTable({
    api: fetchListVer,
    columns: getConfColumns(),
    ...tableCommonConf,
  });

  const [registerSavePointTable, { reload: reloadSavePoint }] = useTable({
    api: fetchSavePonitHistory,
    columns: getSavePointColumns(),
    ...tableCommonConf,
  });

  const [registerBackupTable] = useTable({
    api: fetchBackUps,
    columns: getBackupColumns(),
    ...tableCommonConf,
  });

  const [registerLogsTable] = useTable({
    api: fetchOptionLog,
    columns: getOptionLogColumns(),
    ...tableCommonConf,
  });

  function getConfAction(record: Recordable): ActionItem[] {
    return [
      {
        tooltip: { title: t('flink.app.detail.detailTab.configDetail') },
        type: 'link',
        icon: 'ant-design:eye-outlined',
        onClick: handleConfDetail.bind(null, record),
      },
      {
        tooltip: { title: t('flink.app.detail.compareConfig') },
        type: 'link',
        icon: 'ant-design:swap-outlined',
        onClick: handleCompare.bind(null, record),
        ifShow: getDataSource().length > 1,
      },
      {
        popConfirm: {
          title: t('flink.app.detail.detailTab.confDeleteTitle'),
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
  async function handleConfDetail(record: Recordable) {
    const hide = createMessage.loading('loading');
    try {
      const res = await fetchGetVer({
        id: record.id,
      });
      openDetailDrawer(true, {
        configOverride: decodeByBase64(res.content),
      });
    } catch (error: unknown) {
      console.error(error);
    } finally {
      hide();
    }
  }

  /* delete configuration */
  async function handleDeleteConf(record: Recordable) {
    await fetchRemoveConf({ id: record.id });
    reloadConf();
  }

  function handleCompare(record: Recordable) {
    openCompareModal(true, {
      id: record.id,
      version: record.version,
      createTime: record.createTime,
    });
  }

  function getSavePointAction(record: Recordable): ActionItem[] {
    return [
      {
        tooltip: { title: t('flink.app.detail.detailTab.copyPath') },
        shape: 'circle',
        icon: 'ant-design:copy-outlined',
        onClick: handleCopy.bind(null, record),
      },
      {
        popConfirm: {
          title: t('flink.app.detail.detailTab.pointDeleteTitle'),
          confirm: handleDeleteSavePoint.bind(null, record),
        },
        shape: 'circle',
        icon: 'ant-design:delete-outlined',
        type: 'danger' as any,
      },
    ];
  }
  /* delete savePoint */
  async function handleDeleteSavePoint(record: Recordable) {
    await fetchRemoveSavePoint({ id: record.id });
    reloadSavePoint();
  }

  /* copy path */
  function handleCopy(record: Recordable) {
    try {
      copy(record.path);
      createMessage.success(t('flink.app.detail.detailTab.copySuccess'));
    } catch (error) {
      console.error(error);
      createMessage.error(t('flink.app.detail.detailTab.copyFail'));
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
    <Tabs :defaultActiveKey="1" class="mt-15px" :animated="false" :tab-bar-gutter="0">
      <TabPane key="1" tab="Option" force-render>
        <Descriptions bordered size="middle" layout="vertical">
          <DescriptionItem v-for="(v, k) in JSON.parse(app.options || '{}')" :key="k" :label="k">
            {{ v }}
          </DescriptionItem>
        </Descriptions>
      </TabPane>
      <TabPane
        key="2"
        tab="Configuration"
        v-if="app && app.appType == AppTypeEnum.STREAMPARK_FLINK && tabConf.showConf"
      >
        <BasicTable @register="registerConfigTable">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex == 'format'">
              <Tag color="#2db7f5" v-if="record.format == 1"> yaml </Tag>
              <Tag color="#108ee9" v-if="record.format == 2"> properties </Tag>
            </template>
            <template v-if="column.dataIndex == 'version'">
              <a-button type="primary" shape="circle" size="small" class="mr-10px">
                {{ record.version }}
              </a-button>
            </template>
            <template v-if="column.dataIndex == 'effective'">
              <Tag color="green" v-if="record.effective"> Effective </Tag>
              <Tag color="cyan" v-if="record.latest"> Latest </Tag>
            </template>
            <template v-if="column.dataIndex == 'createTime'">
              <Icon icon="ant-design:clock-circle-outlined" />
              {{ record.createTime }}
            </template>
            <template v-if="column.dataIndex == 'operation'">
              <TableAction :actions="getConfAction(record)" />
            </template>
          </template>
        </BasicTable>
      </TabPane>
      <TabPane key="3" tab="Flink SQL" v-if="app.jobType === JobTypeEnum.SQL">
        <div class="sql-box syntax-true" ref="flinkSql" style="height: 600px"></div>
      </TabPane>
      <TabPane key="4" tab="Savepoints" v-if="tabConf.showSaveOption">
        <BasicTable @register="registerSavePointTable">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex == 'triggerTime'">
              <Icon icon="ant-design:clock-circle-outlined" />
              {{ record.triggerTime }}
            </template>
            <template v-if="column.dataIndex == 'type'">
              <div class="app_state">
                <Tag color="#0C7EF2" v-if="record['type'] === SavePointEnum.CHECK_POINT">
                  {{ t('flink.app.detail.detailTab.check') }}
                </Tag>
                <Tag color="#52c41a" v-if="record['type'] === SavePointEnum.SAVE_POINT">
                  {{ t('flink.app.detail.detailTab.save') }}
                </Tag>
              </div>
            </template>
            <template v-if="column.dataIndex == 'latest'">
              <Tag color="green" v-if="record.latest"> Latest </Tag>
            </template>
            <template v-if="column.dataIndex == 'operation'">
              <TableAction :actions="getSavePointAction(record)" />
            </template>
          </template>
        </BasicTable>
      </TabPane>
      <TabPane key="5" tab="Backups" v-if="tabConf.showBackup">
        <BasicTable @register="registerBackupTable">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex == 'version'">
              <a-button type="primary" shape="circle" size="small">
                {{ record.version }}
              </a-button>
            </template>
          </template>
        </BasicTable>
      </TabPane>
      <TabPane key="6" tab="Option Logs" v-if="tabConf.showOptionLog">
        <BasicTable @register="registerLogsTable">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex == 'yarnAppId'">
              <a-button type="link" @click="handleView(app as any, '')">
                {{ record.yarnAppId }}
              </a-button>
            </template>
            <template v-if="column.dataIndex == 'jobManagerUrl'">
              <a-button type="link" :href="record.jobManagerUrl" target="_blank">
                {{ record.jobManagerUrl }}
              </a-button>
            </template>
            <template v-if="column.dataIndex == 'optionTime'">
              <Icon icon="ant-design:clock-circle-outlined" />
              {{ record.optionTime }}
            </template>
            <template v-if="column.dataIndex == 'success'">
              <Tag class="start-state" color="#52c41a" v-if="record.success"> SUCCESS </Tag>
              <Tag class="start-state" color="#f5222d" v-else> FAILED </Tag>
            </template>
            <template v-if="column.dataIndex == 'operation'">
              <TableAction
                :actions="[
                  {
                    tooltip: { title: t('flink.app.detail.detailTab.exception') },
                    auth: 'app:detail',
                    shape: 'circle',
                    type: 'default',
                    ifShow: !record.success,
                    icon: 'ant-design:eye-outlined',
                    onClick: handleException.bind(null, record),
                  },
                ]"
              />
            </template>
          </template>
        </BasicTable>
      </TabPane>
    </Tabs>

    <CompareModal @register="registerCompare" />
    <ExecOptionModal @register="registerExecOption" />
    <Mergely :read-only="true" @register="registerDetailDrawer" />
  </div>
</template>
