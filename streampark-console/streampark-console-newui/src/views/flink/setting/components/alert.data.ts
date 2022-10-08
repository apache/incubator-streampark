import { FormSchema } from '/@/components/Form';

export const alertFormSchema: Array<FormSchema> = [
  {
    field: 'alertType',
    label: 'Fault Alert Type',
    component: 'Select',
    slot: 'type',
    dynamicRules: () => {
      return [{ required: true, message: 'Fault Alert Type is required' }];
    },
  },
  { field: 'alertEmail', label: 'Alert Email', component: 'Input', colSlot: 'alertEmail' },
  {
    field: 'alertDingURL',
    label: 'DingTalk Url',
    component: 'Input',
    colSlot: 'alertDingURL',
    defaultValue: 'https://oapi.dingtalk.com/robot/send',
  },
  {
    field: 'dingtalkToken',
    label: 'Access Token',
    component: 'Input',
    colSlot: 'dingtalkToken',
  },
  {
    field: 'dingtalkSecretEnable',
    label: 'Secret Enable',
    component: 'Input',
    colSlot: 'dingtalkSecretEnable',
  },
  {
    field: 'dingtalkSecretToken',
    label: 'Secret Token',
    component: 'Input',
    colSlot: 'dingtalkSecretToken',
  },
  {
    field: 'alertDingUser',
    label: 'DingTalk User',
    component: 'Input',
    colSlot: 'alertDingUser',
  },
  {
    field: 'dingtalkIsAtAll',
    label: 'At All User',
    component: 'Input',
    colSlot: 'dingtalkIsAtAll',
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
    colSlot: 'alertSmsTemplate',
  },
  {
    field: 'larkToken',
    label: 'Lark Token',
    component: 'InputTextArea',
    colSlot: 'larkToken',
  },
  {
    field: 'larkIsAtAll',
    label: 'At All User',
    component: 'Switch',
    colSlot: 'larkIsAtAll',
  },
  {
    field: 'larkSecretEnable',
    label: 'Secret Enable',
    component: 'Switch',
    colSlot: 'larkSecretEnable',
  },
  {
    field: 'larkSecretToken',
    label: 'Lark Secret Token',
    component: 'Switch',
    colSlot: 'larkSecretToken',
  },
];
