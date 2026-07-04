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

import type { SparkApplication } from '@/types/api/spark/app.type'
import type { SparkApplicationConfig } from '@/types/api/spark/conf.type'
import { request } from '../../http'

export function fetchGetSparkConf(data: { id: number | string }) {
  return request.Post<SparkApplicationConfig>('/spark/conf/get', data)
}

export function fetchSparkConfTemplate() {
  return request.Post<string>('/spark/conf/template')
}

export function fetchSparkConfList(data: Recordable) {
  return request.Post<{ total: number, records: SparkApplicationConfig[] }>('/spark/conf/list', data)
}

export function fetchSparkConfHistory(data: SparkApplication) {
  return request.Post<SparkApplicationConfig[]>('/spark/conf/history', data)
}

export function fetchSparkConfRemove(data: { id: number }) {
  return request.Post<boolean>('/spark/conf/delete', data)
}

export function fetchSysHadoopConf() {
  return request.Post<Recordable>('/spark/conf/sysHadoopConf')
}
