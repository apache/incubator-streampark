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
  systemSetting: 'System Setting',
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
  update: {
    success: 'setting updated successfully',
  },
  docker: {
    address: {
      label: 'Docker Register Address',
      desc: 'Docker container service address',
    },
    namespace: {
      label: 'Docker namespace',
      desc: 'Namespace for docker image used in docker building env and target image register',
    },
    userName: {
      label: 'Docker Register User',
      desc: 'Docker container service authentication username',
    },
    password: {
      label: 'Docker Register Password',
      desc: 'Docker container service authentication password',
    },
  },
  email: {
    host: {
      label: 'Alert Email Smtp Host',
      desc: 'Alert Mailbox Smtp Host',
    },
    port: {
      label: 'Alert Email Smtp Port',
      desc: 'Smtp Port of the alarm mailbox',
    },
    from: {
      label: 'Alert Email From',
      desc: 'Email to send alerts',
    },
    userName: {
      label: 'Alert Email User',
      desc: 'Authentication username used to send alert emails',
    },
    password: {
      label: 'Alert Email Password',
      desc: 'Authentication password used to send alarm email',
    },
    ssl: {
      label: 'Alert Email SSL',
      desc: 'Whether to enable SSL in the mailbox that sends the alert',
    },
  },
};
