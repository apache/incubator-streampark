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
import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

export const columns: BasicColumn[] = [
  {
    title: 'department name',
    dataIndex: 'deptName',
    width: 160,
    align: 'left',
  },
  {
    title: 'sort',
    dataIndex: 'orderNo',
    width: 50,
  },
  {
    title: 'status',
    dataIndex: 'status',
    width: 80,
    customRender: ({ record }) => {
      const status = record.status;
      const enable = ~~status === 0;
      const color = enable ? 'green' : 'red';
      const text = enable ? 'enable' : 'disable';
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: 'create time',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: 'remark',
    dataIndex: 'remark',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'deptName',
    label: 'department name',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'status',
    label: 'status',
    component: 'Select',
    componentProps: {
      options: [
        { label: 'enable', value: '0' },
        { label: 'disable', value: '1' },
      ],
    },
    colProps: { span: 8 },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'deptName',
    label: 'department name',
    component: 'Input',
    required: true,
  },
  {
    field: 'parentDept',
    label: 'parent department',
    component: 'TreeSelect',

    componentProps: {
      fieldNames: {
        label: 'deptName',
        key: 'id',
        value: 'id',
      },
      getPopupContainer: () => document.body,
    },
    required: true,
  },
  {
    field: 'orderNo',
    label: 'sort',
    component: 'InputNumber',
    required: true,
  },
  {
    field: 'status',
    label: 'status',
    component: 'RadioButtonGroup',
    defaultValue: '0',
    componentProps: {
      options: [
        { label: 'enable', value: '0' },
        { label: 'disable', value: '1' },
      ],
    },
    required: true,
  },
  {
    label: 'remark',
    field: 'remark',
    component: 'InputTextArea',
  },
];
