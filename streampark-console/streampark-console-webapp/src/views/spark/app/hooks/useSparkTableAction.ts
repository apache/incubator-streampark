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
import { ActionItem, FormProps } from '/@/components/Table';
import { useMessage } from '/@/hooks/web/useMessage';
import { AppStateEnum, JobTypeEnum, OptionStateEnum } from '/@/enums/sparkEnum';
import { usePermission } from '/@/hooks/web/usePermission';
import { useI18n } from '/@/hooks/web/useI18n';
import { isFunction, isNullAndUnDef, isObject } from '/@/utils/is';
import { fetchSparkAppCancel, fetchSparkAppRecord, fetchSparkAppRemove } from '/@/api/spark/app';
import type { SparkApplication } from '/@/api/spark/app.type';
import { useSparkAction } from './useSparkAction';
import { useDrawer } from '/@/components/Drawer';
import { ReleaseStateEnum } from '/@/enums/flinkEnum';

// Create form configurations and operation functions in the application table
export const useSparkTableAction = (handlePageDataReload: Fn, optionApps: Recordable) => {
  const { t } = useI18n();
  const tagsOptions = ref<Recordable>([]);

  const router = useRouter();
  const { createMessage, Swal } = useMessage();
  const { hasPermission } = usePermission();
  // const [registerStartModal, { openModal: openStartModal }] = useModal();
  // const [registerStopModal, { openModal: openStopModal }] = useModal();
  // const [registerLogModal, { openModal: openLogModal }] = useModal();
  const [registerBuildDrawer, { openDrawer: openBuildDrawer }] = useDrawer();
  const {
    handleCheckReleaseApp,
    handleAppCheckStart,
    handleCanStop,
    handleAbort,
    handleCopy,
    handleMapping,
    users,
  } = useSparkAction(optionApps);

  /* Operation button list */
  function getActionList(record: SparkApplication, currentPageNo: number): ActionItem[] {
    return [
      {
        tooltip: { title: t('spark.app.operation.edit') },
        auth: 'app:update',
        icon: 'clarity:note-edit-line',
        onClick: handleEdit.bind(null, record, currentPageNo),
      },
      {
        tooltip: { title: t('spark.app.operation.release') },
        ifShow:
          !isNullAndUnDef(record.release) &&
          [
            ReleaseStateEnum.FAILED,
            ReleaseStateEnum.NEED_RELEASE,
            ReleaseStateEnum.NEED_ROLLBACK,
          ].includes(record.release) &&
          record['optionState'] == OptionStateEnum.NONE,
        auth: 'app:release',
        icon: 'ant-design:cloud-upload-outlined',
        onClick: handleCheckReleaseApp.bind(null, record),
      },
      {
        tooltip: { title: t('spark.app.operation.releaseDetail') },
        ifShow:
          (!isNullAndUnDef(record.release) &&
            [ReleaseStateEnum.FAILED, ReleaseStateEnum.RELEASING].includes(record.release)) ||
          record['optionState'] == OptionStateEnum.RELEASING,
        auth: 'app:release',
        icon: 'ant-design:container-outlined',
        onClick: () => openBuildDrawer(true, { appId: record.id }),
      },
      {
        tooltip: { title: t('spark.app.operation.start') },
        ifShow: handleIsStart(record, optionApps),
        auth: 'app:start',
        icon: 'ant-design:play-circle-outlined',
        onClick: handleAppCheckStart.bind(null, record),
      },
      {
        tooltip: { title: t('spark.app.operation.cancel') },
        ifShow:
          (record.state == AppStateEnum.ACCEPTED ||
            record.state == AppStateEnum.RUNNING ||
            record.state == AppStateEnum.SUBMITTED) &&
          record['optionState'] == OptionStateEnum.NONE,
        auth: 'app:cancel',
        icon: 'ant-design:pause-circle-outlined',
        popConfirm: {
          title: t('spark.app.operation.cancel'),
          placement: 'left',
          confirm: handleCancel.bind(null, record),
        },
      },
      {
        tooltip: { title: t('spark.app.operation.detail') },
        auth: 'app:detail',
        icon: 'carbon:data-view-alt',
        onClick: handleDetail.bind(null, record),
      },
      // {
      //   tooltip: { title: t('spark.app.operation.startLog') },
      //   auth: 'app:detail',
      //   icon: 'ant-design:code-outlined',
      //   onClick: () => openLogModal(true, { app: record }),
      // },
      {
        tooltip: { title: t('spark.app.operation.abort') },
        ifShow: handleCanStop(record),
        auth: 'app:cancel',
        icon: 'ant-design:pause-circle-outlined',
        onClick: handleAbort.bind(null, record),
      },
      {
        label: t('spark.app.operation.copy'),
        auth: 'app:copy',
        icon: 'ant-design:copy-outlined',
        onClick: handleCopy.bind(null, record),
      },
      {
        label: t('spark.app.operation.remapping'),
        ifShow: [
          AppStateEnum.ADDED,
          AppStateEnum.FAILED,
          AppStateEnum.STOPPING,
          AppStateEnum.KILLED,
          AppStateEnum.SUCCEEDED,
          AppStateEnum.FINISHED,
          AppStateEnum.LOST,
        ].includes(record.state as AppStateEnum),
        auth: 'app:mapping',
        icon: 'ant-design:deployment-unit-outlined',
        onClick: handleMapping.bind(null, record),
      },
      {
        popConfirm: {
          title: t('spark.app.operation.deleteTip'),
          confirm: handleDelete.bind(null, record),
        },
        label: t('common.delText'),
        ifShow:
          !isNullAndUnDef(record.state) &&
          [
            AppStateEnum.ADDED,
            AppStateEnum.FAILED,
            AppStateEnum.FINISHED,
            AppStateEnum.LOST,
            AppStateEnum.SUCCEEDED,
            AppStateEnum.KILLED,
          ].includes(record.state),
        auth: 'app:delete',
        icon: 'ant-design:delete-outlined',
        color: 'error',
      },
    ];
  }
  /** action button is show */
  function actionIsShow(tableActionItem: ActionItem): boolean | undefined {
    const { auth, ifShow } = tableActionItem;
    let flag = isFunction(ifShow) ? ifShow(tableActionItem) : ifShow;
    /** Judgment auth when not set or allowed to display */
    if ((flag || flag === undefined) && auth) flag = hasPermission(auth);
    return flag;
  }

  function getTableActions(
    record: SparkApplication,
    currentPageNo: any,
  ): { actions: ActionItem[]; dropDownActions: ActionItem[] } {
    const tableAction = getActionList(record, currentPageNo).filter((item: ActionItem) =>
      actionIsShow(item),
    );
    const actions = tableAction.splice(0, 3).map((item: ActionItem) => {
      if (item.label) {
        item.tooltip = {
          title: item.label,
        };
        delete item.label;
      }
      return item;
    });
    return {
      actions,
      dropDownActions: tableAction.map((item: ActionItem) => {
        if (!item.label && isObject(item.tooltip)) item.label = item.tooltip?.title || '';
        return item;
      }),
    };
  }

  /* Click to edit */
  function handleEdit(app: SparkApplication, currentPageNo: number) {
    // Record the current page number
    sessionStorage.setItem('sparkAppPageNo', String(currentPageNo || 1));
    router.push({ path: '/spark/app/edit', query: { appId: app.id } });
  }

  /* Click for details */
  function handleDetail(app: SparkApplication) {
    router.push({ path: '/spark/app/detail', query: { appId: app.id } });
  }

  // click stop application
  async function handleCancel(app: SparkApplication) {
    if (!optionApps.stopping.get(app.id) || app['optionState'] == OptionStateEnum.NONE) {
      await fetchSparkAppCancel({
        id: app.id,
      });
      Swal.fire({
        icon: 'success',
        title: t('flink.app.operation.canceling'),
        showConfirmButton: false,
        timer: 2000,
      });
      optionApps.stopping.set(app.id, new Date().getTime());
      // openStopModal(true, { application: app });
    }
  }

  /* Click to delete */
  async function handleDelete(app: SparkApplication) {
    const hide = createMessage.loading('deleting', 0);
    try {
      await fetchSparkAppRemove(app.id!);
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
        router.push({ path: '/spark/app/add' });
      },
      schemas: [
        {
          label: t('spark.app.tags'),
          field: 'tags',
          component: 'Select',
          componentProps: {
            placeholder: t('spark.app.tags'),
            showSearch: true,
            options: tagsOptions.value.map((t: Recordable) => ({ label: t, value: t })),
            onChange: handlePageDataReload.bind(null, false),
          },
        },
        {
          label: t('spark.app.owner'),
          field: 'userId',
          component: 'Select',
          componentProps: {
            placeholder: t('spark.app.owner'),
            showSearch: true,
            options: users.value.map((u: Recordable) => {
              return { label: u.nickName || u.username, value: u.userId };
            }),
            onChange: handlePageDataReload.bind(null, false),
          },
        },
        {
          label: t('spark.app.jobType'),
          field: 'jobType',
          component: 'Select',
          componentProps: {
            placeholder: t('spark.app.jobType'),
            showSearch: true,
            options: [
              { label: 'JAR', value: JobTypeEnum.JAR },
              { label: 'SQL', value: JobTypeEnum.SQL },
              { label: 'PySpark', value: JobTypeEnum.PYSPARK },
            ],
            onChange: handlePageDataReload.bind(null, false),
          },
        },
        {
          label: t('spark.app.searchName'),
          field: 'appName',
          component: 'Input',
          componentProps: {
            placeholder: t('spark.app.searchName'),
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
    fetchSparkAppRecord(params).then((res) => {
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
  return {
    // registerStartModal,
    // registerStopModal,
    // registerLogModal,
    registerBuildDrawer,
    getTableActions,
    formConfig,
    tagsOptions,
    users,
  };
};
