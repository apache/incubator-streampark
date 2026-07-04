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

import type { MenuListModel, RolePageListGetResultModel } from '@/types/api/base/model/systemModel'
import type { BasicTableParams } from '@/types/api/model/baseModel'
import { request } from '../http'

export function fetchMenuList(params?: Recordable) {
  return request.Post<MenuListModel>('/menu/list', params ?? {})
}

export function fetchRoleMenu(params: { roleId: string | number }) {
  return request.Post<string[]>('/role/menu', params)
}

export function fetchRoleListByPage(params?: BasicTableParams & { roleName?: string }) {
  return request.Post<RolePageListGetResultModel>('/role/list', params ?? {})
}
