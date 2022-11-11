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
import { FormSchema } from '/@/components/Form';

export const alertFormSchema: Array<FormSchema> = [
  {
    field: 'alertType',
    label: 'Fault Alert Type',
    component: 'Select',
    slot: 'type',
    dynamicRules: () => [{ required: true, message: 'Fault Alert Type is required' }],
  },
  {
    field: 'alertEmail',
    label: 'Alert Email',
    component: 'Input',
    colSlot: 'alertEmail',
  },
  {
    field: 'alertDingURL',
    label: 'DingTalk Url',
    component: 'Input',
    colSlot: 'alertDingURL',
  },
  {
    field: 'dingtalkToken',
    label: 'Access Token',
    component: 'Input',
    componentProps: {
      placeholder: 'Please enter the access token of DingTalk',
    },
    rules: [{ required: true, message: 'Access token is required' }],
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'dingtalkSecretEnable',
    label: 'Secret Enable',
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    helpMessage: 'DingTalk ecretToken is enable',
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'dingtalkSecretToken',
    label: 'Secret Token',
    component: 'Input',
    componentProps: {
      placeholder: 'please enter Secret Token',
    },
    ifShow: ({ model }) => (model.alertType || []).includes('2') && model.dingtalkSecretEnable,
    rules: [{ required: true, message: 'DingTalk SecretToken is required', trigger: 'blur' }],
  },
  {
    field: 'alertDingUser',
    label: 'DingTalk User',
    component: 'Input',
    componentProps: {
      placeholder: 'Please enter DingTalk receive user',
    },
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'dingtalkIsAtAll',
    label: 'At All User',
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    helpMessage: 'Whether Notify All',
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'weToken',
    label: 'WeChat token',
    component: 'InputTextArea',
    colSlot: 'weToken',
    componentProps: {
      rows: 4,
      placeholder: 'Please enter WeChart Token',
    },
    rules: [{ required: true, message: 'WeChat Token is required' }],
  },
  {
    field: 'alertSms',
    label: 'SMS',
    component: 'Input',
    componentProps: {
      placeholder: 'Please enter mobile number',
      allowClear: true,
    },
    colSlot: 'alertSms',
    rules: [{ required: true, message: 'mobile number is required' }],
  },
  {
    field: 'alertSmsTemplate',
    label: 'SMS Template',
    component: 'InputTextArea',
    componentProps: {
      rows: 4,
      placeholder: 'SMS Template is required',
    },
    ifShow: ({ model }) => (model.alertType || []).includes('8'),
    colSlot: 'alertSmsTemplate',
  },
  {
    field: 'larkToken',
    label: 'Lark Token',
    component: 'InputTextArea',
    colSlot: 'larkToken',
    rules: [{ required: true, message: 'Lark token is required' }],
  },
  {
    field: 'larkIsAtAll',
    label: 'At All User',
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    ifShow: ({ model }) => (model.alertType || []).includes('16'),
    helpMessage: 'Whether Notify All',
  },
  {
    field: 'larkSecretEnable',
    label: 'Secret Enable',
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    helpMessage: 'Lark secretToken is enable',
    ifShow: ({ model }) => (model.alertType || []).includes('16'),
  },
  {
    field: 'larkSecretToken',
    label: 'Lark Secret Token',
    component: 'Input',
    componentProps: {
      placeholder: 'please enter Lark Secret Token',
    },
    ifShow: ({ model }) => (model.alertType || []).includes('16') && model.larkSecretEnable,
    rules: [{ required: true, message: 'Lark SecretToken is required', trigger: 'blur' }],
  },
];

export const alertTypes = {
  '1': { name: 'E-mail', value: 1, disabled: false, icon: 'mail' },
  '2': { name: 'Ding Talk', value: 2, disabled: false, icon: 'dingtalk' },
  '4': { name: 'Wechat', value: 4, disabled: false, icon: 'wecom' },
  '8': { name: 'SMS', value: 8, disabled: true, icon: 'message' },
  '16': { name: 'Lark', value: 16, disabled: false, icon: 'lark' },
};
