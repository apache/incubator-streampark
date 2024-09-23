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

export enum ResourceTypeEnum {
  APP = 'APP',
  CONNECTOR = 'CONNECTOR',
  UDXF = 'UDXF',
  JAR_LIBRARY = 'JAR_LIBRARY',
  GROUP = 'GROUP',
}

export enum EngineTypeEnum {
  FLINK = 'FLINK',
  SPARK = 'SPARK',
}

export const columns: BasicColumn[] = [
  {
    title: t('flink.resource.table.resourceName'),
    dataIndex: 'resourceName',
    sorter: true,
  },
  {
    title: t('common.description'),
    dataIndex: 'description',
  },
  {
    title: t('flink.resource.resourceType'),
    dataIndex: 'resourceType',
  },
  {
    title: t('flink.resource.engineType'),
    dataIndex: 'engineType',
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

export const searchFormSchema: FormSchema[] = [
  {
    field: 'resourceName',
    label: '',
    component: 'Input',
    componentProps: {
      placeholder: t('flink.resource.table.searchByResourceName'),
      allowClear: true,
    },
    colProps: { span: 6 },
  },
];
