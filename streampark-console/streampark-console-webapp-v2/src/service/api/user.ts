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

import type { TeamSetResponse, UserListRecord } from '@/types/api/system/model/userModel'
import type { BasicTableParams } from '@/types/api/model/baseModel'
import { request } from '../http'

export function fetchPermCode() {
    return request.Get<string[]>('/getPermCode')
}

export function fetchSetUserTeam(data: { teamId: string; userId?: string | number }) {
    return request.Post<TeamSetResponse>('/user/set_team', data)
}

export function fetchUserList(data: BasicTableParams) {
    return request.Post<{ records: UserListRecord[]; total: number }>('/user/list', data)
}

export function fetchAddUser(data: Recordable) {
    return request.Post('/user/post', data)
}

export function fetchUpdateUser(data: Recordable) {
    return request.Put('/user/update', data)
}

export function fetchDeleteUser(data: { userId: string }) {
    return request.Delete('/user/delete', data)
}

export function fetchResetUserPassword(data: { username: string }) {
    return request.Put<string>('/user/password/reset', data)
}

export function fetchUserPasswordUpdate(data: {
    userId: string | number
    oldPassword: string
    password: string
}) {
    return request.Put<boolean>('/user/password', data)
}

export function fetchCheckUserName(data: { username: string }) {
    return request.Post<boolean>('/user/check/name', data)
}

export function fetchNoTokenUserList(data: Recordable = {}) {
    return request.Post<{ records: Array<{ userId: string; username: string }> }>(
        '/user/getNoTokenUser',
        data,
    )
}

export function fetchAppOwners(data: Recordable = {}) {
    return request.Post<Array<{ userId: string; username: string; nickName?: string }>>(
        '/user/appOwners',
        data,
    )
}
