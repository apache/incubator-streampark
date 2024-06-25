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
      name: 'Maven 配置',
    },
    dockerSetting: {
      name: 'Docker 环境配置',
    },
    emailSetting: {
      name: '邮箱配置',
    },
    consoleSetting: {
      name: '控制台配置',
    },
    ingressSetting: {
      name: 'Kubernetes Ingress 配置',
    },
  },
  update: {
    success: '设置更新成功！',
    dockerNotStart: '依赖你本地的Docker服务，请确保本地Docker已启动. 🙂',
  },
  title: {
    mavenSettings: 'Maven Settings 文件路径',
    mavenRepository: 'Maven 仓库',
    mavenUser: 'Maven 仓库访问用户',
    mavenPassword: 'Maven 仓库访问密码',
    docker: 'Docker 容器服务设置',
    email: '告警邮箱设置',
    ingress: 'Ingress 域名设置',
  },
  desc: {
    mavenSettings: '指定本地 maven 的 settings.xml 文件完成路径',
    mavenRepository: '设置 maven 的仓库地址，可以是 maven 私服的地址或者阿里云等 maven 私服地址',
    mavenUser: '访问 maven 私服所需的认证用户名(如需要)',
    mavenPassword: '访问 maven 私服所需的认证密码(如需要)',
    docker: '设置 docker 容器服务的注册信息',
    email: '设置用于发送告警的 email 发送者信息',
    ingress: 'ingress 设置，通过传入域名自动生成基于 nginx 的 ingress',
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
    username: {
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
