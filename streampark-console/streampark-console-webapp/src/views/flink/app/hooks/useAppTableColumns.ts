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
import { ColumnType } from 'ant-design-vue/lib/table';
import { useI18n } from '/@/hooks/web/useI18n';
import { computed, ref, unref } from 'vue';
import { BasicColumn } from '/@/components/Table';
import { AppStateEnum } from '/@/enums/flinkEnum';
import { dateToDuration } from '/@/utils/dateUtil';
import { useMenuSetting } from '/@/hooks/setting/useMenuSetting';
const { t } = useI18n();

export const useAppTableColumns = () => {
  const { getCollapsed } = useMenuSetting();
  // app table column width
  const tableColumnWidth = ref({
    jobName: 250,
    flinkVersion: 150,
    tags: 150,
    state: 130,
    release: 120,
    duration: 150,
    modifyTime: 165,
    nickName: 100,
  });
  function onTableColumnResize(width: number, columns: ColumnType) {
    if (!columns?.dataIndex) return;
    const dataIndexStr = columns?.dataIndex.toString() ?? '';
    if (Reflect.has(tableColumnWidth.value, dataIndexStr)) {
      // when table column width changed, save it to table column width ref
      tableColumnWidth.value[dataIndexStr] = width < 100 ? 100 : width;
    }
  }

  const getAppColumns = computed((): BasicColumn[] => [
    {
      title: t('flink.app.appName'),
      dataIndex: 'jobName',
      align: 'left',
      fixed: 'left',
      resizable: true,
      width: unref(tableColumnWidth).jobName,
    },
    {
      title: t('flink.app.flinkVersion'),
      dataIndex: 'flinkVersion',
      width: unref(tableColumnWidth).flinkVersion,
    },
    {
      title: t('flink.app.tags'),
      ellipsis: true,
      dataIndex: 'tags',
      ...(getCollapsed.value
        ? {
            minWidth: unref(tableColumnWidth).tags,
          }
        : {
            width: unref(tableColumnWidth).tags,
          }),
    },
    { title: t('flink.app.owner'), dataIndex: 'nickName', width: unref(tableColumnWidth).nickName },
    {
      title: t('flink.app.runStatus'),
      dataIndex: 'state',
      fixed: 'right',
      width: unref(tableColumnWidth).state,
      filters: [
        { text: t('flink.app.runStatusOptions.added'), value: String(AppStateEnum.ADDED) },
        { text: t('flink.app.runStatusOptions.starting'), value: String(AppStateEnum.STARTING) },
        { text: t('flink.app.runStatusOptions.running'), value: String(AppStateEnum.RUNNING) },
        { text: t('flink.app.runStatusOptions.failed'), value: String(AppStateEnum.FAILED) },
        { text: t('flink.app.runStatusOptions.canceled'), value: String(AppStateEnum.CANCELED) },
        { text: t('flink.app.runStatusOptions.finished'), value: String(AppStateEnum.FINISHED) },
        { text: t('flink.app.runStatusOptions.suspended'), value: String(AppStateEnum.SUSPENDED) },
        { text: t('flink.app.runStatusOptions.lost'), value: String(AppStateEnum.LOST) },
        { text: t('flink.app.runStatusOptions.silent'), value: String(AppStateEnum.SILENT) },
        {
          text: t('flink.app.runStatusOptions.terminated'),
          value: String(AppStateEnum.TERMINATED),
        },
      ],
    },
    {
      title: t('flink.app.releaseBuild'),
      dataIndex: 'release',
      width: unref(tableColumnWidth).release,
      fixed: 'right',
    },
    {
      title: t('flink.app.duration'),
      dataIndex: 'duration',
      sorter: true,
      width: unref(tableColumnWidth).duration,
      customRender: ({ value }) => dateToDuration(value),
    },
    {
      title: t('flink.app.modifiedTime'),
      dataIndex: 'modifyTime',
      sorter: true,
      width: unref(tableColumnWidth).modifyTime,
    },
  ]);
  return { getAppColumns, onTableColumnResize, tableColumnWidth };
};
