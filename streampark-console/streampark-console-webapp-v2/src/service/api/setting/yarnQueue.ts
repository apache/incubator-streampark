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
import { request } from '../../http'

export interface YarnQueueRecord {
    id: string
    queueLabel: string
    description?: string
    createTime?: string
    modifyTime?: string
}

export function fetchYarnQueueList(data: BasicTableParams & { queueLabel?: string }) {
    return request.Post<YarnQueueRecord[]>('/yarn/queue/list', data)
}

export function fetchYarnQueueCreate(data: Recordable) {
    return request.Post('/yarn/queue/create', data)
}

export function fetchYarnQueueUpdate(data: Recordable) {
    return request.Post('/yarn/queue/update', data)
}

export function fetchYarnQueueDelete(data: { id: string }) {
    return request.Post<{ status: string }>('/yarn/queue/delete', data)
}

export function fetchCheckYarnQueue(data: Recordable) {
    return request.Post<{ status: number }>('/yarn/queue/check', data)
}
