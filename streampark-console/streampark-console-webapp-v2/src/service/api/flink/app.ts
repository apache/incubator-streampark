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

import type {
    AppListResponse,
    CancelParam,
    CreateParams,
    DashboardResponse,
} from '@/types/api/flink/app.type'
import { request } from '../../http'

export function fetchAppConf(params?: { config?: unknown }) {
    return request.Post<Recordable>('/flink/app/read_conf', params ?? {})
}

export function fetchDashboard() {
    return request.Post<DashboardResponse>('/flink/app/dashboard', {})
}

export function fetchAppRecord(data: Recordable) {
    return request.Post<AppListResponse>('/flink/app/list', data)
}

export function fetchAppRemove(id: string) {
    return request.Post<boolean>('/flink/app/delete', { id })
}

export function fetchRemoveBackup(id: string) {
    return request.Post<boolean>('/flink/app/delete/backup', { id })
}

export function fetchYarn() {
    return request.Post<string>('/flink/app/yarn')
}

export function fetchCheckName(data: { id?: string; jobName: string }) {
    return request.Post<number>('/flink/app/check/name', data)
}

export function fetchMain(data: Recordable) {
    return request.Post<string>('/flink/app/main', data)
}

export function fetchAppCreate(data: CreateParams) {
    return request.Post<{ data?: boolean; message?: string }>('/flink/app/create', data)
}

export function fetchAppUpdate(data: Recordable) {
    return request.Post<{ data?: boolean; message?: string }>('/flink/app/update', data)
}

export function fetchAppGet(data: { id: string }) {
    return request.Post<Recordable>('/flink/app/get', data)
}

export function fetchBackUps(data: Recordable) {
    return request.Post<Recordable[]>('/flink/app/backups', data)
}

export function fetchOptionLog(data: Recordable) {
    return request.Post<Recordable[]>('/flink/app/opt_log', data)
}

export function fetchDeleteOperationLog(id: string) {
    return request.Post<boolean>('/flink/app/delete/opt_log', { id })
}

export function fetchAbort(data: { id: string }) {
    return request.Post<boolean>('/flink/app/abort', data)
}

export function fetchStart(data: Recordable) {
    return request.Post<{ data?: boolean; message?: string }>('/flink/app/start', data)
}

export function fetchCopy(data: Recordable) {
    return request.Post<{ data?: boolean; message?: string }>('/flink/app/copy', data)
}

export function fetchMapping(data: Recordable) {
    return request.Post<boolean>('/flink/app/mapping', data)
}

export function fetchK8sStartLog(data: Recordable) {
    return request.Post<Recordable>('/flink/app/k8s_log', data)
}

export function fetchCheckSavepointPath(data: { id?: string; savepointPath?: string }) {
    return request.Post<{ data?: boolean; message?: string }>(
        '/flink/app/check/savepoint_path',
        data,
    )
}

export function fetchCancel(data: CancelParam) {
    return request.Post<boolean>('/flink/app/cancel', data)
}

export function fetchName(data: { config: string }) {
    return request.Post<string>('/flink/app/name', data)
}

export function fetchCheckStart(data: Recordable) {
    return request.Post<number>('/flink/app/check/start', data)
}
