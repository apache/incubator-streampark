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
import { BasicColumn, FormSchema } from '/@/components/Table';
import { useI18n } from '/@/hooks/web/useI18n';
const { t } = useI18n();

export const searchFormSchema: FormSchema[] = [
  {
    field: 'user',
    label: t('system.token.table.userName'),
    component: 'Input',
    colProps: { span: 8 },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'catalogName',
    label: t('setting.flinkCatalog.name'),
    component: 'Input',
    rules: [{ required: true, message: t('setting.flinkCatalog.checkResult.emptyHint') }],
  },
  {
    field: 'properties',
    label: t('setting.flinkCatalog.properties'),
    component: 'InputTextArea',
    rules: [{ required: true, message: t('setting.flinkCatalog.checkResult.emptyProperties') }],
    componentProps: { rows: 10 },
  },
  {
    field: 'description',
    label: t('common.description'),
    component: 'InputTextArea',
    componentProps: { rows: 4 },
  },
];

export const columns: BasicColumn[] = [
  {
    title: 'id',
    dataIndex: 'id',
    ifShow: false,
  },
  {
    title: t('setting.flinkCatalog.name'),
    dataIndex: 'catalogName',
    sorter: true,
  },
  {
    title: t('setting.flinkCatalog.properties'),
    dataIndex: 'properties',
    sorter: true,
  },
  {
    title: t('common.description'),
    dataIndex: 'description',
    ellipsis: true,
    width: 350,
  },
  {
    title: t('common.createTime'),
    dataIndex: 'createTime',
    sorter: true,
  },
  {
    title: t('common.modifyTime'),
    dataIndex: 'modifyTime',
    sorter: true,
  },
];
