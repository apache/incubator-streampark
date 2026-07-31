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
  title: '云账号',
  accountName: '账号名称',
  provider: '云厂商',
  region: '地域',
  accessKey: 'Access Key',
  secretKey: 'Secret Key',
  connectivity: '连通状态',
  status: '状态',
  untested: '未测试',
  connected: '已连通',
  failed: '连接失败',
  enabled: '已启用',
  disabled: '已禁用',
  create: '创建云账号',
  edit: '编辑云账号',
  rotateHint: '编辑时两个密钥字段均留空，将保留原有凭证；凭证必须成对轮换。',
  secretHint: 'Secret Key 仅可写入，服务端不会返回明文。',
  test: '测试连接',
  testSuccess: '连接测试成功',
  testFailed: '连接测试失败',
  requestId: '请求 ID',
  disable: '禁用',
  disableConfirm: '确认禁用该云账号？',
  deleteConfirm: '确认删除该云账号？已被托管环境引用的账号无法删除。',
  grant: 'Team 授权',
  grantTitle: '授权 Team',
  grantHint: '只有被授权的 Team 才能使用该账号创建托管 Flink 环境。',
  noTeams: '暂无可授权的 Team。',
  lastCheckTime: '最近检测时间',
  lastError: '最近错误',
  endpointManaged: '云厂商 Endpoint 由 StreamPark 统一管理。',
};
