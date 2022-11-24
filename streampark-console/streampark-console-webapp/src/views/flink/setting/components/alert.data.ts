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
    label: t('flink.setting.alert.faultAlertType'),
    component: 'Select',
    slot: 'type',
    dynamicRules: () => [
      { required: true, message: t('flink.setting.alert.faultAlertTypeIsRequired') },
    ],
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
    label: t('flink.setting.alert.dingtalkAccessToken'),
    component: 'Input',
    componentProps: {
      placeholder: t('flink.setting.alert.dingtalkAccessTokenPlaceholder'),
    },
    rules: [{ required: true, message: 'Access token is required' }],
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'dingtalkSecretEnable',
    label: t('flink.setting.alert.secretEnable'),
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    helpMessage: t('flink.setting.alert.secretTokenEnableHelpMessage'),
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'dingtalkSecretToken',
    label: t('flink.setting.alert.secretToken'),
    component: 'Input',
    componentProps: {
      placeholder: t('flink.setting.alert.secretTokenPlaceholder'),
    },
    ifShow: ({ model }) => (model.alertType || []).includes('2') && model.dingtalkSecretEnable,
    rules: [
      {
        required: true,
        message: t('flink.setting.alert.dingTalkSecretTokenIsRequired'),
        trigger: 'blur',
      },
    ],
  },
  {
    field: 'alertDingUser',
    label: t('flink.setting.alert.dingTalkUser'),
    component: 'Input',
    componentProps: {
      placeholder: t('flink.setting.alert.dingTalkUserPlaceholder'),
    },
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'dingtalkIsAtAll',
    label: t('flink.setting.alert.dingtalkIsAtAll'),
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    helpMessage: t('flink.setting.alert.whetherNotifyAll'),
    ifShow: ({ model }) => (model.alertType || []).includes('2'),
  },
  {
    field: 'weToken',
    label: t('flink.setting.alert.weChattoken'),
    component: 'InputTextArea',
    colSlot: 'weToken',
    componentProps: {
      rows: 4,
      placeholder: t('flink.setting.alert.weChattokenPlaceholder'),
    },
    rules: [{ required: true, message: t('flink.setting.alert.weChattokenIsRequired') }],
  },
  {
    field: 'alertSms',
    label: t('flink.setting.alert.sms'),
    component: 'Input',
    componentProps: {
      placeholder: t('flink.setting.alert.smsPlaceholder'),
      allowClear: true,
    },
    colSlot: 'alertSms',
    rules: [{ required: true, message: t('flink.setting.alert.mobileNumberIsRequired') }],
  },
  {
    field: 'alertSmsTemplate',
    label: t('flink.setting.alert.smsTemplate'),
    component: 'InputTextArea',
    componentProps: {
      rows: 4,
      placeholder: t('flink.setting.alert.smsTemplateIsRequired'),
    },
    ifShow: ({ model }) => (model.alertType || []).includes('8'),
    colSlot: 'alertSmsTemplate',
  },
  {
    field: 'larkToken',
    label: t('flink.setting.alert.larkToken'),
    component: 'InputTextArea',
    colSlot: 'larkToken',
    rules: [{ required: true, message: 'Lark token is required' }],
  },
  {
    field: 'larkIsAtAll',
    label: t('flink.setting.alert.larkIsAtAll'),
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    ifShow: ({ model }) => (model.alertType || []).includes('16'),
    helpMessage: t('flink.setting.alert.whetherNotifyAll'),
  },
  {
    field: 'larkSecretEnable',
    label: t('flink.setting.alert.larkSecretEnable'),
    component: 'Switch',
    componentProps: {
      checkedChildren: 'ON',
      unCheckedChildren: 'OFF',
    },
    helpMessage: t('flink.setting.alert.larkTokenEnableHelpMessage'),
    ifShow: ({ model }) => (model.alertType || []).includes('16'),
  },
  {
    field: 'larkSecretToken',
    label: t('flink.setting.alert.larkSecretToken'),
    component: 'Input',
    componentProps: {
      placeholder: t('flink.setting.alert.larkSecretTokenPlaceholder'),
    },
    ifShow: ({ model }) => (model.alertType || []).includes('16') && model.larkSecretEnable,
    rules: [
      {
        required: true,
        message: t('flink.setting.alert.larkSecretTokenIsRequired'),
        trigger: 'blur',
      },
    ],
  },
];

export const alertTypes = {
  '1': { name: t('flink.setting.alert.email'), value: 1, disabled: false, icon: 'mail' },
  '2': { name: t('flink.setting.alert.dingTalk'), value: 2, disabled: false, icon: 'dingtalk' },
  '4': { name: t('flink.setting.alert.weChat'), value: 4, disabled: false, icon: 'wecom' },
  '8': { name: t('flink.setting.alert.sms'), value: 8, disabled: true, icon: 'message' },
  '16': { name: t('flink.setting.alert.lark'), value: 16, disabled: false, icon: 'lark' },
};
