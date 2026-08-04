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

import { useAuthStore } from '@/store'
import { $t } from '@/utils'
import {
    extractRestResponseMessage,
    formatApiErrorMessage,
    notifyRequestError,
    resolveRequestErrorMessage,
} from '@/utils/errorMessage'

export { notifyRequestError, parseResponseJson } from '@/utils/errorMessage'

export function buildBusinessError(
    data: Record<string, unknown>,
    config: Required<Service.BackendConfig>,
): Service.RequestError {
    const { codeKey, dataKey } = config
    return {
        errorType: 'Business Error',
        code: data[codeKey] as Service.RequestCode,
        message: extractRestResponseMessage(data, config),
        data: data[dataKey],
    }
}

export function buildResponseError(
    response: Response,
    body: Record<string, unknown> | null,
    config: Required<Service.BackendConfig>,
): Service.RequestError {
    const message = resolveRequestErrorMessage(response.status, body, config)
    const businessCode = body?.[config.codeKey]
    return {
        errorType: body && businessCode != null ? 'Business Error' : 'Response Error',
        code: (businessCode ?? response.status) as Service.RequestCode,
        message,
        data: body?.[config.dataKey] ?? null,
    }
}

export function finalizeRequestError(
    error: Service.RequestError,
    method: { meta?: Recordable },
): Service.RequestResult<unknown> {
    const silent = method.meta?.silentError === true
    if (!silent) notifyRequestError(error)
    return handleServiceResult({ ...error, errorNotified: !silent }, false)
}

export function handleServiceResult<T = unknown>(
    data: Service.RequestError | Record<string, unknown>,
    isSuccess = true,
): Service.RequestResult<T> {
    const errorNotified = !isSuccess && Boolean((data as Recordable).errorNotified)
    return {
        isSuccess,
        errorType: isSuccess
            ? null
            : ((data.errorType as Service.RequestErrorType) ?? 'Business Error'),
        code: data.code as Service.RequestCode,
        message: formatApiErrorMessage(data.message) || String(data.message ?? ''),
        data: data.data as T,
        errorNotified,
    }
}

export async function handleRefreshToken() {
    await useAuthStore().logout()
}

export function showTransportError(error: Error, method: { type: string; url: string }) {
    const text = error.message || ''
    let message = $t('http.defaultTip')
    if (text.includes('timeout') || text.includes('Timeout'))
        message = $t('sys.api.apiTimeoutMessage')
    else if (text.includes('NetworkError') || text.includes('Failed to fetch'))
        message = $t('sys.api.networkExceptionMsg')
    else if (text) message = text

    window.$message?.error(message)
    console.warn(`[${method.type}] ${method.url}`, error)
}
