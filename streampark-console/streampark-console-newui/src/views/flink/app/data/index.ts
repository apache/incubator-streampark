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

/* app 表格列 */
export const getAppColumns = (): BasicColumn[] => [
  {
    title: 'Application Name',
    dataIndex: 'jobName',
    align: 'left',
    width: 300,
  },
  {
    title: 'Tags',
    ellipsis: true,
    dataIndex: 'tags',
    width: 150,
  },
  {
    title: 'Owner',
    dataIndex: 'nickName',
    width: 100,
  },
  {
    title: 'Flink Version',
    dataIndex: 'flinkVersion',
    width: 130,
  },
  {
    title: 'Duration',
    dataIndex: 'duration',
    sorter: true,
    width: 150,
    customRender: ({ value }) => dateToDuration(value),
  },
  {
    title: 'Modified Time',
    dataIndex: 'modifyTime',
    sorter: true,
    width: 170,
  },
  {
    title: 'Run Status',
    dataIndex: 'state',
    width: 120,
    filters: [
      { text: 'ADDED', value: '0' },
      { text: 'STARTING', value: '3' },
      { text: 'RUNNING', value: '5' },
      { text: 'FAILED', value: '6' },
      { text: 'CANCELED', value: '9' },
      { text: 'FINISHED', value: '10' },
      { text: 'LOST', value: '13' },
      { text: 'SILENT', value: '17' },
      { text: 'TERMINATED', value: '18' },
    ],
  },
  {
    title: 'Launch | Build',
    dataIndex: 'launch',
    width: 220,
  },

  {
    title: 'Modified Time',
    dataIndex: 'modifyTime',
    sorter: true,
    width: 170,
  },
];

/* Get diff editor configuration */
export const getMonacoOptions = (readOnly: boolean) => {
  return {
    selectOnLineNumbers: false,
    foldingStrategy: 'indentation', // code fragmentation
    overviewRulerBorder: false, // Don't scroll bar borders
    autoClosingBrackets: true,
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
  { label: 'remote (standalone)', value: 1, disabled: false },
  { label: 'yarn application', value: 4, disabled: false },
  { label: 'yarn session', value: 3, disabled: false },
  { label: 'kubernetes session', value: 5, disabled: false },
  { label: 'kubernetes application', value: 6, disabled: false },
  {
    label: 'yarn per-job (deprecated, please use yarn-application mode)',
    value: 2,
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
