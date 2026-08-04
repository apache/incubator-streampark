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

import { router } from '@/router'
import {
    fetchSetUserTeam,
    fetchSignin,
    fetchSignout,
    fetchSsoToken,
    fetchUserTeam,
} from '@/service'
import { useRouteStore } from './router'
import { useTabStore } from './tab'
import { APP_TEAMID_KEY_ } from '@/enums/cacheEnum'
import { RoleEnum } from '@/enums/roleEnum'
import { useUserStoreWithOut } from '@/store/modules/user'
import { getToken } from '@/utils/auth'

export const useAuthStore = defineStore('auth-store', {
    state: () => ({
        initialized: false,
        pendingTeamSelect: false,
    }),
    getters: {
        isLogin() {
            return Boolean(getToken())
        },
    },
    actions: {
        async logout() {
            const route = unref(router.currentRoute)
            try {
                if (getToken()) await fetchSignout()
            } catch {
                /* ignore signout errors */
            }
            const userStore = useUserStoreWithOut()
            await userStore.logout(false)
            const routeStore = useRouteStore()
            routeStore.resetRouteStore()
            const tabStore = useTabStore()
            tabStore.clearAllTabs()
            this.pendingTeamSelect = false
            this.$reset()
            if (route.meta.requiresAuth !== false) {
                router.push({
                    name: 'login',
                    query: { redirect: route.fullPath },
                })
            }
        },

        async completeLogin(
            payload: import('@/types/api/system/model/userModel').LoginResultModel,
        ) {
            const userStore = useUserStoreWithOut()
            userStore.setData(payload)

            if (payload.user?.lastTeamId) {
                userStore.teamId = payload.user.lastTeamId
                sessionStorage.setItem(APP_TEAMID_KEY_, payload.user.lastTeamId)
                localStorage.setItem(APP_TEAMID_KEY_, payload.user.lastTeamId)
            }

            const userId = payload.user?.userId
            if (userId) {
                const teamResult = await fetchUserTeam({ userId })
                const teams = teamResult.isSuccess && teamResult.data ? teamResult.data : []
                userStore.setTeamList(
                    teams.map((i: { teamName: string; id: string | number }) => ({
                        label: i.teamName,
                        value: String(i.id),
                    })),
                )

                const hasTeam = Boolean(userStore.teamId || payload.user?.lastTeamId)
                if (!hasTeam && teams.length > 1) {
                    this.pendingTeamSelect = true
                    return false
                }
                if (!hasTeam && teams.length === 1) {
                    await this.switchTeam(String(teams[0].id), userId)
                }
            }

            const routeStore = useRouteStore()
            await routeStore.initAuthRoute()

            const current = unref(router.currentRoute)
            const redirect =
                (current.query.redirect as string) || import.meta.env.VITE_HOME_PATH || '/'
            await router.push(redirect)
            this.pendingTeamSelect = false
            return true
        },

        async switchTeam(teamId: string, userId?: string | number) {
            const userStore = useUserStoreWithOut()
            const uid = userId ?? userStore.getUserInfo?.userId
            const result = await fetchSetUserTeam({ teamId, userId: uid })
            if (!result.isSuccess || !result.data)
                throw new Error(result.message || 'Failed to switch team')

            const { permissions, roles = [], user } = result.data
            userStore.setUserInfo(user as any)
            userStore.setRoleList(roles as RoleEnum[])
            userStore.setPermissions(permissions)
            userStore.teamId = teamId
            sessionStorage.setItem(APP_TEAMID_KEY_, teamId)
            localStorage.setItem(APP_TEAMID_KEY_, teamId)

            const routeStore = useRouteStore()
            routeStore.resetRoutes()
            await routeStore.initAuthRoute()
        },

        async loginWithSso() {
            const result = await fetchSsoToken()
            if (!result.isSuccess || !result.data?.token)
                throw new Error(result.message || 'SSO login failed')
            const done = await this.completeLogin(result.data)
            if (!done) return { needTeamSelect: true }
            return { needTeamSelect: false }
        },

        async login(username: string, password: string, loginType = 'PASSWORD') {
            const result = await fetchSignin({ username, password, loginType })
            if (!result.isSuccess) throw new Error(result.message || 'Login failed')

            const payload =
                result.data as import('@/types/api/system/model/userModel').LoginResultModel
            if (!payload?.token) throw new Error('Login failed: empty token')

            const done = await this.completeLogin(payload)
            if (!done) return { needTeamSelect: true }
            return { needTeamSelect: false }
        },

        async confirmTeam(teamId: string) {
            await this.switchTeam(teamId)
            this.pendingTeamSelect = false
            const current = unref(router.currentRoute)
            const redirect =
                (current.query.redirect as string) || import.meta.env.VITE_HOME_PATH || '/'
            await router.push(redirect)
        },
    },
})
