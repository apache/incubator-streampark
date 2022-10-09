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
import { Switch } from 'ant-design-vue';
import { useMessage } from '/@/hooks/web/useMessage';
import { setTokenStatus } from '/@/api/sys/token';
import { getNoTokenUserList } from '/@/api/sys/user';
import dayjs from 'dayjs';

// status enum
const enum StatusEnum {
  On = '1',
  Off = '0',
}

export const columns: BasicColumn[] = [
  {
    title: 'User Name',
    dataIndex: 'username',
    width: 150,
    sorter: true,
  },
  {
    title: 'Token',
    width: 250,
    dataIndex: 'token',
  },
  {
    title: 'Description',
    dataIndex: 'description',
  },
  {
    title: 'Create Time',
    dataIndex: 'createTime',
  },
  {
    title: 'Expire Time',
    dataIndex: 'expireTime',
    sorter: true,
  },
  {
    title: 'Status',
    dataIndex: 'userStatus',
    width: 100,
    customRender: ({ record }) => {
      if (!Reflect.has(record, 'pendingStatus')) {
        record.pendingStatus = false;
      }
      return h(Switch, {
        checked: record.userStatus === StatusEnum.On,
        checkedChildren: 'on',
        unCheckedChildren: 'off',
        loading: record.pendingStatus,
        onChange(checked: boolean) {
          record.pendingStatus = true;
          const newStatus = checked ? StatusEnum.On : StatusEnum.Off;
          const { createMessage } = useMessage();

          setTokenStatus({ tokenId: record.id })
            .then(() => {
              record.userStatus = newStatus;
              createMessage.success(`success`);
            })
            .finally(() => {
              record.pendingStatus = false;
            });
        },
      });
    },
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'username',
    label: 'User Name',
    component: 'Input',
    colProps: { span: 8 },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'userId',
    label: 'User',
    component: 'ApiSelect',
    required: true,
    componentProps: {
      api: getNoTokenUserList,
      resultField: 'records',
      labelField: 'username',
      valueField: 'userId',
    },
  },
  {
    field: 'description',
    label: 'Description',
    component: 'InputTextArea',
  },
  {
    field: 'expireTime',
    label: 'ExpireTime',
    component: 'DatePicker',
    defaultValue: dayjs('9999-01-01'),
    componentProps: {
      disabled: true,
    },
  },
];
