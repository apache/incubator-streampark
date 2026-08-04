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
import type {
    ResourceDeleteParam,
    ResourceListRecord,
    ResourceParam,
} from '@/types/api/resource/upload/model/resourceModel'
import { request } from '../../http'

export function fetchResourceList(data: BasicTableParams) {
    return request.Post<ResourceListRecord[] | { records?: ResourceListRecord[]; total?: number }>(
        '/resource/page',
        data,
    )
}

export function fetchAddResource(data: ResourceParam) {
    return request.Post<boolean>('/resource/add', data)
}

export function fetchUpdateResource(data: ResourceParam) {
    return request.Put<boolean>('/resource/update', data)
}

export function fetchResourceDelete(data: ResourceDeleteParam) {
    return request.Delete<{ status?: string; message?: string }>('/resource/delete', data)
}

export function fetchTeamResource(data: Recordable = {}) {
    return request.Post<ResourceListRecord[]>('/resource/list', data)
}

export function checkResource(data: ResourceParam & Recordable) {
    return request.Post<Recordable>('/resource/check', data)
}

export function fetchUpload(formData: FormData) {
    return request.Post<{ path: string; mainClass?: string }>('/resource/upload', formData, {
        timeout: 1000 * 60 * 10,
    })
}

export function fetchUploadJars() {
    return request.Post<string[]>('/resource/upload_jars')
}
