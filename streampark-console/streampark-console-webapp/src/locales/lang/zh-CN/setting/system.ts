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
    dockerNotStart: '依赖你本地的Docker服务，请确保本地Docker已启动. 🙂',
  },
  docker: {
    address: {
      label: 'Docker 地址',
      desc: 'Docker 容器的服务地址',
    },
    namespace: {
      label: 'Docker 命名空间',
      desc: 'Docker 构建环境和目标镜像注册使用的命名空间',
    },
    userName: {
      label: 'Docker 用户名',
      desc: 'Docker 容器服务认证用户名',
    },
    password: {
      label: 'Docker 密码',
      desc: 'Docker 容器服务认证密码',
    },
  },
  email: {
    host: {
      label: '发送邮件服务器',
      desc: '发送告警邮件的服务器 Smtp 主机',
    },
    port: {
      label: '发送邮件Smtp端口',
      desc: '告警邮箱 Smtp端口',
    },
    userName: {
      label: '邮箱用户名',
      desc: '发送告警邮件的用户名,一般是你的邮箱地址',
    },
    password: {
      label: '邮箱密码',
      desc: '用于发送告警邮件的认证密码',
    },
    from: {
      label: '邮箱地址',
      desc: '用于发送告警的邮箱',
    },
    ssl: {
      label: '开启 SSL',
      desc: '是否在发送告警邮箱中启用 SSL',
    },
  },
};
