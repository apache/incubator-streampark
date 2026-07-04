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

import type { AddMemberParams, MemberListRecord, UpdateMemberParams } from '@/types/api/system/model/memberModel'
import type { UserListItem } from '@/types/api/base/model/systemModel'
import type { BasicTableParams } from '@/types/api/model/baseModel'
import { request } from '../http'

export interface UserTeamItem {
  id: string
  teamName: string
}

export function fetchUserTeam(data: { userId: number | string }) {
  return request.Post<UserTeamItem[]>('/member/teams', data)
}

export function fetchMemberList(data: BasicTableParams & { userName?: string, roleName?: string }) {
  return request.Post<MemberListRecord[]>('/member/list', data)
}

export function fetchAddMember(data: AddMemberParams) {
  return request.Post('/member/post', data)
}

export function fetchUpdateMember(data: UpdateMemberParams) {
  return request.Put('/member/update', data)
}

export function fetchMemberDelete(data: { id: string }) {
  return request.Delete<{ status: string }>('/member/delete', data)
}

export function fetchCandidateUsers(params?: { teamId?: string | number }) {
  const method = request.Post<UserListItem[]>('/member/candidateUsers', {})
  if (params)
    method.config.params = params
  return method
}
