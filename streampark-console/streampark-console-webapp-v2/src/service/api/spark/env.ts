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

import type { SparkCreate, SparkEnv } from '@/types/api/spark/home.type'
import { request } from '../../http'

export function fetchSparkEnvList() {
  return request.Post<SparkEnv[]>('/spark/env/list')
}

export function fetchSparkEnv(id: string) {
  return request.Post<SparkEnv>('/spark/env/get', { id })
}

export function fetchSparkSetDefault(id: string) {
  return request.Post('/spark/env/default', { id })
}

export function fetchSparkEnvRemove(id: string) {
  return request.Post<boolean>('/spark/env/delete', { id })
}

export function fetchSparkEnvCheck(data: { id: string | null, sparkName: string, sparkHome: string }) {
  return request.Post<number>('/spark/env/check', data)
}

export function fetchSparkEnvCreate(data: SparkCreate) {
  return request.Post<{ code?: number, data?: boolean, msg?: string }>('/spark/env/create', data)
}

export function fetchSparkEnvUpdate(data: SparkCreate) {
  return request.Post<{ code?: number, data?: boolean, msg?: string }>('/spark/env/update', data)
}

export function fetchSparkSync(id: string) {
  return request.Post<boolean>('/spark/env/sync', { id })
}
