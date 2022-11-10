/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { handleIsStart } from '../utils';
import { useFlinkAppStore } from '/@/store/modules/flinkApplication';
import { useFlinkApplication } from './useApp';
import { fetchAppRecord, fetchAppRemove } from '/@/api/flink/app/app';
import { AppListRecord } from '/@/api/flink/app/app.type';
import { fetchFlamegraph } from '/@/api/flink/app/metrics';
import { ActionItem, FormProps } from '/@/components/Table';
import { useMessage } from '/@/hooks/web/useMessage';
import { ExecModeEnum } from '/@/enums/flinkEnum';
import { usePermission } from '/@/hooks/web/usePermission';
import { useI18n } from '/@/hooks/web/useI18n';
export enum JobTypeEnum {
  JAR = 1,
  SQL = 2,
}

// Create form configurations and operation functions in the application table
export const useAppTableAction = (
  openStartModal: Fn,
  openStopModal: Fn,
  openLogModal: Fn,
  openBuildDrawer: Fn,
  handlePageDataReload: Fn,
  optionApps: Recordable,
) => {
  const { t } = useI18n();
  const tagsOptions = ref<Recordable>([]);

  const flinkAppStore = useFlinkAppStore();
  const router = useRouter();
  const { createMessage } = useMessage();
  const { hasPermission } = usePermission();
  const {
    handleCheckLaunchApp,
    handleAppCheckStart,
    handleCanStop,
    handleForcedStop,
    handleCopy,
    handleMapping,
    users,
  } = useFlinkApplication(openStartModal);

  /* Operation button */
  function getTableActions(record: AppListRecord, currentPgaeNo: any): ActionItem[] {
    return [
      {
        tooltip: { title: t('flink.app.tableAction.edit') },
        auth: 'app:update',
        icon: 'clarity:note-edit-line',
        onClick: handleEdit.bind(null, record, currentPgaeNo),
      },
      {
        tooltip: { title: t('flink.app.tableAction.launch') },
        ifShow: [-1, 1, 4].includes(record.launch) && record['optionState'] === 0,
        icon: 'ant-design:cloud-upload-outlined',
        onClick: handleCheckLaunchApp.bind(null, record),
      },
      {
        tooltip: { title: t('flink.app.tableAction.launchDetail') },
        ifShow: [-1, 2].includes(record.launch) || record['optionState'] === 1,
        icon: 'ant-design:container-outlined',
        onClick: () => openBuildDrawer(true, { appId: record.id }),
      },
      {
        tooltip: { title: t('flink.app.tableAction.start') },
        ifShow: handleIsStart(record, optionApps),
        auth: 'app:start',
        icon: 'ant-design:play-circle-outlined',
        onClick: handleAppCheckStart.bind(null, record),
      },
      {
        tooltip: { title: t('flink.app.tableAction.cancel') },
        ifShow: record.state === 5 && record['optionState'] === 0,
        auth: 'app:cancel',
        icon: 'ant-design:pause-circle-outlined',
        onClick: handleCancel.bind(null, record),
      },
      {
        tooltip: { title: t('flink.app.tableAction.detail') },
        auth: 'app:detail',
        icon: 'ant-design:eye-outlined',
        onClick: handleDetail.bind(null, record),
      },
      {
        tooltip: { title: t('flink.app.tableAction.startLog') },
        ifShow: [ExecModeEnum.KUBERNETES_SESSION, ExecModeEnum.KUBERNETES_APPLICATION].includes(
          record.executionMode,
        ),
        auth: 'app:detail',
        icon: 'ant-design:sync-outlined',
        onClick: () => openLogModal(true, { app: record }),
      },
      {
        tooltip: { title: t('flink.app.tableAction.force') },
        ifShow: handleCanStop(record),
        auth: 'app:cancel',
        icon: 'ant-design:pause-circle-outlined',
        onClick: handleForcedStop.bind(null, record),
      },
    ];
  }
  /* Click to edit */
  function handleEdit(app: AppListRecord, currentPageNo: number) {
    // Record the current page number
    sessionStorage.setItem('appPageNo', String(currentPageNo || 1));
    flinkAppStore.setApplicationId(app.id);
    if (app.appType === 1) {
      // jobType( 1 custom code 2: flinkSQL)
      router.push({ path: '/flink/app/edit_streampark', query: { appId: app.id } });
    } else if (app.appType === 2) {
      //Apache Flink
      router.push({ path: '/flink/app/edit_flink', query: { appId: app.id } });
    }
  }

  /* Click for details */
  function handleDetail(app: AppListRecord) {
    flinkAppStore.setApplicationId(app.id);
    router.push({ path: '/flink/app/detail', query: { appId: app.id } });
  }
  // click stop application
  function handleCancel(app: AppListRecord) {
    if (!optionApps.stopping.get(app.id) || app['optionState'] === 0) {
      openStopModal(true, { application: app });
    }
  }
  /* pull down button */
  function getActionDropdown(record: AppListRecord): ActionItem[] {
    return [
      {
        label: t('flink.app.tableAction.copy'),
        auth: 'app:copy',
        icon: 'ant-design:copy-outlined',
        onClick: handleCopy.bind(null, record),
      },
      {
        label: t('flink.app.tableAction.remapping'),
        ifShow: [0, 7, 10, 11, 13].includes(record.state),
        auth: 'app:mapping',
        icon: 'ant-design:deployment-unit-outlined',
        onClick: handleMapping.bind(null, record),
      },
      {
        label: t('flink.app.tableAction.flameGraph'),
        ifShow: record.flameGraph,
        auth: 'app:flameGraph',
        icon: 'ant-design:fire-outlined',
        onClick: handleFlameGraph.bind(null, record),
      },
      {
        popConfirm: {
          title: t('flink.app.tableAction.deleteTip'),
          confirm: handleDelete.bind(null, record),
        },
        label: t('common.delText'),
        ifShow: [0, 7, 9, 10, 13, 18, 19].includes(record.state),
        auth: 'app:delete',
        icon: 'ant-design:delete-outlined',
        color: 'error',
      },
    ];
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

  /* Click to delete */
  async function handleDelete(app: AppListRecord) {
    const hide = createMessage.loading('deleting', 0);
    try {
      await fetchAppRemove(app.id);
      createMessage.success('delete successful');
      handlePageDataReload(false);
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }

  const formConfig = computed((): Partial<FormProps> => {
    const tableFormConfig: FormProps = {
      baseColProps: { span: 5, style: { paddingRight: '20px' } },
      actionColOptions: { span: 4 },
      showSubmitButton: false,
      showResetButton: false,
      async resetFunc() {
        router.push({ path: '/flink/app/add' });
      },
      schemas: [
        {
          label: 'Tags',
          field: 'tags',
          component: 'Select',
          componentProps: {
            placeholder: 'Tags',
            showSearch: true,
            options: tagsOptions.value.map((t: Recordable) => ({ label: t, value: t })),
            onChange: handlePageDataReload.bind(null, false),
          },
        },
        {
          label: 'Owner',
          field: 'userId',
          component: 'Select',
          componentProps: {
            placeholder: 'Owner',
            showSearch: true,
            options: users.value.map((u: Recordable) => {
              return { label: u.nickName || u.username, value: u.userId };
            }),
            onChange: handlePageDataReload.bind(null, false),
          },
        },
        {
          label: 'Type',
          field: 'jobType',
          component: 'Select',
          componentProps: {
            placeholder: 'Type',
            showSearch: true,
            options: [
              { label: 'JAR', value: JobTypeEnum.JAR },
              { label: 'SQL', value: JobTypeEnum.SQL },
            ],
            onChange: handlePageDataReload.bind(null, false),
          },
        },
        {
          label: 'Name',
          field: 'jobName',
          component: 'InputSearch',
          componentProps: {
            placeholder: 'Search',
            onChange: handlePageDataReload.bind(null, false),
            onSearch: handlePageDataReload.bind(null, false),
          },
        },
      ],
    };
    if (hasPermission('app:create')) {
      Object.assign(tableFormConfig, {
        showResetButton: true,
        resetButtonOptions: {
          text: t('common.add'),
          color: 'primary',
          preIcon: 'ant-design:plus-outlined',
        },
      });
    }
    return tableFormConfig;
  });

  /*  tag */
  function handleInitTagsOptions() {
    const params = Object.assign({}, { pageSize: 999999999, pageNum: 1 });
    fetchAppRecord(params).then((res) => {
      const dataSource = res?.records || [];
      dataSource.forEach((record) => {
        if (record.tags !== null && record.tags !== undefined && record.tags !== '') {
          const tagsArray = record.tags.split(',') as string[];
          tagsArray.forEach((x: string) => {
            if (x.length > 0 && tagsOptions.value.indexOf(x) == -1) {
              tagsOptions.value.push(x);
            }
          });
        }
      });
    });
  }
  onMounted(() => {
    handleInitTagsOptions();
  });
  return { getTableActions, getActionDropdown, formConfig };
};
