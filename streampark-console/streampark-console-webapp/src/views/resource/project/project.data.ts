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

import { BuildStateEnum } from '/@/enums/flinkEnum';
import { useI18n } from '/@/hooks/web/useI18n';
const { t } = useI18n();

interface Status {
  label?: string;
  key?: string;
}

export const statusList: Status[] = [
  { label: t('flink.project.projectStatus.all'), key: '' },
  { label: t('flink.project.projectStatus.notBuild'), key: String(BuildStateEnum.NOT_BUDIL) },
  { label: t('flink.project.projectStatus.building'), key: String(BuildStateEnum.BUILDING) },
  { label: t('flink.project.projectStatus.buildSuccess'), key: String(BuildStateEnum.SUCCESSFUL) },
  { label: t('flink.project.projectStatus.buildFailed'), key: String(BuildStateEnum.FAILED) },
];

export const buildStateMap = {
  [String(BuildStateEnum.NOT_BUDIL)]: {
    color: '#C0C0C0',
    label: t('flink.project.projectStatus.notBuild'),
  },
  [String(BuildStateEnum.NEED_REBUILD)]: {
    color: '#FFA500',
    label: t('flink.project.projectStatus.needRebuild'),
  },
  [String(BuildStateEnum.BUILDING)]: {
    color: '#1AB58E',
    label: t('flink.project.projectStatus.building'),
    className: 'status-processing-building',
  },
  [String(BuildStateEnum.SUCCESSFUL)]: {
    color: '#52c41a',
    label: t('flink.project.projectStatus.successful'),
  },
  [String(BuildStateEnum.FAILED)]: {
    color: '#f5222d',
    label: t('flink.project.projectStatus.failed'),
  },
};
