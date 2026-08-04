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

import type { ErrorLogInfo } from '/#/store'
import { formatToDateTime } from '@/utils/dateUtil'
import { ErrorTypeEnum } from '@/enums/exceptionEnum'

export interface ErrorLogState {
    errorLogInfoList: Nullable<ErrorLogInfo[]>
    errorLogListCount: number
}

export const useErrorLogStore = defineStore('app-error-log', {
    state: (): ErrorLogState => ({
        errorLogInfoList: null,
        errorLogListCount: 0,
    }),
    getters: {
        getErrorLogInfoList(): ErrorLogInfo[] {
            return this.errorLogInfoList || []
        },
        getErrorLogListCount(): number {
            return this.errorLogListCount
        },
    },
    actions: {
        addErrorLogInfo(info: ErrorLogInfo) {
            const item = {
                ...info,
                time: formatToDateTime(Date.now()),
            }
            this.errorLogInfoList = [item, ...(this.errorLogInfoList || [])]
            this.errorLogListCount += 1
        },
        setErrorLogListCount(count: number) {
            this.errorLogListCount = count
        },
        clearErrorLog() {
            this.errorLogInfoList = null
            this.errorLogListCount = 0
        },
    },
})

export function setupErrorHandle(app: import('vue').App) {
    app.config.errorHandler = (err, instance, info) => {
        const errorLogStore = useErrorLogStore()
        errorLogStore.addErrorLogInfo({
            type: ErrorTypeEnum.VUE,
            name: (err as Error)?.name,
            file: instance?.$?.type?.name || '',
            message: (err as Error)?.message || String(err),
            stack: (err as Error)?.stack,
            detail: info,
            url: window.location.href,
        })
    }
}
