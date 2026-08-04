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

import type { TokenCreateParam, TokenListRecord } from '@/types/api/system/model/tokenModel'
import type { BasicTableParams } from '@/types/api/model/baseModel'
import { request } from '../http'

export function fetchTokenList(data?: BasicTableParams & { username?: string }) {
    return request.Post<TokenListRecord[]>('/token/list', data ?? {})
}

export function fetchTokenCreate(data: TokenCreateParam) {
    return request.Post<TokenListRecord>('/token/create', data)
}

export function fetchTokenStatusToggle(data: { tokenId: string }) {
    return request.Post<boolean>('/token/toggle', data)
}

export function fetchTokenDelete(data: { tokenId: string }) {
    return request.Delete<boolean>('/token/delete', data)
}

export function fetchCheckToken(data?: Recordable) {
    return request.Post<number>('/token/check', data ?? {})
}
