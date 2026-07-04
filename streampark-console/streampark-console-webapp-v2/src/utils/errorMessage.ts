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

import { $t } from '@/utils'
import { ERROR_NO_TIP_STATUS, ERROR_STATUS } from '@/service/http/config'

const STREAMPARK_PREFIX = /^\[StreamPark\]\s*/i

/** Normalize backend / transport error text for UI display. */
export function formatApiErrorMessage(raw: unknown): string {
  if (raw == null)
    return ''
  if (typeof raw === 'string')
    return raw.replace(STREAMPARK_PREFIX, '').trim()
  if (typeof raw === 'object') {
    const record = raw as Recordable
    const nested = record.message ?? record.msg ?? record.error ?? record.errorMessage
    if (nested != null && nested !== raw)
      return formatApiErrorMessage(nested)
  }
  return String(raw).replace(STREAMPARK_PREFIX, '').trim()
}

export function extractRestResponseMessage(
  body: Record<string, unknown> | null | undefined,
  config: Required<Service.BackendConfig>,
): string {
  if (!body)
    return ''
  const { msgKey } = config
  return formatApiErrorMessage(
    body[msgKey] ?? body.message ?? body.msg ?? body.error ?? body.errorMessage,
  )
}

export async function parseResponseJson(response: Response): Promise<Record<string, unknown> | null> {
  try {
    const text = await response.clone().text()
    if (!text.trim())
      return null
    const parsed = JSON.parse(text)
    if (parsed && typeof parsed === 'object')
      return parsed as Record<string, unknown>
  }
  catch {
    // non-JSON error pages
  }
  return null
}

export function resolveHttpStatusMessage(status: number): string {
  const resolver = ERROR_STATUS[status as keyof typeof ERROR_STATUS]
    ?? ERROR_STATUS.default
  return resolver()
}

export function resolveRequestErrorMessage(
  httpStatus: number,
  body: Record<string, unknown> | null,
  config: Required<Service.BackendConfig>,
): string {
  const businessMessage = extractRestResponseMessage(body, config)
  if (businessMessage)
    return businessMessage
  return resolveHttpStatusMessage(httpStatus)
}

export function resolveResultMessage(
  result: Pick<Service.RequestResult<unknown>, 'message' | 'isSuccess'>,
  fallback?: string,
): string {
  const formatted = formatApiErrorMessage(result.message)
  if (formatted)
    return formatted
  return fallback || $t('http.defaultTip')
}

export class ApiRequestError extends Error {
  readonly errorNotified: boolean

  constructor(message: string, errorNotified = false) {
    super(message)
    this.name = 'ApiRequestError'
    this.errorNotified = errorNotified
  }
}

export function throwApiFailure(
  result: Pick<Service.RequestResult<unknown>, 'isSuccess' | 'message' | 'code' | 'errorNotified'>,
  fallback?: string,
): never {
  throw new ApiRequestError(
    resolveResultMessage(result, fallback),
    Boolean(result.errorNotified),
  )
}

/** Present API failure to the user once via message toast. */
export function notifyRequestError(error: Pick<Service.RequestError, 'code' | 'message'>) {
  const code = Number(error.code)
  if (ERROR_NO_TIP_STATUS.includes(code))
    return

  const message = formatApiErrorMessage(error.message) || $t('http.defaultTip')
  if (!message)
    return

  window.$message?.error(message)
}

/** Show API error only when the HTTP layer has not already notified the user. */
export function showResultError(
  result: Pick<Service.RequestResult<unknown>, 'isSuccess' | 'message' | 'code' | 'errorNotified'>,
  fallback?: string,
) {
  if (result.isSuccess || result.errorNotified)
    return

  notifyRequestError({
    code: result.code,
    message: resolveResultMessage(result, fallback),
  })
}

export function showCatchError(error: unknown, fallback?: string) {
  if (error instanceof ApiRequestError && error.errorNotified)
    return
  const message = error instanceof Error
    ? formatApiErrorMessage(error.message)
    : formatApiErrorMessage(error)
  window.$message?.error(message || fallback || $t('http.defaultTip'))
}
