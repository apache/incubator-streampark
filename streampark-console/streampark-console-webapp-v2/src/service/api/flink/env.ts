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

import type { BasicTableParams } from '@/types/api/model/baseModel'
import type { FlinkCreate, FlinkEnv } from '@/types/api/flink/flinkEnv.type'
import { request } from '../../http'

export function fetchFlinkEnvPage(data: BasicTableParams) {
    return request.Post<FlinkEnv[] | { records?: FlinkEnv[]; total?: number }>(
        '/flink/env/page',
        data,
    )
}

export function fetchListFlinkEnv() {
    return request.Post<FlinkEnv[]>('/flink/env/list')
}

export function fetchFlinkEnvGet(id: string) {
    return request.Post<FlinkEnv>('/flink/env/get', { id })
}

export function fetchDefaultSet(id: string) {
    return request.Post('/flink/env/default', { id })
}

export function fetchFlinkEnvRemove(id: string) {
    return request.Post<{ code?: number; data?: boolean }>('/flink/env/delete', { id })
}

export function fetchCheckEnv(data: { id: string | null; flinkName: string; flinkHome: string }) {
    return request.Post<{ code?: number; data?: number; msg?: string }>('/flink/env/check', data)
}

export function fetchValidity(id: string) {
    return request.Post<{ code?: number; data?: boolean }>('/flink/env/validity', { id })
}

export function fetchFlinkCreate(data: FlinkCreate) {
    return request.Post<{ code?: number; data?: boolean; msg?: string }>('/flink/env/create', data)
}

export function fetchFlinkUpdate(data: FlinkCreate) {
    return request.Post<{ code?: number; data?: boolean; msg?: string }>('/flink/env/update', data)
}

export function fetchFlinkSync(id: string) {
    return request.Post<boolean>('/flink/env/sync', { id })
}
