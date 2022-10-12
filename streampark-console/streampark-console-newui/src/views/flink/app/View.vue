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
  import { defineComponent, onMounted, ref, reactive, unref, onUnmounted } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';

  export default defineComponent({
    name: 'AppView',
  });
</script>
<script lang="ts" setup name="AppView">
  import { useTimeoutFn } from '@vueuse/core';
  import { Row, Col, Tooltip, Badge, Divider, Select, Input, Tag } from 'ant-design-vue';
  import { fetchAppRecord, fetchDashboard, fetchAppRemove } from '/@/api/flink/app/app';
  import { fetchFlamegraph } from '/@/api/flink/app/metrics';
  import { ActionItem, useTable } from '/@/components/Table';
  import { Icon } from '/@/components/Icon';
  import { useFlinkApplication } from './hooks/useApp';
  import { useRouter } from 'vue-router';
  import { useFlinkAppStore } from '/@/store/modules/flinkApplication';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { AppListRecord } from '/@/api/flink/app/app.type';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getAppColumns, launchTitleMap } from './data';
  import { handleIsStart, handleView } from './utils';
  import { useDrawer } from '/@/components/Drawer';
  import { useModal } from '/@/components/Modal';
  import StatisticCard from './components/AppView/statisticCard.vue';
  import StartApplicationModal from './components/AppView/StartApplicationModal.vue';
  import StopApplicationModal from './components/AppView/StopApplicationModal.vue';
  import LogModal from './components/AppView/LogModal.vue';
  import BuildDrawer from './components/AppView/BuildDrawer.vue';
  import State from './components/State';

  const SelectOption = Select.Option;
  const InputGroup = Input.Group;
  const InputSearch = Input.Search;

  const { t } = useI18n();
  const router = useRouter();
  const flinkAppStore = useFlinkAppStore();
  const { createMessage } = useMessage();

  const optionApps = {
    starting: new Map(),
    stopping: new Map(),
    launch: new Map(),
  };

  const tagsOptions = ref<Recordable>([]);
  const dashboardLoading = ref(true);
  const dashBigScreenMap = reactive<Recordable>({});
  const searchText = ref('');
  const tags = ref(undefined);
  const jobType = ref(undefined);
  const userId = ref(undefined);
  const yarn = ref<Nullable<string>>(null);

  // Get Dashboard Metrics Data
  async function handleDashboard() {
    try {
      const res = await fetchDashboard();
      if (res) {
        Object.assign(dashBigScreenMap, {
          availiableTask: {
            staticstics: { title: 'Available Task Slots', value: res.availableSlot },
            footer: [
              { title: 'Task Slots', value: res.totalSlot },
              { title: 'Task Managers', value: res.totalTM },
            ],
          },
          runningJob: {
            staticstics: { title: 'Running Jobs', value: res.runningJob },
            footer: [
              { title: 'Total Task', value: res.task.total },
              { title: 'Running Task', value: res.task.running },
            ],
          },
          jobManager: {
            staticstics: { title: 'JobManager Memory', value: res.jmMemory },
            footer: [{ title: 'Total JobManager Mem', value: `${res.jmMemory} MB` }],
          },
          taskManager: {
            staticstics: { title: 'TaskManager Memory', value: res.tmMemory },
            footer: [{ title: 'Total TaskManager Mem', value: `${res.tmMemory} MB` }],
          },
        });
      }
    } catch (error) {
      console.error(error);
    } finally {
      dashboardLoading.value = false;
    }
  }
  const [registerStartModal, { openModal: openStartModal }] = useModal();
  const [registerStopModal, { openModal: openStopModal }] = useModal();
  const [registerLogModal, { openModal: openLogModal }] = useModal();
  const [registerBuildDrawer, { openDrawer: openBuildDrawer }] = useDrawer();

  const [registerTable, { reload, getLoading }] = useTable({
    rowKey: 'id',
    api: fetchAppRecord,
    beforeFetch: (params) => {
      if (Reflect.has(params, 'state')) {
        if (params.state && params.state.length > 0) {
          params['stateArray'] = [...params.state];
        }
        delete params.state;
      }
      Object.assign(params, {
        jobName: searchText.value,
        jobType: jobType.value,
        userId: userId.value,
        tags: tags.value,
      });
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
        if (x['optionState'] === 0) {
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
    tableSetting: { fullScreen: true },
    actionColumn: {
      dataIndex: 'operation',
      title: 'Operation',
      width: 200,
    },
  });

  // build Detail
  function openBuildProgressDetailDrawer(app: AppListRecord) {
    openBuildDrawer(true, { appId: app.id });
  }

  /* Click to add */
  function handleAdd() {
    router.push({ path: '/flink/app/add' });
  }
  /* Click to edit */
  function handleEdit(app: AppListRecord) {
    flinkAppStore.setApplicationId(app.id);
    if (app.appType === 1) {
      // jobType( 1 custom code 2: flinkSQL)
      router.push({ path: '/flink/app/edit_streampark', query: { appId: app.id } });
    } else if (app.appType === 2) {
      //Apache Flink
      router.push({ path: '/flink/app/edit_flink' });
    }
  }
  /* Click for details */
  function handleDetail(app: AppListRecord) {
    flinkAppStore.setApplicationId(app.id);
    router.push({ path: '/flink/app/detail', query: { appId: app.id } });
  }

  /* view */
  async function handleJobView(app: AppListRecord) {
    // Task is running, restarting, in savePoint
    if ([4, 5].includes(app.state) || app['optionState'] === 4) {
      // yarn-per-job|yarn-session|yarn-application
      handleView(app, unref(yarn));
    }
  }
  /* Click to delete */
  async function handleDelete(app: AppListRecord) {
    const hide = createMessage.loading('deleting', 0);
    try {
      await fetchAppRemove(app.id);
      createMessage.success('delete successful');
      reload();
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }

  async function handleFlameGraph(app: AppListRecord) {
    const hide = createMessage.loading('flameGraph generating...', 0);
    try {
      const { data } = await fetchFlamegraph({
        appId: app.id,
        width: document.documentElement.offsetWidth || document.body.offsetWidth,
      });
      if (data != null) {
        const blob = new Blob([data], { type: 'image/svg+xml' });
        const imageUrl = (window.URL || window.webkitURL).createObjectURL(blob);
        window.open(imageUrl);
      }
    } catch (error) {
      console.error(error);
      createMessage.error('flameGraph generate failed');
    } finally {
      hide();
    }
  }

  function handleCancel(app: AppListRecord) {
    if (!optionApps.stopping.get(app.id) || app['optionState'] === 0) {
      openStopModal(true, {
        application: app,
      });
    }
  }
  /* log view */
  function handleSeeLog(app: AppListRecord) {
    openLogModal(true, { app: unref(app) });
  }
  const {
    handleCheckLaunchApp,
    handleAppCheckStart,
    handleCanStop,
    handleForcedStop,
    handleCopy,
    handleMapping,
    users,
  } = useFlinkApplication(openStartModal);

  /*  tag */
  async function handleInitTagsOptions() {
    const params = Object.assign(
      {},
      {
        pageSize: 999999999,
        pageNum: 1,
      },
    );
    const res = await fetchAppRecord(params);
    const dataSource = res.records;
    dataSource.forEach((record) => {
      if (record.tags !== null && record.tags !== undefined && record.tags !== '') {
        const tagsArray = record.tags.split(',');
        tagsArray.forEach((x) => {
          if (x.length > 0 && tagsOptions.value.indexOf(x) == -1) {
            tagsOptions.value.push(x);
          }
        });
      }
    });
  }

  /* Operation button */
  function getTableActions(record: AppListRecord): ActionItem[] {
    return [
      {
        tooltip: { title: 'Edit Application' },
        auth: 'app:update',
        icon: 'ant-design:edit-outlined',
        onClick: handleEdit.bind(null, record),
        type: 'default',
        shape: 'circle',
      },
      {
        tooltip: { title: 'Launch Application' },
        ifShow: [-1, 1, 4].includes(record.launch) && record['optionState'] === 0,
        icon: 'ant-design:cloud-upload-outlined',
        onClick: handleCheckLaunchApp.bind(null, record),
        type: 'default',
        shape: 'circle',
      },
      {
        tooltip: { title: 'Launching Progress Detail' },
        ifShow: [-1, 2].includes(record.launch) || record['optionState'] === 1,
        auth: 'app:update',
        icon: 'ant-design:container-outlined',
        onClick: openBuildProgressDetailDrawer.bind(null, record),
        type: 'default',
        shape: 'circle',
      },
      {
        tooltip: { title: 'Start Application' },
        ifShow: handleIsStart(record, optionApps),
        auth: 'app:start',
        icon: 'ant-design:play-circle-outlined',
        onClick: handleAppCheckStart.bind(null, record),
        type: 'default',
        shape: 'circle',
      },
      {
        tooltip: { title: 'Cancel Application' },
        ifShow: record.state === 5 && record['optionState'] === 0,
        auth: 'app:cancel',
        icon: 'ant-design:pause-circle-outlined',
        onClick: handleCancel.bind(null, record),
        type: 'default',
        shape: 'circle',
      },
      {
        tooltip: { title: 'View Application Detail' },
        auth: 'app:detail',
        icon: 'ant-design:eye-outlined',
        onClick: handleDetail.bind(null, record),
        type: 'default',
        shape: 'circle',
      },
      {
        tooltip: { title: 'See Flink Start log' },
        ifShow: [5, 6].includes(record.executionMode),
        auth: 'app:detail',
        icon: 'ant-design:sync-outlined',
        onClick: handleSeeLog.bind(null, record),
        type: 'default',
        shape: 'circle',
      },
      {
        tooltip: { title: 'Forced Stop Application' },
        ifShow: handleCanStop(record),
        auth: 'app:cancel',
        icon: 'ant-design:pause-circle-outlined',
        onClick: handleForcedStop.bind(null, record),
        type: 'default',
        shape: 'circle',
      },
    ];
  }

  /* pull down button */
  function getActionDropdown(record: AppListRecord): ActionItem[] {
    return [
      {
        label: 'Copy Application',
        auth: 'app:copy',
        icon: 'ant-design:copy-outlined',
        onClick: handleCopy.bind(null, record),
        shape: 'circle',
      },
      {
        label: 'Remapping Application',
        ifShow: [0, 7, 10, 11, 13].includes(record.state),
        auth: 'app:mapping',
        icon: 'ant-design:deployment-unit-outlined',
        onClick: handleMapping.bind(null, record),
        shape: 'circle',
      },
      {
        label: 'View FlameGraph',
        ifShow: record.flameGraph,
        auth: 'app:flameGraph',
        icon: 'ant-design:fire-outlined',
        onClick: handleFlameGraph.bind(null, record),
        shape: 'circle',
      },
      {
        popConfirm: {
          title: 'Are you sure delete this job ?',
          confirm: handleDelete.bind(null, record),
        },
        label: 'Delete',
        ifShow: [0, 7, 9, 10, 13, 18, 19].includes(record.state),
        auth: 'app:delete',
        icon: 'ant-design:delete-outlined',
        shape: 'circle',
        color: 'error',
      },
    ];
  }
  /* Update options data */
  function handleOptionApp(data: {
    type: 'starting' | 'stopping' | 'launch';
    key: any;
    value: any;
  }) {
    optionApps[data.type].set(data.key, data.value);
  }

  const { start, stop } = useTimeoutFn(() => {
    handleDashboard();
    if (!getLoading()) {
      reload({ polling: true });
    }
    start();
  }, 2000);

  onUnmounted(() => {
    stop();
  });

  onMounted(async () => {
    handleDashboard();
    handleInitTagsOptions();
  });
</script>
<template>
  <PageWrapper contentFullHeight>
    <Row :gutter="24" class="dashboard">
      <Col
        class="gutter-row mt-10px"
        :md="6"
        :xs="24"
        v-for="(value, key) in dashBigScreenMap"
        :key="key"
      >
        <StatisticCard
          :statisticProps="value.staticstics"
          :footerList="value.footer"
          :loading="dashboardLoading"
        />
      </Col>
    </Row>
    <BasicTable @register="registerTable" class="app_list !px-0 mt-20px">
      <template #headerTop>
        <div class="text-right my-15px">
          <InputGroup compact>
            <Select
              placeholder="Tags"
              show-search
              allowClear
              v-model:value="tags"
              class="!ml-16px !w-150px text-left"
            >
              <SelectOption v-for="tag in tagsOptions" :key="tag">{{ tag }}</SelectOption>
            </Select>
            <Select
              placeholder="Owner"
              allowClear
              v-model:value="userId"
              class="!ml-16px !w-120px text-left"
            >
              <SelectOption v-for="u in users" :key="u.userId">
                <span v-if="u.nickName"> {{ u.nickName }} </span>
                <span v-else> {{ u.username }} </span>
              </SelectOption>
            </Select>
            <Select
              placeholder="Type"
              allowClear
              v-model:value="jobType"
              class="!ml-16px w-80px text-left"
            >
              <SelectOption value="1">JAR</SelectOption>
              <SelectOption value="2">SQL</SelectOption>
            </Select>
            <InputSearch
              placeholder="Search..."
              v-model:value="searchText"
              class="!w-250px text-left"
            />
            <a-button type="primary" style="margin-left: 20px" @click="handleAdd">
              <Icon icon="ant-design:plus-outlined" />
              {{ t('common.add') }}
            </a-button>
          </InputGroup>
        </div>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'jobName'">
          <span class="app_type app_jar" v-if="record['jobType'] === 1"> JAR </span>
          <span class="app_type app_sql" v-if="record['jobType'] === 2"> SQL </span>

          <span
            class="link"
            :class="{
              pointer: [4, 5].includes(record.state) || record['optionState'] === 4,
            }"
            @click="handleJobView(record)"
          >
            <Tooltip :title="record.description"> {{ record.jobName }} </Tooltip>
          </span>

          <template v-if="record['jobType'] === 1">
            <Badge
              v-if="record.launch === 5"
              class="build-badge"
              count="NEW"
              title="the associated project has changed and this job need to be rechecked"
            />
            <Badge
              v-else-if="record.launch >= 2"
              class="build-badge"
              count="NEW"
              title="the application has changed."
            />
          </template>
        </template>
        <template v-if="column.dataIndex === 'tags'">
          <Tooltip v-if="record.tags" :title="record.tags">
            <span
              v-for="(tag, index) in record.tags.split(',')"
              :key="'tag-'.concat(index.toString())"
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
          <State option="launch" :title="launchTitleMap.get(record.launch)" :data="record" />
          <Divider type="vertical" style="margin: 0 4px" v-if="record.buildStatus != null" />
          <State
            option="build"
            :click="openBuildProgressDetailDrawer.bind(null, record)"
            :data="record"
          />
        </template>
        <template v-if="column.dataIndex === 'operation'">
          <TableAction
            :actions="getTableActions(record)"
            :dropDownActions="getActionDropdown(record)"
          >
            <template #more>
              <a-button type="default" shape="circle" size="small" class="ml-6px">
                <Icon icon="ant-design:more-outlined" class="icon-more" />
              </a-button>
            </template>
          </TableAction>
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
