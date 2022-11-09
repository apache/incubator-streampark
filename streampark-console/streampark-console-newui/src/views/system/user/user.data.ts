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
import { checkUserName, fetchUserTypes } from '/@/api/system/user';
import { FormTypeEnum } from '/@/enums/formEnum';
import {useI18n} from "/@/hooks/web/useI18n";
const { t } = useI18n();

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
  { title: t('system.user.table.userName'), dataIndex: 'username', sorter: true },
  { title: t('system.user.table.nickName'), dataIndex: 'nickName' },
  { title: t('system.user.table.userType'), dataIndex: 'userType' },
  {
    title: t('system.user.table.status'),
    dataIndex: 'status',
    customRender: ({ record }) => {
      const enable = record?.status === StatusEnum.Effective;
      const color = enable ? 'green' : 'red';
      const text = enable ? t('system.user.userStatus.effective') : t('system.user.userStatus.locked') ;
      return h(Tag, { color }, () => text);
    },
    filters: [
      { text: t('system.user.userStatus.effective'), value: '1' },
      { text: t('system.user.userStatus.locked'), value: '0' },
    ],
    filterMultiple: false,
  },
  {
    title: t('system.user.table.createTime'),
    dataIndex: 'createTime',
    sorter: true,
  },
];

export const searchFormSchema: FormSchema[] = [
  { field: 'username', label: t('system.user.table.userName'), component: 'Input', colProps: { span: 8 } },
  { field: 'createTime', label: t('system.user.table.createTime'),  component: 'RangePicker', colProps: { span: 8 } },
];

export const formSchema = (formType: string): FormSchema[] => {
  const isCreate = formType === FormTypeEnum.Create;
  // const isUpdate = formType === FormTypeEnum.Edit;
  const isView = formType === FormTypeEnum.View;

  return [
    { field: 'userId', label: 'User Id', component: 'Input', show: false },
    {
      field: 'username',
      label: t('system.user.table.userName'),
      component: 'Input',
      rules: [
        { required: isCreate, message: t('system.user.operation.userNameIsRequired') },
        { min: 4, message: t('system.user.operation.userNameMinLengthRequirement') },
        { max: 8, message:  t('system.user.operation.userNameMaxLengthRequirement') },
        {
          validator: async (_, value) => {
            if (!isCreate || !value || value.length < 4 || value.length > 8) {
              return Promise.resolve();
            }
            const res = await checkUserName({ username: value });
            if (!res) {
              return Promise.reject(t('system.user.operation.userNameAlreadyExists'));
            }
          },
          trigger: 'blur',
        },
      ],
      componentProps: {
        id: 'formUserName',
        disabled: !isCreate,
      },
    },
    {
      field: 'nickName',
      label: t('system.user.table.nickName'),
      component: 'Input',
      dynamicRules: () => {
        return [{ required: isCreate, message: t('system.user.operation.nickNameIsRequired') }];
      },
      componentProps: { disabled: !isCreate },
    },
    {
      field: 'password',
      label: t('system.user.table.password'),
      component: 'InputPassword',
      componentProps: { placeholder: t('system.user.operation.passwordPlaceholder') },
      helpMessage: t('system.user.operation.passwordMinLengthRequirement'),
      rules: [
        { required: true, message: t('system.user.operation.passwordIsRequired') },
        { min: 8, message: t('system.user.operation.passwordIsRequired') },
      ],
      required: true,
      ifShow: isCreate,
    },
    {
      field: 'email',
      label: t('system.user.table.email'),
      component: 'Input',
      rules: [
        { type: 'email', message: t('system.user.operation.emailValidMessage') },
        { max: 50, message: t('system.user.operation.emailMaxLengthRequirement') },
      ],
      componentProps: {
        readonly: isView,
        placeholder: t('system.user.operation.emailPlaceholder'),
      },
    },
    {
      label: t('system.user.table.userType'),
      field: 'userType',
      component: 'ApiSelect',
      componentProps: {
        disabled: isView,
        api: fetchUserTypes,
        placeholder: t('system.user.operation.userTypePlaceholder'),
      },
      rules: [{ required: true, message: t('system.user.operation.userTypePlaceholder') }],
    },
    {
      field: 'status',
      label: t('system.user.table.status'),
      component: 'RadioGroup',
      defaultValue: StatusEnum.Effective,
      componentProps: {
        options: [
          { label: t('system.user.userStatus.locked'), value: StatusEnum.Locked },
          { label: t('system.user.userStatus.effective'), value: StatusEnum.Effective },
        ],
      },
      rules: [{ required: true, message: t('system.user.operation.statusPlaceholder') }],
    },
    {
      field: 'sex',
      label: t('system.user.table.gender'),
      component: 'RadioGroup',
      defaultValue: GenderEnum.Male,
      componentProps: {
        options: [
          { label: t('system.user.userGender.male'), value: GenderEnum.Male },
          { label: t('system.user.userGender.female'), value: GenderEnum.Female },
          { label: t('system.user.userGender.secret'), value: GenderEnum.Other },
        ],
      },
      required: true,
    },
    {
      field: 'description',
      label: t('system.user.table.description'),
      component: 'InputTextArea',
      componentProps: { rows: 5, placeholder: t('system.user.operation.descriptionPlaceholder') },
      ifShow: isCreate,
    },
  ];
};
