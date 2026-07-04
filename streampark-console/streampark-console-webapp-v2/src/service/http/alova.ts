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

import { getToken } from '@/utils/auth'
import { APP_TEAMID_KEY_ } from '@/enums/cacheEnum'
import qs from 'qs'
import { createAlova } from 'alova'
import { createServerTokenAuthentication } from 'alova/client'
import adapterFetch from 'alova/fetch'
import VueHook from 'alova/vue'
import type { VueHookType } from 'alova/vue'
import {
  DEFAULT_ALOVA_OPTIONS,
  DEFAULT_BACKEND_OPTIONS,
  isBackendSuccess,
} from './config'
import {
  buildBusinessError,
  buildResponseError,
  finalizeRequestError,
  handleRefreshToken,
  handleServiceResult,
  parseResponseJson,
  showTransportError,
} from './handle'

const { onAuthRequired, onResponseRefreshToken } = createServerTokenAuthentication<VueHookType>({
  refreshTokenOnSuccess: {
    isExpired: async (response, method) => {
      const res = await response.clone().json()
      const isExpired = method.meta && method.meta.isExpired
      return (response.status === 401 || res.code === 401) && !isExpired
    },
    handler: async (_response, method) => {
      if (!method.meta)
        method.meta = { isExpired: true }
      else
        method.meta.isExpired = true
      await handleRefreshToken()
    },
  },
  assignToken: (method) => {
    const token = getToken()
    if (token)
      method.config.headers.Authorization = token
    const teamId = localStorage.getItem(APP_TEAMID_KEY_) || sessionStorage.getItem(APP_TEAMID_KEY_)
    if (teamId)
      method.config.headers['Team-Id'] = teamId
  },
})

export function createAlovaInstance(
  alovaConfig: Service.AlovaConfig,
  backendConfig?: Service.BackendConfig,
) {
  const _backendConfig = { ...DEFAULT_BACKEND_OPTIONS, ...backendConfig }
  const _alovaConfig = { ...DEFAULT_ALOVA_OPTIONS, ...alovaConfig }

  return createAlova({
    statesHook: VueHook,
    requestAdapter: adapterFetch(),
    cacheFor: null,
    baseURL: _alovaConfig.baseURL,
    timeout: _alovaConfig.timeout,
    beforeRequest: onAuthRequired((method) => {
      const useJson = method.meta?.isJsonPost === true
      if (method.type === 'POST' && !useJson) {
        if (method.data == null)
          method.data = {}
        method.config.headers['Content-Type'] = 'application/x-www-form-urlencoded'
        const teamId = localStorage.getItem(APP_TEAMID_KEY_) || sessionStorage.getItem(APP_TEAMID_KEY_)
        if (teamId && typeof method.data === 'object' && !(method.data instanceof URLSearchParams)) {
          const payload = method.data as Record<string, unknown>
          if (payload.teamId == null)
            payload.teamId = Number.parseInt(teamId, 10)
        }
        if (typeof method.data === 'object' && !(method.data instanceof URLSearchParams)) {
          method.data = qs.stringify(method.data, { arrayFormat: 'brackets' })
        }
      }
      alovaConfig.beforeRequest?.(method)
    }),
    responded: onResponseRefreshToken({
      onSuccess: async (response, method) => {
        const { status } = response
        if (status === 200 && method.meta?.isBlob)
          return response.blob()

        const apiData = await parseResponseJson(response)

        if (status === 200) {
          if (apiData && isBackendSuccess(apiData, _backendConfig))
            return handleServiceResult(apiData)
          if (apiData)
            return finalizeRequestError(buildBusinessError(apiData, _backendConfig), method)
          return finalizeRequestError(buildResponseError(response, null, _backendConfig), method)
        }

        return finalizeRequestError(buildResponseError(response, apiData, _backendConfig), method)
      },
      onError: (error, method) => {
        showTransportError(error, method)
      },
      onComplete: async () => {},
    }),
  })
}
