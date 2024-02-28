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
  systemSetting: '系统设置',
  systemSettingItems: {
    mavenSetting: {
      name: 'Maven配置',
    },
    dockerSetting: {
      name: 'Docker环境配置',
    },
    emailSetting: {
      name: '邮箱配置',
    },
    consoleSetting: {
      name: '控制台配置',
    },
    ingressSetting: {
      name: 'k8s Ingress 配置',
    },
  },
  update: {
    success: '设置更新成功！',
  },
  docker: {
    address: {
      label: 'Docker注册地址',
      desc: 'Docker容器服务地址',
    },
    namespace: {
      label: 'Docker命名空间',
      desc: 'Docker构建环境和目标镜像注册使用的命名空间',
    },
    userName: {
      label: 'Docker注册用户',
      desc: 'Docker容器服务认证用户名',
    },
    password: {
      label: 'Docker注册密码',
      desc: 'Docker容器服务认证密码',
    },
  },
  email: {
    host: {
      label: '告警邮件Smtp主机',
      desc: '告警邮箱Smtp主机',
    },
    port: {
      label: '告警邮件Smtp端口',
      desc: '告警邮箱Smtp端口',
    },
    userName: {
      label: '告警邮件用户名',
      desc: '用于发送告警邮件的用户名认证',
    },
    password: {
      label: '告警邮件密码',
      desc: '用于发送告警邮件的密码认证',
    },
    from: {
      label: '告警邮件发送者',
      desc: '发送告警的邮箱',
    },
    ssl: {
      label: '告警邮件SSL',
      desc: '是否在发送告警邮箱中启用 SSL',
    },
  },
};
