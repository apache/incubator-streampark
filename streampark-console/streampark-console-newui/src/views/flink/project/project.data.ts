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
export const enum BuildStatusEnum {
  All = ' ',
  NotBuild = '-1',
  Building = '0',
  BuildSuccess = '1',
  BuildFail = '2',
  NeedBuild = '-2',
}

interface Status {
  label?: string;
  key?: string;
}

export const statusList: Status[] = [
  {
    label: 'All',
    key: BuildStatusEnum.All,
  },
  {
    label: 'Not Build',
    key: BuildStatusEnum.NotBuild,
  },
  {
    label: 'Building',
    key: BuildStatusEnum.Building,
  },
  {
    label: 'Build Success',
    key: BuildStatusEnum.BuildSuccess,
  },
  {
    label: 'Build Failed',
    key: BuildStatusEnum.BuildFail,
  },
];

export const buildStateMap = {
  [BuildStatusEnum.NotBuild]: {
    color: '#C0C0C0',
    label: 'NOT BUILD',
  },
  [BuildStatusEnum.NeedBuild]: {
    color: '#FFA500',
    label: 'NEED REBUILD',
  },
  [BuildStatusEnum.Building]: {
    color: '#1AB58E',
    label: 'BUILDING',
    className: 'status-processing-building',
  },
  [BuildStatusEnum.BuildSuccess]: {
    color: '#52c41a',
    label: 'SUCCESSFUL',
  },
  [BuildStatusEnum.BuildFail]: {
    color: '#f5222d',
    label: 'FAILED',
  },
};

export enum ProjectType {
  Flink = 1,
  Spark = 2,
}
