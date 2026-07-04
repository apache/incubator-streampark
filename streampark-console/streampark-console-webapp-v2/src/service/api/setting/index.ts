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

import type { SystemSetting } from '@/types/api/setting/types/setting.type'
import { request } from '../../http'

export function fetchSystemSettingAll() {
  return request.Post<SystemSetting[]>('/setting/all')
}

export function fetchSystemSettingUpdate(data: { settingKey: string, settingValue: string | boolean }) {
  return request.Post('/setting/update', data)
}

export function fetchDockerConfig() {
  return request.Post<Recordable>('/setting/docker')
}

export function fetchEmailConfig() {
  return request.Post<Recordable>('/setting/email')
}

export function fetchDockerUpdate(data: Recordable) {
  return request.Post('/setting/update/docker', data)
}

export function fetchEmailUpdate(data: Recordable) {
  return request.Post('/setting/update/email', data)
}

export function fetchVerifyDocker(data: Recordable) {
  return request.Post<SettingVerifyResult>('/setting/check/docker', data)
}

export function fetchVerifyEmail(data: Recordable) {
  return request.Post<SettingVerifyResult>('/setting/check/email', data)
}

export function fetchCheckHadoop() {
  return request.Post<boolean>('/setting/check/hadoop')
}

export interface SettingVerifyResult {
  status: number
  msg?: string
}

export * from './yarnQueue'
export * from './externalLink'
