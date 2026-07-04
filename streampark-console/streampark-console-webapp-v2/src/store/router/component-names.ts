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

/** Maps view path (under views/) to Vue component `name` for keep-alive. */
const VIEW_ALIASES: Record<string, string> = {
  'spark/app/create.vue': 'spark/app/Add.vue',
  'spark/app/edit.vue': 'spark/app/Edit.vue',
}

export const VIEW_COMPONENT_NAMES: Record<string, string> = {
  'flink/app/index.vue': 'FlinkAppList',
  'flink/app/Add.vue': 'FlinkAppAdd',
  'flink/app/Detail.vue': 'FlinkAppDetail',
  'flink/app/EditStreamPark.vue': 'EditStreamParkApp',
  'flink/app/EditFlink.vue': 'EditFlinkApp',
  'flink/home/index.vue': 'FlinkHome',
  'flink/cluster/index.vue': 'FlinkCluster',
  'flink/cluster/Add.vue': 'AddCluster',
  'flink/cluster/Edit.vue': 'EditCluster',
  'flink/gateway/index.vue': 'FlinkGateway',
  'spark/app/index.vue': 'SparkApplication',
  'spark/app/Add.vue': 'SparkAppAdd',
  'spark/app/Edit.vue': 'SparkAppEdit',
  'spark/app/detail.vue': 'SparkApplicationDetail',
  'spark/home/index.vue': 'SparkHome',
  'resource/project/index.vue': 'Project',
  'resource/project/Add.vue': 'AddProject',
  'resource/project/Edit.vue': 'EditProject',
  'resource/upload/index.vue': 'ResourceUpload',
  'resource/variable/index.vue': 'Variable',
  'resource/variable/DependApps.vue': 'DependApps',
  'setting/system/index.vue': 'SystemSetting',
  'setting/alarm/index.vue': 'AlertSetting',
  'setting/extlink/index.vue': 'ExternalLinkSetting',
  'setting/yarn-queue/index.vue': 'YarnQueue',
  'system/user/index.vue': 'User',
  'system/token/index.vue': 'UserToken',
  'system/team/index.vue': 'Team',
  'system/role/index.vue': 'RoleManagement',
  'system/member/index.vue': 'Member',
  'build-in/error-log/index.vue': 'ErrorLog',
}

export function resolveViewComponentName(componentPath: string | null | undefined) {
  if (!componentPath)
    return undefined
  let rel = componentPath
  if (rel.startsWith('/src/views/'))
    rel = rel.replace('/src/views/', '')
  else if (rel.startsWith('/views/'))
    rel = rel.replace(/^\/views\//, '')
  if (rel.startsWith('/'))
    rel = rel.slice(1)
  if (VIEW_ALIASES[rel])
    rel = VIEW_ALIASES[rel]
  if (rel.endsWith('/View.vue'))
    rel = rel.replace(/\/View\.vue$/, '/index.vue')
  return VIEW_COMPONENT_NAMES[rel]
}
