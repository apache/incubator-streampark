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
import { Icon } from '/@/components/Icon';

export const enum TypeEnum {
  Menu = '0',
  Button = '1',
  Dir = '2',
}

const isDir = (type: string) => type === TypeEnum.Dir;
const isMenu = (type: string) => type === TypeEnum.Menu;
const isButton = (type: string) => type === TypeEnum.Button;

export const columns: BasicColumn[] = [
  {
    title: 'Name',
    dataIndex: 'text',
    width: 200,
    align: 'left',
  },
  {
    title: 'Icon',
    dataIndex: 'icon',
    width: 50,
    customRender: ({ record }) => {
      return record.icon
        ? h(Icon, { icon: record.icon + '-outlined', prefix: 'ant-design' })
        : null;
    },
  },
  {
    title: 'Type',
    dataIndex: 'type',
    customRender: ({ record }) => {
      const text = isMenu(record.type) ? 'menu' : 'button';
      return h(Tag, { color: isMenu(record.type) ? 'cyan' : 'pink' }, () => text);
    },
  },
  {
    title: 'Path',
    dataIndex: 'path',
  },
  {
    title: 'Vue Component',
    dataIndex: 'component',
  },
  {
    title: 'Permission',
    dataIndex: 'permission',
  },
  {
    title: 'Order By',
    dataIndex: 'order',
  },
  {
    title: 'Create Time',
    dataIndex: 'createTime',
  },
  {
    title: 'Modify Time',
    dataIndex: 'modifyTime',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'menuName',
    label: 'Menu Name',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'createTime',
    label: 'Create Time',
    component: 'RangePicker',
    componentProps: {},
    colProps: { span: 8 },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'type',
    label: 'Menu Type',
    component: 'RadioButtonGroup',
    defaultValue: TypeEnum.Menu,
    componentProps: {
      options: [
        { label: 'Menu', value: TypeEnum.Menu },
        { label: 'Button', value: TypeEnum.Button },
      ],
    },
    colProps: { lg: 24, md: 24 },
  },
  {
    field: 'menuId',
    label: 'menuId',
    component: 'Input',
    show: false,
  },
  {
    field: 'menuName',
    label: 'Menu Name',
    component: 'Input',
    required: true,
    rules: [
      { required: true, message: 'Menu Name is required' },
      { max: 20, message: 'exceeds maximum length limit of 20 characters' },
    ],
  },
  {
    field: 'parentId',
    label: 'Parent Menu',
    component: 'TreeSelect',
    componentProps: {
      fieldNames: {
        label: 'title',
        key: 'id',
        value: 'id',
      },
      treeLine: true,
      getPopupContainer: () => document.body,
    },
  },
  {
    field: 'orderNum',
    label: 'sort',
    component: 'InputNumber',
    required: true,
    ifShow: ({ values }) => !isButton(values.type),
  },
  {
    field: 'icon',
    label: 'icon',
    component: 'IconPicker',
    required: true,
    ifShow: ({ values }) => !isButton(values.type),
  },
  {
    field: 'path',
    label: 'Menu URL',
    component: 'Input',
    required: true,
    ifShow: ({ values }) => !isButton(values.type),
  },
  {
    field: 'component',
    label: 'component address',
    component: 'Input',
    ifShow: ({ values }) => isMenu(values.type),
  },
  {
    field: 'perms',
    label: 'Related permissions',
    component: 'Input',
    rules: [{ max: 50, message: 'Length cannot exceed 50 characters' }],
    ifShow: ({ values }) => !isDir(values.type),
  },
  {
    field: 'display',
    label: 'whether to display',
    component: 'Switch',
    defaultValue: '1',
    componentProps: {
      checkedValue: '1',
      unCheckedValue: '0',
      checkedChildren: 'Yes',
      unCheckedChildren: 'No',
    },
    ifShow: ({ values }) => !isButton(values.type),
  },
];
