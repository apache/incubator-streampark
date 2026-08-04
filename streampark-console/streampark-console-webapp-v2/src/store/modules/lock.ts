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

import type { LockInfo } from '/#/store'
import { getPinia } from '@/store/index'
import { fetchSignin } from '@/service'
import { LOCK_INFO_KEY } from '@/enums/cacheEnum'
import { Persistent } from '@/utils/cache/persistent'
import { useUserStore } from './user'

interface LockState {
    lockInfo: Nullable<LockInfo>
}

export const useLockStore = defineStore('app-lock', {
    state: (): LockState => ({
        lockInfo: Persistent.getLocal(LOCK_INFO_KEY),
    }),
    getters: {
        getLockInfo(): Nullable<LockInfo> {
            return this.lockInfo
        },
    },
    actions: {
        setLockInfo(info: LockInfo) {
            this.lockInfo = Object.assign({}, this.lockInfo, info)
            Persistent.setLocal(LOCK_INFO_KEY, this.lockInfo, true)
        },
        resetLockInfo() {
            Persistent.removeLocal(LOCK_INFO_KEY, true)
            this.lockInfo = null
        },
        async unLock(password?: string) {
            const userStore = useUserStore()
            if (this.lockInfo?.pwd === password) {
                this.resetLockInfo()
                return true
            }
            try {
                const username = userStore.getUserInfo?.username
                const loginType = userStore.getUserInfo?.loginType
                const result = await fetchSignin({
                    username: username!,
                    password: password!,
                    loginType: loginType!,
                })
                if (result.isSuccess && result.data) {
                    this.resetLockInfo()
                    return true
                }
                return false
            } catch {
                return false
            }
        },
    },
})

export function useLockStoreWithOut() {
    return useLockStore(getPinia())
}
