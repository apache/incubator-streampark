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
import { dateToDuration } from '/@/utils/dateUtil';
import { BasicColumn } from '/@/components/Table';
import { ExecModeEnum } from '/@/enums/flinkEnum';
import { useI18n } from '/@/hooks/web/useI18n';
const { t } = useI18n();

/* app */
export const getAppColumns = (): BasicColumn[] => [
  {
    title: t('flink.app.table.applicationName'),
    dataIndex: 'jobName',
    align: 'left',
    fixed: 'left',
    width: 300,
  },
  { title: t('flink.app.table.tags'), ellipsis: true, dataIndex: 'tags', width: 150 },
  { title: t('flink.app.table.owner'), dataIndex: 'nickName', width: 100 },
  { title: t('flink.app.table.flinkVersion'), dataIndex: 'flinkVersion', width: 130 },
  {
    title: t('flink.app.table.duration'),
    dataIndex: 'duration',
    sorter: true,
    width: 150,
    customRender: ({ value }) => dateToDuration(value),
  },
  { title: t('flink.app.table.modifiedTime'), dataIndex: 'modifyTime', sorter: true, width: 170 },
  {
    title: t('flink.app.table.runStatus'),
    dataIndex: 'state',
    width: 120,
    filters: [
      { text: t('flink.app.runStatusOptions.added'), value: '0' },
      { text: t('flink.app.runStatusOptions.starting'), value: '3' },
      { text: t('flink.app.runStatusOptions.running'), value: '5' },
      { text: t('flink.app.runStatusOptions.failed'), value: '7' },
      { text: t('flink.app.runStatusOptions.canceled'), value: '9' },
      { text: t('flink.app.runStatusOptions.finished'), value: '10' },
      { text: t('flink.app.runStatusOptions.suspended'), value: '11' },
      { text: t('flink.app.runStatusOptions.lost'), value: '13' },
      { text: t('flink.app.runStatusOptions.silent'), value: '17' },
      { text: t('flink.app.runStatusOptions.terminated'), value: '18' },
    ],
  },
  { title: t('flink.app.table.launchBuild'), dataIndex: 'launch', width: 220 },
];

/* Get diff editor configuration */
export const getMonacoOptions = (readOnly: boolean) => {
  return {
    selectOnLineNumbers: false,
    foldingStrategy: 'indentation', // code fragmentation
    overviewRulerBorder: false, // Don't scroll bar borders
    autoClosingBrackets: 'always',
    autoClosingDelete: 'always',
    tabSize: 2, // tab indent length
    readOnly,
    inherit: true,
    scrollBeyondLastLine: false,
    lineNumbersMinChars: 5,
    lineHeight: 24,
    automaticLayout: true,
    cursorBlinking: 'line',
    cursorStyle: 'line',
    cursorWidth: 3,
    renderFinalNewline: true,
    renderLineHighlight: 'all',
    quickSuggestionsDelay: 100, // Code prompt delay
    scrollbar: {
      useShadows: false,
      vertical: 'visible',
      horizontal: 'visible',
      horizontalSliderSize: 5,
      verticalSliderSize: 5,
      horizontalScrollbarSize: 15,
      verticalScrollbarSize: 15,
    },
  };
};

export const resolveOrder = [
  { label: 'parent-first', value: 0 },
  { label: 'child-first', value: 1 },
];

export const k8sRestExposedType = [
  { label: 'LoadBalancer', value: 0 },
  { label: 'ClusterIP', value: 1 },
  { label: 'NodePort', value: 2 },
];

export const executionModes = [
  { label: 'remote (standalone)', value: ExecModeEnum.REMOTE, disabled: false },
  { label: 'yarn application', value: ExecModeEnum.YARN_APPLICATION, disabled: false },
  { label: 'yarn session', value: ExecModeEnum.YARN_SESSION, disabled: false },
  { label: 'kubernetes session', value: ExecModeEnum.KUBERNETES_SESSION, disabled: false },
  {
    label: 'kubernetes application',
    value: ExecModeEnum.KUBERNETES_APPLICATION,
    disabled: false,
  },
  {
    label: 'yarn per-job (deprecated, please use yarn-application mode)',
    value: ExecModeEnum.YARN_PER_JOB,
    disabled: false,
  },
];
export const cpTriggerAction = [
  { label: 'alert', value: 1 },
  { label: 'restart', value: 2 },
];
export const launchTitleMap = new Map();
launchTitleMap.set(-1, 'launch failed');
launchTitleMap.set(1, 'current job need relaunch');
launchTitleMap.set(2, 'launching');
launchTitleMap.set(3, 'launch finished,need restart');
launchTitleMap.set(4, 'application is rollbacked,need relaunch');
