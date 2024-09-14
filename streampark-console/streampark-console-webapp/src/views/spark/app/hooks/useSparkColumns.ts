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
const { t } = useI18n();

export const useSparkColumns = () => {
  // app table column width
  const tableColumnWidth = ref({
    appName: 250,
    sparkVersion: 150,
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
      title: t('spark.app.appName'),
      dataIndex: 'appName',
      align: 'left',
      fixed: 'left',
      resizable: true,
      width: unref(tableColumnWidth).appName,
    },
    {
      title: t('spark.app.sparkVersion'),
      dataIndex: 'sparkVersion',
      width: unref(tableColumnWidth).sparkVersion,
    },
    {
      title: t('spark.app.tags'),
      ellipsis: true,
      dataIndex: 'tags',
      width: unref(tableColumnWidth).tags,
    },

    {
      title: t('spark.app.duration'),
      dataIndex: 'duration',
      sorter: true,
      width: unref(tableColumnWidth).duration,
      customRender: ({ value }) => dateToDuration(value),
    },
    {
      title: t('spark.app.modifiedTime'),
      dataIndex: 'modifyTime',
      sorter: true,
      width: unref(tableColumnWidth).modifyTime,
    },
    { title: t('spark.app.owner'), dataIndex: 'nickName', width: unref(tableColumnWidth).nickName },
    {
      title: t('spark.app.releaseBuild'),
      dataIndex: 'release',
      width: unref(tableColumnWidth).release,
      fixed: 'right',
    },
    {
      title: t('spark.app.runStatus'),
      dataIndex: 'state',
      fixed: 'right',
      width: unref(tableColumnWidth).state,
      filters: [
        { text: t('spark.app.runStatusOptions.added'), value: String(AppStateEnum.ADDED) },
        { text: t('spark.app.runStatusOptions.starting'), value: String(AppStateEnum.STARTING) },
        { text: t('spark.app.runStatusOptions.running'), value: String(AppStateEnum.RUNNING) },
        { text: t('spark.app.runStatusOptions.failed'), value: String(AppStateEnum.FAILED) },
        { text: t('spark.app.runStatusOptions.canceled'), value: String(AppStateEnum.CANCELED) },
        { text: t('spark.app.runStatusOptions.finished'), value: String(AppStateEnum.FINISHED) },
        { text: t('spark.app.runStatusOptions.suspended'), value: String(AppStateEnum.SUSPENDED) },
        { text: t('spark.app.runStatusOptions.lost'), value: String(AppStateEnum.LOST) },
        { text: t('spark.app.runStatusOptions.silent'), value: String(AppStateEnum.SILENT) },
        {
          text: t('spark.app.runStatusOptions.terminated'),
          value: String(AppStateEnum.TERMINATED),
        },
      ],
    },
  ]);
  return { getAppColumns, onTableColumnResize, tableColumnWidth };
};
