import { BasicColumn } from '/@/components/Table/src/types/table';
import { Tag, TypographyParagraph } from 'ant-design-vue';
import { DescItem } from '/@/components/Description';
import { h } from 'vue';
import State from '../components/State';
import Icon from '/@/components/Icon';
import { dateToDuration } from '/@/utils/dateUtil';

export const getDescSchema = (): DescItem[] => {
  return [
    {
      field: 'id',
      label: 'ID',
      render: (curVal) =>
        h(TypographyParagraph, { copyable: true, style: { color: '#477de9' } }, () => curVal),
    },
    { field: 'jobName', label: 'Application Name' },
    {
      field: 'jobType',
      label: 'Development Mode',
      render: (curVal) =>
        h(
          'div',
          { class: 'app_state' },
          h(Tag, { color: curVal === 1 ? '#545454' : '#0C7EF2' }, () =>
            curVal === 1 ? 'Custom Code' : 'Flink SQL',
          ),
        ),
    },
    { field: 'module', label: 'Module', show: (data) => data.jobType != 2 },
    { field: 'projectName', label: 'Project', show: (data) => data.jobType != 2 },
    {
      field: 'appType',
      label: 'Application Type',
      render: (curVal) =>
        h(Tag, { color: curVal == 1 ? 'cyan' : 'blue' }, () =>
          curVal == 1 ? 'StreamPark Flink' : 'Apache Flink',
        ),
    },
    {
      field: 'state',
      label: 'Status',
      render: (_curVal, data) => h(State, { option: 'state', data }),
    },
    {
      field: 'startTime',
      label: 'Start Time',
      render: (curVal) =>
        h(
          'div',
          null,
          curVal
            ? [h(Icon, { icon: 'ant-design:clock-circle-outlined' }), h('span', null, curVal)]
            : '-',
        ),
    },
    {
      field: 'endTime',
      label: 'End Time',
      render: (curVal) =>
        h(
          'div',
          null,
          curVal
            ? [h(Icon, { icon: 'ant-design:clock-circle-outlined' }), h('span', null, curVal)]
            : '-',
        ),
    },
    {
      field: 'duration',
      label: 'Duration',
      render: (curVal) => dateToDuration(curVal),
      show: (data) => data.duration,
    },
    { field: 'description', label: 'Description', span: 2 },
  ];
};
/* 配置 */
export const getConfColumns = (): BasicColumn[] => [
  {
    title: 'Version',
    dataIndex: 'version',
  },
  {
    title: 'Conf Format',
    dataIndex: 'format',
  },
  {
    title: 'Effective',
    dataIndex: 'effective',
  },
  {
    title: 'Candidate',
    dataIndex: 'candidate',
  },
  {
    title: 'Modify Time',
    dataIndex: 'createTime',
  },
];

export const getSavePointColumns = (): BasicColumn[] => [
  {
    title: 'Path',
    dataIndex: 'path',
    width: '45%',
  },
  {
    title: 'Trigger Time',
    dataIndex: 'triggerTime',
    width: 250,
  },
  {
    title: 'Type',
    dataIndex: 'type',
  },
  {
    title: 'Latest',
    dataIndex: 'latest',
  },
];
export const getBackupColumns = (): BasicColumn[] => [
  {
    title: 'Save Path',
    dataIndex: 'path',
    width: '40%',
    align: 'left',
  },
  {
    title: 'Description',
    dataIndex: 'description',
    width: '20%',
  },
  {
    title: 'Version',
    dataIndex: 'version',
    width: '10%',
  },
  {
    title: 'Backup Time',
    dataIndex: 'createTime',
  },
];

export const getOptionLogColumns = (): BasicColumn[] => [
  {
    title: 'Application Id',
    dataIndex: 'yarnAppId',
    width: '20%',
  },
  {
    title: 'JobManager URL',
    dataIndex: 'jobManagerUrl',
    width: '25%',
  },
  {
    title: 'Start Status',
    dataIndex: 'success',
  },
  {
    title: 'Option Time',
    dataIndex: 'optionTime',
  },
];
