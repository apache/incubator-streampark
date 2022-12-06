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
  settingTab: {
    systemSetting: 'System Setting',
    alertSetting: 'Alert Setting',
    flinkHome: 'Flink Home',
    flinkCluster: 'Flink Cluster',
  },
  systemSettingItems: {
    mavenSetting: {
      name: 'Maven Setting',
    },
    dockerSetting: {
      name: 'Docker Setting',
    },
    emailSetting: {
      name: 'Sender Email Setting',
    },
    consoleSetting: {
      name: 'Console Setting',
    },
    ingressSetting: {
      name: 'Ingress Setting',
    },
  },
  flink: {
    flinkName: 'Flink Name',
    flinkNamePlaceholder: 'Please enter flink name',
    flinkHome: 'Flink Home',
    flinkHomePlaceholder: 'Please enter flink home',
    description: 'description',
    descriptionPlaceholder: 'Please enter description',
    operateMessage: {
      flinkNameTips: 'the flink name, e.g: flink-1.12',
      flinkNameIsUnique: 'flink name is already exists',
      flinkNameIsRequired: 'flink name is required',
      flinkHomeTips: 'The absolute path of the FLINK_HOME',
      flinkHomeIsRequired: 'flink home is required',
      createFlinkHomeSuccessful: ' create successful!',
      updateFlinkHomeSuccessful: ' update successful!',
    },
  },
  alert: {
    alertSetting: 'Alert Setting',
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
    delete: 'Are you sure delete this alert conf ?',
  },
  cluster: {
    detail: 'View Cluster Detail',
    stop: 'Stop Cluster',
    start: 'Start Cluster',
    edit: 'Edit Cluster',
    delete: 'Are you sure delete this cluster ?',
    form: {
      clusterName: 'Cluster Name',
      address: 'Cluster URL',
      internal: 'internal cluster',
      executionMode: 'Execution Mode',
      versionId: 'Flink Version',
      addExisting: 'existing cluster',
      addNew: 'new cluster',
      yarnQueue: 'Yarn Queue',
      yarnSessionClusterId: 'Yarn Session Cluster',
      k8sNamespace: 'Kubernetes Namespace',
      k8sClusterId: 'Kubernetes ClusterId',
      serviceAccount: 'Service Account',
      k8sConf: 'Kube Conf File',
      flinkImage: 'Flink Base Docker Image',
      k8sRestExposedType: 'Rest-Service Exposed Type',
      resolveOrder: 'Resolve Order',
      taskSlots: 'Task Slots',
      totalOptions: 'Total Memory Options',
      jmOptions: 'JM Memory Options',
      tmOptions: 'TM Memory Options',
      dynamicProperties: 'Dynamic Properties',
      clusterDescription: 'Description',
    },
    placeholder: {
      addType: 'Please select cluster Add Type',
      clusterName: 'Please enter cluster name',
      executionMode: 'Please enter cluster name',
      versionId: 'please select Flink Version',
      yarnQueue: 'Please enter yarn queue',
      addressRemoteMode: 'Please enter jobManager URL',
      addressNoRemoteMode: 'Please enter cluster address,  e.g: http://host:port',
      yarnSessionClusterId: 'Please enter Yarn Session cluster',
      k8sConf: '~/.kube/config',
      flinkImage:
        'Please enter the tag of Flink base docker image, such as: flink:1.13.0-scala_2.11-java8',
      k8sRestExposedType: 'kubernetes.rest-service.exposed.type',
      resolveOrder: 'classloader.resolve-order',
      taskSlots: 'Number of slots per TaskManager',
      jmOptions: 'Please select the resource parameters to set',
      tmOptions: 'Please select the resource parameters to set',
      clusterDescription: 'Please enter description for this application',
    },
    required: {
      address: 'cluster address is required',
      executionMode: 'Execution Mode is required',
      clusterId: 'Yarn Session Cluster is required',
      versionId: 'Flink Version is required',
      flinkImage: 'Flink Base Docker Image is required',
      resolveOrder: 'Resolve Order is required',
    },
    operateMessage: {
      createFlinkSessionClusterSuccessful: ' create successful!',
      createFlinkSessionClusterFailed: 'create cluster failed, please check log',
      hadoopEnvInitializationFailed:
        'Hadoop environment initialization failed, please check the environment settings',
      flinkClusterIsStarting: 'The current cluster is starting',
      flinkClusterHasStartedSuccessful: 'The current cluster is started',
      updateFlinkClusterSuccessful: ' update successful!',
    },
    view: {
      clusterId: 'ClusterId',
    },
  },
  env: {
    conf: 'Flink Conf',
    sync: 'Sync Conf',
  },
};
