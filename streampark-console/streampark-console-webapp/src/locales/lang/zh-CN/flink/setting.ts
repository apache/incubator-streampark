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
  alert: {
    alertSetting: '告警设置',
    alertName: '告警名称',
    alertNamePlaceHolder: '请输入告警名称',
    alertNameTips: '告警名称, 举例: StreamPark 组告警',
    alertNameErrorMessage: {
      alertNameIsRequired: '告警名称必填',
      alertNameAlreadyExists: '告警名称必须唯一. 当前输入的名称已存在',
      alertConfigFailed: '错误出现 ,原因: ',
    },
    faultAlertType: '故障告警类型',
    faultAlertTypeIsRequired: '故障告警类型必选',
    email: '电子邮箱',
    alertEmail: '告警邮箱',
    alertEmailAddressIsRequired: '邮箱地址必填',
    alertEmailFormatIsInvalid: '(邮箱)格式有误',
    alertEmailPlaceholder: '请输入邮箱，多个邮箱用逗号(,)隔开',
    dingTalk: '钉钉',
    dingTalkUrl: '钉钉Url',
    dingTalkUrlFormatIsInvalid: '(钉钉Url)格式有误',
    dingTalkPlaceholder: '请输入钉钉Url',
    dingtalkAccessToken: '访问令牌',
    dingtalkAccessTokenPlaceholder: '请输入钉钉访问令牌',
    secretEnable: '启用密钥令牌',
    secretTokenEnableHelpMessage: '钉钉密钥令牌是否启用',
    secretToken: '密钥令牌',
    secretTokenPlaceholder: '请输入密钥令牌',
    dingTalkSecretTokenIsRequired: '钉钉密钥令牌必填',
    dingTalkUser: '钉钉消息接受者',
    dingTalkUserPlaceholder: '请输入钉钉消息接受者',
    dingtalkIsAtAll: '(通知)所有',
    whetherNotifyAll: '是否(通知)所有(消息接收者)',
    weChat: '微信',
    weChattoken: '微信令牌',
    weChattokenPlaceholder: '请输入微信令牌',
    weChattokenIsRequired: '微信令牌必填',
    sms: '短信',
    smsPlaceholder: '请输入手机号',
    mobileNumberIsRequired: '请输入手机号',
    smsTemplate: '短信模板',
    smsTemplateIsRequired: '短信模板必填',
    lark: '飞书',
    larkToken: '飞书令牌',
    larkTokenPlaceholder: '请输入飞书令牌',
    larkIsAtAll: '(通知)所有',
    larkSecretEnable: '启用飞书密钥令牌',
    larkTokenEnableHelpMessage: '飞书密钥令牌是否启用',
    larkSecretToken: '飞书密钥令牌',
    larkSecretTokenPlaceholder: '请输入飞书密钥令牌',
    larkSecretTokenIsRequired: '飞书密钥令牌必填',
    alertDetail: '告警(配置)详情',
    alertOperationMessage: {
      updateAlertConfigFailed: '告警配置更新失败!',
      updateAlertConfigSuccessfull: '告警配置更新成功!',
    },
    delete: '是否确定删除此警报 ?',
  },
  cluster: {
    detail: '查看集群详情',
    stop: '停止集群',
    start: '开启集群',
    edit: '编辑集群',
    delete: '确定要删除此集群 ?',
    form: {
      addType: '添加类型',
      addExisting: '已有集群',
      addNew: '全新集群',
    },
    placeholder: {
      addType: '请选择集群添加类型',
    },
    required: {
      address: '必须填写集群地址',
      clusterId: 'Yarn Session Cluster 为必填项',
    },
  },
  env: {
    conf: 'Flink 配置',
    sync: '配置同步',
  },
};
