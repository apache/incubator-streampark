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
import { useI18n } from '/@/hooks/web/useI18n';

const { t } = useI18n();
export const alertFormSchema: Array<FormSchema> = [
  {
    field: 'alertType',
    label: t('setting.alarm.faultAlertType'),
    component: 'Select',
    slot: 'type',
    dynamicRules: () => [{ required: true, message: t('setting.alarm.faultAlertTypeIsRequired') }],
  },
  {
    field: 'alertEmail',
    label: t('setting.alarm.alertEmail'),
    component: 'Input',
    colSlot: 'alertEmail',
  },
  {
    field: 'alertDingURL',
    label: t('setting.alarm.dingTalkUrl'),
    component: 'Input',
    colSlot: 'alertDingURL',
  },
  {
    field: 'dingtalkToken',
    label: t('setting.alarm.dingtalkAccessToken'),
    component: 'Input',
    componentProps: {
      placeholder: t('setting.alarm.dingtalkAccessTokenPlaceholder'),
    },
    rules: [{ required: true, message: 'Access token is required' }],
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'dingtalkSecretEnable',
    label: t('setting.alarm.secretEnable'),
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    helpMessage: t('setting.alarm.secretTokenEnableHelpMessage'),
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'dingtalkSecretToken',
    label: t('setting.alarm.secretToken'),
    component: 'Input',
    componentProps: {
      placeholder: t('setting.alarm.secretTokenPlaceholder'),
    },
    ifShow: ({ model }) => (model.alertType || []).includes('2') && model.dingtalkSecretEnable,
    rules: [
      {
        required: true,
        message: t('setting.alarm.dingTalkSecretTokenIsRequired'),
        trigger: 'blur',
      },
    ],
  },
  {
    field: 'alertDingUser',
    label: t('setting.alarm.dingTalkUser'),
    component: 'Input',
    componentProps: {
      placeholder: t('setting.alarm.dingTalkUserPlaceholder'),
    },
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'dingtalkIsAtAll',
    label: t('setting.alarm.dingtalkIsAtAll'),
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    helpMessage: t('setting.alarm.whetherNotifyAll'),
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'weToken',
    label: t('setting.alarm.weChattoken'),
    component: 'InputTextArea',
    colSlot: 'weToken',
    componentProps: {
      rows: 4,
      placeholder: t('setting.alarm.weChattokenPlaceholder'),
    },
    rules: [{ required: true, message: t('setting.alarm.weChattokenIsRequired') }],
  },
  {
    field: 'alertSms',
    label: t('setting.alarm.sms'),
    component: 'Input',
    componentProps: {
      placeholder: t('setting.alarm.smsPlaceholder'),
      allowClear: true,
    },
    colSlot: 'alertSms',
    rules: [{ required: true, message: t('setting.alarm.mobileNumberIsRequired') }],
  },
  {
    field: 'alertSmsTemplate',
    label: t('setting.alarm.smsTemplate'),
    component: 'InputTextArea',
    componentProps: {
      rows: 4,
      placeholder: t('setting.alarm.smsTemplateIsRequired'),
    },
    ifShow: ({ model }) => (model.alertType || []).includes('8'),
    colSlot: 'alertSmsTemplate',
  },
  {
    field: 'larkToken',
    label: t('setting.alarm.larkToken'),
    component: 'InputTextArea',
    colSlot: 'larkToken',
    rules: [{ required: true, message: 'Lark token is required' }],
  },
  {
    field: 'larkIsAtAll',
    label: t('setting.alarm.larkIsAtAll'),
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    ifShow: ({ model }) => (model.alertType || []).includes('16'),
    helpMessage: t('setting.alarm.whetherNotifyAll'),
  },
  {
    field: 'larkSecretEnable',
    label: t('setting.alarm.larkSecretEnable'),
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    helpMessage: t('setting.alarm.larkTokenEnableHelpMessage'),
    ifShow: ({ model }) => (model.alertType || []).includes('16'),
  },
  {
    field: 'larkSecretToken',
    label: t('setting.alarm.larkSecretToken'),
    component: 'Input',
    componentProps: {
      placeholder: t('setting.alarm.larkSecretTokenPlaceholder'),
    },
    ifShow: ({ model }) => (model.alertType || []).includes('16') && model.larkSecretEnable,
    rules: [
      {
        required: true,
        message: t('setting.alarm.larkSecretTokenIsRequired'),
        trigger: 'blur',
      },
    ],
  },
];

export const alertTypes = {
  '1': { name: t('setting.alarm.email'), value: 1, disabled: false, icon: 'mail' },
  '2': { name: t('setting.alarm.dingTalk'), value: 2, disabled: false, icon: 'dingtalk' },
  '4': { name: t('setting.alarm.weChat'), value: 4, disabled: false, icon: 'wecom' },
  '8': { name: t('setting.alarm.sms'), value: 8, disabled: true, icon: 'message' },
  '16': { name: t('setting.alarm.lark'), value: 16, disabled: false, icon: 'lark' },
};
