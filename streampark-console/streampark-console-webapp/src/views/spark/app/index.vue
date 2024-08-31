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
  import { nextTick, ref, onUnmounted, onMounted } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { AppStateEnum, OptionStateEnum, ReleaseStateEnum } from '/@/enums/flinkEnum';
  import { JobTypeEnum } from '/@/enums/sparkEnum';
  import { useTimeoutFn } from '@vueuse/core';
  import { Tooltip, Badge, Tag, Popover } from 'ant-design-vue';
  import { useTable } from '/@/components/Table';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { releaseTitleMap } from './data';

  import AppDashboard from './components/AppDashboard.vue';
  import State, {
    buildStatusMap,
    optionStateMap,
    releaseStateMap,
    stateMap,
  } from '/@/views/flink/app/components/State';
  import { useSparkColumns } from './hooks/useSparkColumns';
  import AppTableResize from '/@/views/flink/app/components/AppResize.vue';
  import { fetchSparkAppRecord } from '/@/api/spark/app';
  import { useSparkTableAction } from './hooks/useSparkTableAction';
  defineOptions({
    name: 'SparkApplication',
  });
  const { t } = useI18n();
  const optionApps = {
    starting: new Map(),
    stopping: new Map(),
    release: new Map(),
    savepointing: new Map(),
  };
  const errorCount = ref(0);
  const appDashboardRef = ref<any>();

  const currentTablePage = ref(1);
  const { onTableColumnResize, tableColumnWidth, getAppColumns } = useSparkColumns();
  const titleLenRef = ref({
    maxState: '',
    maxRelease: '',
    maxBuild: '',
  });

  const getSparkAppList = async (params: Recordable) => {
    try {
      if (Reflect.has(params, 'state')) {
        if (params.state && params.state.length > 0) {
          params['stateArray'] = [...params.state];
        }
        delete params.state;
      }
      currentTablePage.value = params.pageNum;
      // sessionStorage.setItem('appPageNo', params.pageNum);
      const res = await fetchSparkAppRecord(params);

      const timestamp = new Date().getTime();
      res.records.forEach((x) => {
        Object.assign(x, {
          expanded: [
            {
              appId: x.appId,
              jmMemory: x.jmMemory,
              tmMemory: x.tmMemory,
              totalTM: x.totalTM,
              totalSlot: x.totalSlot,
              availableSlot: x.availableSlot,
            },
          ],
        });
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
          if (optionApps.release.get(x.id)) {
            if (timestamp - optionApps.release.get(x.id) > 2000) {
              optionApps.release.delete(x.id);
            }
          }
          if (optionApps.savepointing.get(x.id)) {
            if (timestamp - optionApps.savepointing.get(x.id) > 2000) {
              optionApps.savepointing.delete(x.id);
            }
          }
        }
      });
      const stateLenMap = res.records.reduce(
        (
          prev: {
            maxState: string;
            maxRelease: string;
            maxBuild: string;
          },
          cur: any,
        ) => {
          const { state, optionState, release, buildStatus } = cur;
          // state title len
          if (optionState === OptionStateEnum.NONE) {
            const stateStr = stateMap[state]?.title;
            if (stateStr && stateStr.length > prev.maxState.length) {
              prev.maxState = stateStr;
            }
          } else {
            const stateStr = optionStateMap[optionState]?.title;
            if (stateStr && stateStr.length > prev.maxState.length) {
              prev.maxState = stateStr;
            }
          }

          //release title len
          const releaseStr = releaseStateMap[release]?.title;
          if (releaseStr && releaseStr.length > prev.maxRelease.length) {
            prev.maxRelease = releaseStr;
          }

          //build title len
          const buildStr = buildStatusMap[buildStatus]?.title;
          if (buildStr && buildStr.length > prev.maxBuild.length) {
            prev.maxBuild = buildStr;
          }
          return prev;
        },
        {
          maxState: '',
          maxRelease: '',
          maxBuild: '',
        },
      );
      Object.assign(titleLenRef.value, stateLenMap);

      return {
        list: res.records,
        total: res.total,
      };
    } catch (error) {
      errorCount.value += 1;
      console.error(error);
    }
  };
  const [registerTable, { reload, getLoading, setPagination }] = useTable({
    rowKey: 'id',
    api: getSparkAppList,
    immediate: true,
    canResize: false,
    showIndexColumn: false,
    showTableSetting: true,
    useSearchForm: true,
    tableSetting: { fullScreen: true, redo: false },
    actionColumn: {
      dataIndex: 'operation',
      title: t('component.table.operation'),
      width: 180,
    },
  });

  const { getTableActions, formConfig } = useSparkTableAction(handlePageDataReload);

  function handlePageDataReload(polling = false) {
    nextTick(() => {
      appDashboardRef.value?.handleDashboard(false);
      reload({ polling });
    });
  }
  const { start, stop } = useTimeoutFn(() => {
    if (errorCount.value <= 3) {
      start();
    } else {
      return;
    }
    if (!getLoading()) {
      handlePageDataReload(true);
    }
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
  <PageWrapper contentFullHeight fixed-height>
    <AppDashboard ref="appDashboardRef" />
    <BasicTable
      @register="registerTable"
      :columns="getAppColumns"
      @resize-column="onTableColumnResize"
      class="app_list !px-0 pt-20px flex flex-col"
      :formConfig="formConfig"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'jobName'">
          <span class="app_type app_jar" v-if="record['jobType'] == JobTypeEnum.JAR"> JAR </span>
          <span class="app_type app_sql" v-if="record['jobType'] == JobTypeEnum.SQL"> SQL </span>
          <span class="app_type app_py" v-if="record['jobType'] == JobTypeEnum.PYSPARK">
            PySpark
          </span>
          <span
            class="link"
            :class="{
              'cursor-pointer':
                [AppStateEnum.RESTARTING, AppStateEnum.RUNNING].includes(record.state) ||
                record['optionState'] === OptionStateEnum.SAVEPOINTING,
            }"
          >
            <Popover :title="t('common.detailText')">
              <template #content>
                <div class="flex">
                  <span class="pr-6px font-bold">{{ t('flink.app.appName') }}:</span>
                  <div class="max-w-300px break-words">{{ record.jobName }}</div>
                </div>
                <div class="pt-2px">
                  <span class="pr-6px font-bold">{{ t('flink.app.jobType') }}:</span>
                  <Tag color="blue">
                    <span v-if="record['jobType'] == JobTypeEnum.JAR"> JAR </span>
                    <span v-if="record['jobType'] == JobTypeEnum.SQL"> SQL </span>
                    <span v-if="record['jobType'] == JobTypeEnum.PYSPARK"> PySpark </span>
                  </Tag>
                </div>
                <div class="pt-2px flex">
                  <span class="pr-6px font-bold">{{ t('common.description') }}:</span>
                  <div class="max-w-300px break-words">{{ record.description }}</div>
                </div>
              </template>
              {{ record.jobName }}
            </Popover>
          </span>

          <template v-if="record['jobType'] == JobTypeEnum.JAR">
            <Badge
              v-if="record.release === ReleaseStateEnum.NEED_CHECK"
              class="build-badge"
              count="NEW"
              :title="t('flink.app.view.recheck')"
            />
            <Badge
              v-else-if="record.release >= ReleaseStateEnum.RELEASING"
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
              <Tag color="blue">{{ tag }}</Tag>
            </span>
          </Tooltip>
        </template>
        <template v-if="column.dataIndex === 'task'">
          <State option="task" :data="record" />
        </template>
        <template v-if="column.dataIndex === 'state'">
          <State option="state" :data="record" :maxTitle="titleLenRef.maxState" />
        </template>
        <template v-if="column.dataIndex === 'release'">
          <State
            option="release"
            :maxTitle="titleLenRef.maxRelease"
            :title="releaseTitleMap[record.release] || ''"
            :data="record"
          />
        </template>
        <template v-if="column.dataIndex === 'operation'">
          <TableAction v-bind="getTableActions(record, currentTablePage)" />
        </template>
      </template>
      <template #insertTable="{ tableContainer }">
        <AppTableResize
          :table-container="tableContainer"
          :resize-min="100"
          v-model:left="tableColumnWidth.jobName"
        />
      </template>
    </BasicTable>
  </PageWrapper>
</template>
<style lang="less">
  @import url('./styles/View.less');
</style>
