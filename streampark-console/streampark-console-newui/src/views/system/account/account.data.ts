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
import { getAllRoleList, isAccountExist } from '/@/api/demo/system';
import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: 'username',
    dataIndex: 'account',
    width: 120,
  },
  {
    title: 'nick name',
    dataIndex: 'nickname',
    width: 120,
  },
  {
    title: 'email',
    dataIndex: 'email',
    width: 120,
  },
  {
    title: 'create time',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: 'role',
    dataIndex: 'role',
    width: 200,
  },
  {
    title: 'remark',
    dataIndex: 'remark',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'account',
    label: 'username',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'nickname',
    label: 'nick name',
    component: 'Input',
    colProps: { span: 8 },
  },
];

export const accountFormSchema: FormSchema[] = [
  {
    field: 'account',
    label: 'username',
    component: 'Input',
    helpMessage: [
      'This field demonstrates asynchronous validation',
      'Cannot enter username with admin',
    ],
    rules: [
      {
        required: true,
        message: 'please enter user name',
      },
      {
        validator(_, value) {
          return new Promise((resolve, reject) => {
            isAccountExist(value)
              .then(() => resolve())
              .catch((err) => {
                reject(err.message || 'verification failed');
              });
          });
        },
      },
    ],
  },
  {
    field: 'pwd',
    label: 'password',
    component: 'InputPassword',
    required: true,
    ifShow: false,
  },
  {
    label: 'role',
    field: 'role',
    component: 'ApiSelect',
    componentProps: {
      api: getAllRoleList,
      labelField: 'roleName',
      valueField: 'roleValue',
    },
    required: true,
  },
  {
    field: 'dept',
    label: 'department',
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
    field: 'nickname',
    label: 'nick name',
    component: 'Input',
    required: true,
  },

  {
    label: 'email',
    field: 'email',
    component: 'Input',
    required: true,
  },

  {
    label: 'remark',
    field: 'remark',
    component: 'InputTextArea',
  },
];
