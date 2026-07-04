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

import type {
  VariableDeleteParam,
  VariableListRecord,
  VariableParam,
} from '@/types/api/resource/variable/model/variableModel'
import type { BasicTableParams } from '@/types/api/model/baseModel'
import { request } from '../../http'

export function fetchVariableList(data: BasicTableParams & { variableCode?: string }) {
  return request.Post<VariableListRecord[]>('/variable/page', data)
}

export function fetchAddVariable(data: VariableParam) {
  return request.Post('/variable/post', data)
}

export function fetchUpdateVariable(data: VariableParam) {
  return request.Put('/variable/update', data)
}

export function fetchVariableDelete(data: VariableDeleteParam) {
  return request.Delete<{ status: string, message?: string }>('/variable/delete', data)
}

export function fetchCheckVariableCode(data: { variableCode: string }) {
  return request.Post<{ status: string, data?: boolean, message?: string }>('/variable/check/code', data)
}

export function fetchVariableInfo(data: { id: string }) {
  return request.Post<VariableListRecord>('/variable/show_original', data)
}

export function fetchDependApps(data: Recordable) {
  return request.Post<Recordable[] | { records?: Recordable[], total?: number }>('/variable/depend_apps', data)
}

export function fetchVariableAll(data?: { keyword?: string }) {
  return request.Post<VariableListRecord[]>('/variable/list', data ?? {})
}
