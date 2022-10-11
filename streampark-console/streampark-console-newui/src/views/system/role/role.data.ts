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
import { RuleObject, StoreValue } from 'ant-design-vue/lib/form/interface';
import { fetchCheckName } from '/@/api/sys/role';
import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: 'Role Name',
    dataIndex: 'roleName',
  },
  {
    title: 'Description',
    dataIndex: 'remark',
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
    field: 'roleName',
    label: 'Role',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'createTime',
    label: 'Create Time',
    component: 'RangePicker',
    colProps: { span: 8 },
  },
];
async function handleRoleCheck(_rule: RuleObject, value: StoreValue) {
  if (value) {
    if (value.length > 10) {
      return Promise.reject('Role name should not be longer than 10 characters');
    } else {
      const res = await fetchCheckName({
        roleName: value,
      });
      if (res) {
        return Promise.resolve();
      } else {
        return Promise.reject('Sorry, the role name already exists');
      }
    }
  } else {
    return Promise.reject('Role name cannot be empty');
  }
}
export const formSchema: FormSchema[] = [
  {
    field: 'roleId',
    label: 'Role Id',
    component: 'Input',
    show: false,
  },
  {
    field: 'roleName',
    label: 'Role Name',
    required: true,
    component: 'Input',
    rules: [{ required: true, validator: handleRoleCheck, trigger: 'blur' }],
  },
  {
    label: 'Description',
    field: 'remark',
    component: 'InputTextArea',
  },
  {
    label: '',
    field: 'menuId',
    slot: 'menu',
    defaultValue: [],
    component: 'Input',
    rules: [{ required: true, message: 'Please select the permission.' }],
  },
];
