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
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { checkUserName, fetchUserTypes } from '/@/api/sys/user';
import { FormTypeEnum } from '/@/enums/formEnum';

// user status enum
const enum StatusEnum {
  Effective = '1',
  Locked = '0',
}

// gender
const enum GenderEnum {
  Male = '0',
  Female = '1',
  Other = '2',
}

export const columns: BasicColumn[] = [
  {
    title: 'User Name',
    dataIndex: 'username',
    width: 200,
    sorter: true,
  },
  {
    title: 'Nick Name',
    dataIndex: 'nickName',
  },
  {
    title: 'User Type',
    dataIndex: 'userType',
    width: 180,
  },
  {
    title: 'Status',
    dataIndex: 'status',
    customRender: ({ record }) => {
      const enable = record?.status === StatusEnum.Effective;
      const color = enable ? 'green' : 'red';
      const text = enable ? 'Effective' : 'locked';
      return h(Tag, { color }, () => text);
    },
    filters: [
      { text: 'Effective', value: '1' },
      { text: 'Locked', value: '0' },
    ],
    filterMultiple: false,
  },
  {
    title: 'Create Time',
    dataIndex: 'createTime',
    width: 180,
    sorter: true,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'username',
    label: 'User Name',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    label: 'Create Time',
    field: 'createTime',
    component: 'RangePicker',
    colProps: { span: 8 },
  },
];

export const formSchema = (formType: string): FormSchema[] => {
  const isCreate = formType === FormTypeEnum.Create;
  // const isUpdate = formType === FormTypeEnum.Edit;
  const isView = formType === FormTypeEnum.View;

  return [
    {
      field: 'userId',
      label: 'User Id',
      component: 'Input',
      show: false,
    },
    {
      field: 'username',
      label: 'User Name',
      component: 'Input',
      rules: [
        { required: isCreate, message: 'username is required' },
        { min: 4, message: 'username length cannot be less than 4 characters' },
        { max: 8, message: 'exceeds maximum length limit of 8 characters' },
        {
          validator: async (_, value) => {
            if (!isCreate || !value || value.length < 4 || value.length > 8) {
              return Promise.resolve();
            }
            const res = await checkUserName({ username: value });
            if (!res) {
              return Promise.reject(`Sorry the username already exists`);
            }
          },
          trigger: 'blur',
        },
      ],
      componentProps: {
        id: 'formUserName',
        readonly: !isCreate,
      },
    },
    {
      field: 'nickName',
      label: 'Nick Name',
      component: 'Input',
      rules: [{ required: isCreate, message: 'nickName is required' }],
      componentProps: {
        readonly: !isCreate,
      },
    },
    {
      field: 'password',
      label: 'Password',
      component: 'InputPassword',
      rules: [
        { required: true, message: 'password is required' },
        { min: 8, message: 'Password length cannot be less than 8 characters' },
      ],
      required: true,
      ifShow: isCreate,
    },
    {
      field: 'email',
      label: 'E-Mail',
      component: 'Input',
      rules: [
        { type: 'email', message: 'please enter a valid email address' },
        { max: 50, message: 'exceeds maximum length limit of 50 characters' },
      ],
      componentProps: {
        readonly: isView,
        placeholder: 'input email',
      },
    },
    {
      label: 'User Type',
      field: 'userType',
      component: 'ApiSelect',
      componentProps: {
        disabled: isView,
        api: fetchUserTypes,
      },
      required: true,
    },
    {
      field: 'status',
      label: 'Status',
      component: 'RadioGroup',
      defaultValue: StatusEnum.Locked,
      componentProps: {
        options: [
          { label: 'locked', value: StatusEnum.Locked },
          { label: 'effective', value: StatusEnum.Effective },
        ],
      },
      rules: [{ required: true, message: 'please select status' }],
    },
    {
      field: 'sex',
      label: 'Gender',
      component: 'RadioGroup',
      defaultValue: GenderEnum.Male,
      componentProps: {
        options: [
          { label: 'male', value: GenderEnum.Male },
          { label: 'female', value: GenderEnum.Female },
          { label: 'secret', value: GenderEnum.Other },
        ],
      },
      required: true,
    },
    {
      field: 'description',
      label: 'Description',
      component: 'InputTextArea',
      ifShow: isCreate,
    },
  ];
};
