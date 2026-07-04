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

export interface BooleanActionResult {
  ok: boolean
  message?: string
}

/**
 * Normalize StreamPark boolean action responses.
 * Alova exposes RestResponse `data` directly (e.g. `true`), while legacy
 * callers may still receive nested `{ data: boolean, message?: string }`.
 */
export function unwrapBooleanResult(data: unknown, fallbackMessage?: string): BooleanActionResult {
  if (typeof data === 'boolean')
    return { ok: data, message: fallbackMessage }
  if (data == null)
    return { ok: true, message: fallbackMessage }
  if (typeof data === 'object') {
    const record = data as Recordable
    const message = (record.message ?? record.msg ?? fallbackMessage) as string | undefined
    if (typeof record.data === 'boolean')
      return { ok: record.data, message }
    if ('data' in record)
      return { ok: Boolean(record.data), message }
  }
  return { ok: Boolean(data), message: fallbackMessage }
}
