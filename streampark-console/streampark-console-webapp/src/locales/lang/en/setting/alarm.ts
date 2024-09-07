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
export default {
  alertSetting: 'Alarm Setting',
  tooltip: {
    test: 'Alert Test',
    detail: 'Alert Detail',
    edit: 'Edit Alert Config',
    delete: 'Are you sure delete this alert conf ?',
  },
  alertName: 'Alert Name',
  alertNamePlaceHolder: 'Please enter alert name',
  alertNameTips: 'the alert name, e.g: StreamPark team alert',
  alertNameErrorMessage: {
    alertNameIsRequired: 'Alert Name is required',
    alertNameAlreadyExists: 'Alert Name must be unique. The alert name already exists',
    alertConfigFailed: 'error happened ,caused by: ',
  },
  faultAlertType: 'Fault Alert Type',
  faultAlertTypeIsRequired: 'Fault Alert Type is required',
  email: 'E-mail',
  alertEmail: 'Alert Email',
  alertEmailAddressIsRequired: 'email address is required',
  alertEmailFormatIsInvalid: 'Incorrect format',
  alertEmailPlaceholder: 'Please enter email,separate multiple emails with comma(,)',
  dingTalk: 'Ding Talk',
  dingTalkUrl: 'DingTalk Url',
  dingTalkUrlFormatIsInvalid: 'Incorrect format',
  dingTalkPlaceholder: 'Please enter DingTask Url',
  dingtalkAccessToken: 'Access Token',
  dingtalkAccessTokenPlaceholder: 'Please enter the access token of DingTalk',
  secretEnable: 'Secret Enable',
  secretTokenEnableHelpMessage: 'DingTalk secretToken is enable',
  secretToken: 'Secret Token',
  secretTokenPlaceholder: 'please enter Secret Token',
  dingTalkSecretTokenIsRequired: 'DingTalk SecretToken is required',
  dingTalkUser: 'DingTalk User',
  dingTalkUserPlaceholder: 'Please enter DingTalk receive user',
  dingtalkIsAtAll: 'At All User',
  whetherNotifyAll: 'Whether Notify All',
  weChat: 'WeChat',
  weChattoken: 'WeChat token',
  weChattokenPlaceholder: 'Please enter WeChart Token',
  weChattokenIsRequired: 'WeChat Token is required',
  sms: 'SMS',
  smsPlaceholder: 'Please enter mobile number',
  mobileNumberIsRequired: 'Please enter mobile number',
  smsTemplate: 'SMS Template',
  smsTemplateIsRequired: 'SMS Template is required',
  lark: 'Lark',
  larkPlaceholder: 'Lark',
  larkToken: 'Lark Token',
  larkTokenPlaceholder: 'Please enter the access token of LarkTalk',
  larkIsAtAll: 'At All User',
  larkSecretEnable: 'Secret Enable',
  larkTokenEnableHelpMessage: 'Lark secretToken is enable',
  larkSecretToken: 'Lark Secret Token',
  larkSecretTokenPlaceholder: 'please enter Lark Secret Token',
  larkSecretTokenIsRequired: 'Lark SecretToken is required',
  alertDetail: 'Alert Detail',
  alertOperationMessage: {
    updateAlertConfigFailed: 'Update AlertConfig Failed!',
    updateAlertConfigSuccessfull: 'Update AlertConfig successful!',
  },
  fail: {
    title: 'Failed create AlertConfig',
    subTitle: 'alertName {0} is already exists!',
    update: 'Failed update AlertConfig',
  },
  success: {
    title: 'Create AlertConfig successful!',
    update: 'Update AlertConfig successful!',
  },
};
