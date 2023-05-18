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
import { BasicColumn } from '/@/components/Table/src/types/table';
import { Tag, TypographyParagraph } from 'ant-design-vue';
import { DescItem } from '/@/components/Description';
import { h } from 'vue';
import State from '../components/State';
import Icon from '/@/components/Icon';
import { dateToDuration } from '/@/utils/dateUtil';
import { AppTypeEnum, JobTypeEnum } from '/@/enums/flinkEnum';
import { useI18n } from '/@/hooks/web/useI18n';

const { t } = useI18n();
export const getDescSchema = (): DescItem[] => {
  return [
    {
      field: 'id',
      label: t('flink.app.id'),
      render: (curVal) =>
        h(TypographyParagraph, { copyable: true, style: { color: '#477de9' } }, () => curVal),
    },
    { field: 'jobName', label: t('flink.app.appName') },
    {
      field: 'jobType',
      label: t('flink.app.developmentMode'),
      render: (curVal) =>
        h(
          'div',
          { class: 'bold-tag' },
          h(Tag, { color: curVal === 1 ? '#545454' : '#0C7EF2', class: 'mr-8px' }, () =>
            curVal === 1 ? 'Custom Code' : 'Flink SQL',
          ),
        ),
    },
    {
      field: 'module',
      label: t('flink.app.module'),
      show: (data) => data.jobType != JobTypeEnum.SQL,
    },
    {
      field: 'projectName',
      label: t('flink.app.project'),
      show: (data) => data.jobType != JobTypeEnum.SQL,
    },
    {
      field: 'appType',
      label: t('flink.app.appType'),
      render: (curVal) =>
        h(Tag, { color: curVal == AppTypeEnum.STREAMPARK_FLINK ? 'cyan' : 'blue' }, () =>
          curVal == AppTypeEnum.STREAMPARK_FLINK ? 'StreamPark Flink' : 'Apache Flink',
        ),
    },
    {
      field: 'state',
      label: t('flink.app.status'),
      render: (_curVal, data) => h(State, { option: 'state', data }),
    },
    {
      field: 'startTime',
      label: t('flink.app.startTime'),
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
      label: t('flink.app.endTime'),
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
      label: t('flink.app.duration'),
      render: (curVal) => dateToDuration(curVal),
      show: (data) => data.duration,
    },
    { field: 'description', label: t('common.description'), span: 2 },
  ];
};
/* setting */
export const getConfColumns = (): BasicColumn[] => [
  { title: 'Version', dataIndex: 'version' },
  { title: 'Conf Format', dataIndex: 'format' },
  { title: 'Effective', dataIndex: 'effective' },
  { title: 'Modify Time', dataIndex: 'createTime' },
];

export const getFlinkSqlColumns = (): BasicColumn[] => [
  { title: 'Version', dataIndex: 'version' },
  { title: 'Effective', dataIndex: 'effective' },
  { title: 'Candidate', dataIndex: 'candidate' },
  { title: 'Modify Time', dataIndex: 'createTime' },
];

export const getSavePointColumns = (): BasicColumn[] => [
  { title: 'Path', dataIndex: 'path' },
  { title: 'Trigger Time', dataIndex: 'triggerTime', width: 250 },
  { title: 'Type', dataIndex: 'type', width: 170 },
  { title: 'Latest', dataIndex: 'latest', width: 200 },
];
export const getBackupColumns = (): BasicColumn[] => [
  { title: 'Save Path', dataIndex: 'path', align: 'left' },
  { title: 'Description', dataIndex: 'description' },
  { title: 'Version', dataIndex: 'version', width: 100, align: 'center' },
  { title: 'Backup Time', dataIndex: 'createTime', width: 200 },
];

export const getOptionLogColumns = (): BasicColumn[] => [
  { title: 'Operation Name', dataIndex: 'optionName', width: 150 },
  { title: 'Application Id', dataIndex: 'yarnAppId' },
  { title: 'JobManager URL', dataIndex: 'jobManagerUrl' },
  { title: 'Start Status', dataIndex: 'success', width: 120 },
  { title: 'Option Time', dataIndex: 'optionTime', width: 200 },
];
