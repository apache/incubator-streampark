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

import type { AppListResponse, DashboardResponse, SparkApplication } from '@/types/api/spark/app.type'
import { request } from '../../http'

export function fetchSparkAppGet(data: { id: string }) {
  return request.Post<SparkApplication>('/spark/app/get', data)
}

export function fetchSparkAppCreate(data: SparkApplication) {
  return request.Post<boolean>('/spark/app/create', data)
}

export function fetchSparkAppCopy(data: SparkApplication) {
  return request.Post<{ data?: boolean, message?: string }>('/spark/app/copy', data)
}

export function fetchSparkAppUpdate(data: SparkApplication) {
  return request.Post<boolean>('/spark/app/update', data)
}

export function fetchSparkDashboard() {
  return request.Post<DashboardResponse>('/spark/app/dashboard')
}

export function fetchSparkAppRecord(data: Recordable) {
  return request.Post<AppListResponse>('/spark/app/list', data)
}

export function fetchSparkMapping(data: SparkApplication) {
  return request.Post<boolean>('/spark/app/mapping', data)
}

export function fetchSparkAppStart(data: SparkApplication) {
  return request.Post<{ data?: boolean, message?: string }>('/spark/app/start', data)
}

export function fetchCheckSparkAppStart(data: SparkApplication) {
  return request.Post<number>('/spark/app/check/start', data)
}

export function fetchSparkAppCancel(data: SparkApplication) {
  return request.Post<boolean>('/spark/app/cancel', data)
}

export function fetchSparkAppClean(data: SparkApplication) {
  return request.Post<boolean>('/spark/app/clean', data)
}

export function fetchSparkAppForcedStop(data: SparkApplication) {
  return request.Post<boolean>('/spark/app/forcedStop', data)
}

export function fetchSparkYarn() {
  return request.Post<string>('/spark/app/yarn')
}

export function fetchCheckSparkName(data: { id?: string, appName: string }) {
  return request.Post<number>('/spark/app/check/name', data)
}

export function fetchSparkAppConf(params?: { config?: unknown }) {
  return request.Post<string>('/spark/app/read_conf', params ?? {})
}

export function fetchSparkMain(data: SparkApplication) {
  return request.Post<string>('/spark/app/main', data)
}

export function fetchSparkBackUps(data: SparkApplication) {
  return request.Post<Recordable[]>('/spark/app/backups', data)
}

export function fetchSparkOptionLog(data: SparkApplication) {
  return request.Post<Recordable[]>('/spark/app/opt_log', data)
}

export function fetchSparkDeleteOptLog(id: string) {
  return request.Post<boolean>('/spark/app/delete/opt_log', { id })
}

export function fetchSparkAppRemove(id: string) {
  return request.Post<boolean>('/spark/app/delete', { id })
}

export function fetchSparkRemoveBackup(id: string) {
  return request.Post<boolean>('/spark/app/delete/bak', { id })
}
