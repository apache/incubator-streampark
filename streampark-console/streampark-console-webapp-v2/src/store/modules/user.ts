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

import type { UserInfo } from '/#/store'
import { getPinia } from '@/store/index'
import {
    APP_TEAMID_KEY_,
    EXPIRE_KEY,
    PERMISSION_KEY,
    ROLES_KEY,
    TOKEN_KEY,
    USER_INFO_KEY,
} from '@/enums/cacheEnum'
import { RoleEnum } from '@/enums/roleEnum'
import { getAuthCache, setAuthCache } from '@/utils/auth'
import { Persistent } from '@/utils/cache/persistent'

interface TeamListType {
    label: string
    value: string
}

interface UserState {
    userInfo: Nullable<UserInfo>
    token?: string
    expire?: string
    roleList: RoleEnum[]
    permissions: string[]
    sessionTimeout?: boolean
    lastUpdateTime: number
    teamId: string
    teamList: TeamListType[]
}

function getUserTeamId(): string {
    return sessionStorage.getItem(APP_TEAMID_KEY_) || localStorage.getItem(APP_TEAMID_KEY_) || ''
}

export const useUserStore = defineStore('app-user', {
    state: (): UserState => ({
        userInfo: null,
        token: undefined,
        expire: undefined,
        roleList: [],
        permissions: [],
        sessionTimeout: false,
        lastUpdateTime: 0,
        teamId: getUserTeamId(),
        teamList: [],
    }),
    getters: {
        getUserInfo(): UserInfo {
            return this.userInfo || getAuthCache<UserInfo>(USER_INFO_KEY) || ({} as UserInfo)
        },
        getToken(): string {
            return this.token || getAuthCache<string>(TOKEN_KEY)
        },
        getExpire(): string {
            return this.expire || getAuthCache<string>(EXPIRE_KEY)
        },
        getRoleList(): RoleEnum[] {
            return this.roleList?.length > 0 ? this.roleList : getAuthCache<RoleEnum[]>(ROLES_KEY)
        },
        getPermissions(): string[] {
            return this.permissions?.length > 0
                ? this.permissions
                : getAuthCache<string[]>(PERMISSION_KEY)
        },
        getTeamId(): string | undefined {
            return this.teamId
        },
        getTeamList(): TeamListType[] {
            return this.teamList
        },
    },
    actions: {
        setToken(info: string | undefined) {
            this.token = info ? info : ''
            setAuthCache(TOKEN_KEY, info)
        },
        setExpire(info: string | undefined) {
            this.expire = info || ''
            setAuthCache(EXPIRE_KEY, info)
        },
        setRoleList(roleList: RoleEnum[]) {
            this.roleList = roleList || []
            setAuthCache(ROLES_KEY, roleList)
        },
        setUserInfo(info: UserInfo | null) {
            this.userInfo = info
            this.lastUpdateTime = Date.now()
            setAuthCache(USER_INFO_KEY, info)
        },
        setPermissions(permissions: string[] = []) {
            this.permissions = permissions
            setAuthCache(PERMISSION_KEY, permissions)
        },
        setData(data: Recordable) {
            const { token, expire, user, permissions, roles = [] } = data
            this.setToken(token)
            this.setExpire(expire)
            this.setUserInfo(user)
            this.setRoleList(roles)
            this.setPermissions(permissions)
        },
        setTeamList(teamList: TeamListType[]) {
            this.teamList = teamList
        },
        async logout(_goLogin = false) {
            this.setUserInfo(null)
            sessionStorage.removeItem(APP_TEAMID_KEY_)
            sessionStorage.removeItem('appPageNo')
            localStorage.removeItem(APP_TEAMID_KEY_)
            this.setToken(undefined)
            this.setExpire(undefined)
            this.setRoleList([])
            this.setPermissions([])
            this.teamId = ''
            Persistent.removeLocal(TOKEN_KEY, true)
            Persistent.removeLocal(EXPIRE_KEY, true)
            Persistent.removeLocal(USER_INFO_KEY, true)
            Persistent.removeLocal(ROLES_KEY, true)
            Persistent.removeLocal(PERMISSION_KEY, true)
        },
    },
})

export function useUserStoreWithOut() {
    return useUserStore(getPinia())
}
