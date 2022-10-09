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

export const columns: BasicColumn[] = [
  {
    title: 'Team Code',
    dataIndex: 'teamCode',
    width: 200,
    align: 'left',
  },
  {
    title: 'Team Name',
    dataIndex: 'teamName',
  },
  {
    title: 'Create Time',
    dataIndex: 'createTime',
    width: 180,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'teamName',
    label: 'Team Name',
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

export const formSchema: FormSchema[] = [
  {
    field: 'teamName',
    label: 'Team Name',
    component: 'Input',
    required: true,
  },
  {
    field: 'teamCode',
    label: 'Team Code',
    component: 'Input',
    required: true,
  },
];
