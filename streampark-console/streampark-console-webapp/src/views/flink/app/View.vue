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
  export default defineComponent({
    name: 'AppView',
  });
</script>
<script lang="ts" setup name="AppView">
  import { defineComponent, nextTick, ref, unref, onUnmounted, onMounted } from 'vue';
  import { useAppTableAction } from './hooks/useAppTableAction';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { AppStateEnum, JobTypeEnum, OptionStateEnum, LaunchStateEnum } from '/@/enums/flinkEnum';
  import { useTimeoutFn } from '@vueuse/core';
  import { Tooltip, Badge, Divider, Tag } from 'ant-design-vue';
  import { fetchAppRecord } from '/@/api/flink/app/app';
  import { useTable } from '/@/components/Table';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { AppListRecord } from '/@/api/flink/app/app.type';
  import { getAppColumns, launchTitleMap } from './data';
  import { handleView } from './utils';
  import { useDrawer } from '/@/components/Drawer';
  import { useModal } from '/@/components/Modal';

  import StartApplicationModal from './components/AppView/StartApplicationModal.vue';
  import StopApplicationModal from './components/AppView/StopApplicationModal.vue';
  import LogModal from './components/AppView/LogModal.vue';
  import BuildDrawer from './components/AppView/BuildDrawer.vue';
  import AppDashboard from './components/AppView/AppDashboard.vue';
  import State from './components/State';

  const { t } = useI18n();
  const optionApps = {
    starting: new Map(),
    stopping: new Map(),
    launch: new Map(),
  };

  const appDashboardRef = ref<any>();

  const yarn = ref<Nullable<string>>(null);
  const currentTablePage = ref(1);

  const [registerStartModal, { openModal: openStartModal }] = useModal();
  const [registerStopModal, { openModal: openStopModal }] = useModal();
  const [registerLogModal, { openModal: openLogModal }] = useModal();
  const [registerBuildDrawer, { openDrawer: openBuildDrawer }] = useDrawer();

  const [registerTable, { reload, getLoading, setPagination }] = useTable({
    rowKey: 'id',
    api: fetchAppRecord,
    beforeFetch: (params) => {
      if (Reflect.has(params, 'state')) {
        if (params.state && params.state.length > 0) {
          params['stateArray'] = [...params.state];
        }
        delete params.state;
      }
      currentTablePage.value = params.pageNum;
      // sessionStorage.setItem('appPageNo', params.pageNum);
      return params;
    },
    afterFetch: (dataSource) => {
      const timestamp = new Date().getTime();
      dataSource.forEach((x) => {
        x.expanded = [
          {
            appId: x.appId,
            jmMemory: x.jmMemory,
            tmMemory: x.tmMemory,
            totalTM: x.totalTM,
            totalSlot: x.totalSlot,
            availableSlot: x.availableSlot,
          },
        ];
        if (x['optionState'] === OptionStateEnum.NONE) {
          if (optionApps.starting.get(x.id)) {
            if (timestamp - optionApps.starting.get(x.id) > 2000 * 2) {
              optionApps.starting.delete(x.id);
            }
          }
          if (optionApps.stopping.get(x.id)) {
            if (timestamp - optionApps.stopping.get(x.id) > 2000) {
              optionApps.stopping.delete(x.id);
            }
          }
          if (optionApps.launch.get(x.id)) {
            if (timestamp - optionApps.launch.get(x.id) > 2000) {
              optionApps.launch.delete(x.id);
            }
          }
        }
      });
      return dataSource;
    },
    fetchSetting: { listField: 'records' },
    immediate: true,
    canResize: false,
    columns: getAppColumns(),
    showIndexColumn: false,
    showTableSetting: true,
    useSearchForm: true,
    tableSetting: { fullScreen: true, redo: false },
    actionColumn: { dataIndex: 'operation', title: t('component.table.operation'), width: 180 },
  });

  const { getTableActions, getActionDropdown, formConfig } = useAppTableAction(
    openStartModal,
    openStopModal,
    openLogModal,
    openBuildDrawer,
    handlePageDataReload,
    optionApps,
  );

  // build Detail
  function openBuildProgressDetailDrawer(app: AppListRecord) {
    openBuildDrawer(true, { appId: app.id });
  }

  /* view */
  async function handleJobView(app: AppListRecord) {
    // Task is running, restarting, in savePoint
    if (
      [AppStateEnum.RESTARTING, AppStateEnum.RUNNING].includes(app.state) ||
      app['optionState'] === OptionStateEnum.SAVEPOINTING
    ) {
      // yarn-per-job|yarn-session|yarn-application
      handleView(app, unref(yarn));
    }
  }

  /* Update options data */
  function handleOptionApp(data: {
    type: 'starting' | 'stopping' | 'launch';
    key: any;
    value: any;
  }) {
    optionApps[data.type].set(data.key, data.value);
  }

  function handlePageDataReload(polling = false) {
    nextTick(() => {
      appDashboardRef.value?.handleDashboard(false);
      reload({ polling });
    });
  }
  const { start, stop } = useTimeoutFn(() => {
    if (!getLoading()) {
      handlePageDataReload(true);
    }
    start();
  }, 2000);

  onMounted(() => {
    // If there is a page, jump to the page number of the record
    const currentPage = sessionStorage.getItem('appPageNo');
    if (currentPage) {
      setPagination({
        current: Number(currentPage) || 1,
      });
      sessionStorage.removeItem('appPageNo');
    }
  });

  onUnmounted(() => {
    stop();
  });
</script>
<template>
  <PageWrapper contentFullHeight>
    <AppDashboard ref="appDashboardRef" />
    <BasicTable @register="registerTable" class="app_list !px-0 pt-20px" :formConfig="formConfig">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'jobName'">
          <span class="app_type app_jar" v-if="record['jobType'] === JobTypeEnum.JAR"> JAR </span>
          <span class="app_type app_sql" v-if="record['jobType'] === JobTypeEnum.SQL"> SQL </span>

          <span
            class="link"
            :class="{
              'cursor-pointer':
                [AppStateEnum.RESTARTING, AppStateEnum.RUNNING].includes(record.state) ||
                record['optionState'] === OptionStateEnum.SAVEPOINTING,
            }"
            @click="handleJobView(record)"
          >
            <Tooltip :title="record.description"> {{ record.jobName }} </Tooltip>
          </span>

          <template v-if="record['jobType'] === JobTypeEnum.JAR">
            <Badge
              v-if="record.launch == LaunchStateEnum.NEED_CHECK"
              class="build-badge"
              count="NEW"
              :title="t('flink.app.view.recheck')"
            />
            <Badge
              v-else-if="record.launch >= LaunchStateEnum.LAUNCHING"
              class="build-badge"
              count="NEW"
              :title="t('flink.app.view.changed')"
            />
          </template>
        </template>
        <template v-if="column.dataIndex === 'tags'">
          <Tooltip v-if="record.tags" :title="record.tags">
            <span
              v-for="(tag, index) in record.tags.split(',')"
              :key="'tag-'.concat(index.toString())"
              class="pl-4px"
            >
              <Tag color="blue" class="app-tag">{{ tag }}</Tag>
            </span>
          </Tooltip>
        </template>
        <template v-if="column.dataIndex === 'task'">
          <State option="task" :data="record" />
        </template>
        <template v-if="column.dataIndex === 'state'">
          <State option="state" :data="record" />
        </template>
        <template v-if="column.dataIndex === 'launch'">
          <State option="launch" :title="launchTitleMap[record.launch] || ''" :data="record" />
          <Divider type="vertical" style="margin: 0 4px" v-if="record.buildStatus" />
          <State
            option="build"
            :click="openBuildProgressDetailDrawer.bind(null, record)"
            :data="record"
          />
        </template>
        <template v-if="column.dataIndex === 'operation'">
          <TableAction
            :actions="getTableActions(record, currentTablePage)"
            :dropDownActions="getActionDropdown(record)"
          />
        </template>
      </template>
    </BasicTable>
    <StartApplicationModal @register="registerStartModal" @update-option="handleOptionApp" />
    <StopApplicationModal @register="registerStopModal" @update-option="handleOptionApp" />
    <LogModal @register="registerLogModal" />
    <BuildDrawer @register="registerBuildDrawer" />
  </PageWrapper>
</template>
<style lang="less">
  @import url('./styles/View.less');
</style>
