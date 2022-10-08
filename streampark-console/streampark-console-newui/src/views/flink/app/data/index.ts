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

/* 获取diff编辑器配置 */
export const getMonacoOptions = (readOnly: boolean) => {
  return {
    selectOnLineNumbers: false,
    foldingStrategy: 'indentation', // 代码分小段折叠
    overviewRulerBorder: false, // 不要滚动条边框
    autoClosingBrackets: true,
    tabSize: 2, // tab 缩进长度
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
    quickSuggestionsDelay: 100, //代码提示延时
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
