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

import { ActionItem, FormProps } from '/@/components/Table';
import { useMessage } from '/@/hooks/web/useMessage';
import { AppStateEnum } from '/@/enums/flinkEnum';
import { JobTypeEnum } from '/@/enums/sparkEnum';
import { usePermission } from '/@/hooks/web/usePermission';
import { useI18n } from '/@/hooks/web/useI18n';
import { isFunction, isObject } from '/@/utils/is';
import { fetchSparkAppRecord, fetchSparkAppRemove } from '/@/api/spark/app';
import type { SparkApplication } from '/@/api/spark/app.type';
import { useSparkAction } from './useSparkAction';

// Create form configurations and operation functions in the application table
export const useSparkTableAction = (handlePageDataReload: Fn) => {
  const { t } = useI18n();
  const tagsOptions = ref<Recordable>([]);

  const router = useRouter();
  const { createMessage } = useMessage();
  const { hasPermission } = usePermission();
  const { handleCopy, handleMapping, users } = useSparkAction();

  /* Operation button list */
  function getActionList(record: SparkApplication, _currentPageNo: number): ActionItem[] {
    return [
      {
        label: t('flink.app.operation.copy'),
        auth: 'app:copy',
        icon: 'ant-design:copy-outlined',
        onClick: handleCopy.bind(null, record),
      },
      {
        label: t('flink.app.operation.remapping'),
        ifShow: [
          AppStateEnum.ADDED,
          AppStateEnum.FAILED,
          AppStateEnum.CANCELED,
          AppStateEnum.KILLED,
          AppStateEnum.SUCCEEDED,
          AppStateEnum.TERMINATED,
          AppStateEnum.POS_TERMINATED,
          AppStateEnum.FINISHED,
          AppStateEnum.SUSPENDED,
          AppStateEnum.LOST,
        ].includes(record.state as AppStateEnum),
        auth: 'app:mapping',
        icon: 'ant-design:deployment-unit-outlined',
        onClick: handleMapping.bind(null, record),
      },
      {
        popConfirm: {
          title: t('flink.app.operation.deleteTip'),
          confirm: handleDelete.bind(null, record),
        },
        label: t('common.delText'),
        ifShow: [
          AppStateEnum.ADDED,
          AppStateEnum.FAILED,
          AppStateEnum.CANCELED,
          AppStateEnum.FINISHED,
          AppStateEnum.LOST,
          AppStateEnum.TERMINATED,
          AppStateEnum.POS_TERMINATED,
          AppStateEnum.SUCCEEDED,
          AppStateEnum.KILLED,
        ].includes(record.state as AppStateEnum),
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
        router.push({ path: '/spark/app/create' });
      },
      schemas: [
        {
          label: t('flink.app.tags'),
          field: 'tags',
          component: 'Select',
          componentProps: {
            placeholder: t('flink.app.tags'),
            showSearch: true,
            options: tagsOptions.value.map((t: Recordable) => ({ label: t, value: t })),
            onChange: handlePageDataReload.bind(null, false),
          },
        },
        {
          label: t('flink.app.owner'),
          field: 'userId',
          component: 'Select',
          componentProps: {
            placeholder: t('flink.app.owner'),
            showSearch: true,
            options: users.value.map((u: Recordable) => {
              return { label: u.nickName || u.username, value: u.userId };
            }),
            onChange: handlePageDataReload.bind(null, false),
          },
        },
        {
          label: t('flink.app.jobType'),
          field: 'jobType',
          component: 'Select',
          componentProps: {
            placeholder: t('flink.app.jobType'),
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
          label: t('flink.app.searchName'),
          field: 'jobName',
          component: 'Input',
          componentProps: {
            placeholder: t('flink.app.searchName'),
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
  return { getTableActions, formConfig };
};
