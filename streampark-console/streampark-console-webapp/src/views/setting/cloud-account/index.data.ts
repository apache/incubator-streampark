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

import type { BasicColumn, FormSchema } from '/@/components/Table';
import { useI18n } from '/@/hooks/web/useI18n';

const { t } = useI18n();

export const columns: BasicColumn[] = [
  { title: t('setting.cloudAccount.accountName'), dataIndex: 'accountName', sorter: true },
  { title: t('setting.cloudAccount.provider'), dataIndex: 'providerType', width: 120 },
  { title: t('setting.cloudAccount.region'), dataIndex: 'region', width: 140 },
  { title: t('setting.cloudAccount.accessKey'), dataIndex: 'accessKeyMask', width: 150 },
  { title: t('setting.cloudAccount.connectivity'), dataIndex: 'connectivityState', width: 130 },
  { title: t('setting.cloudAccount.status'), dataIndex: 'status', width: 100 },
  { title: t('setting.cloudAccount.lastCheckTime'), dataIndex: 'lastCheckTime', width: 180 },
  { title: t('common.modifyTime'), dataIndex: 'modifyTime', sorter: true, width: 180 },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'accountName',
    label: '',
    component: 'Input',
    componentProps: {
      allowClear: true,
      placeholder: t('setting.cloudAccount.accountName'),
    },
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '',
    component: 'Select',
    componentProps: {
      allowClear: true,
      placeholder: t('setting.cloudAccount.status'),
      options: [
        { label: t('setting.cloudAccount.enabled'), value: 1 },
        { label: t('setting.cloudAccount.disabled'), value: 0 },
      ],
    },
    colProps: { span: 4 },
  },
];
