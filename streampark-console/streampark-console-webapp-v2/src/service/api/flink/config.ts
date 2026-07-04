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

import type { HadoopConf } from '@/types/api/flink/config.type'
import { request } from '../../http'

export function fetchGetVer(data: { id: string, appId?: string }) {
  return request.Post<Recordable>('/flink/conf/get', data)
}

export function fetchConfTemplate() {
  return request.Post<string>('/flink/conf/template')
}

export function fetchSysHadoopConf() {
  return request.Post<HadoopConf>('/flink/conf/sys_hadoop_conf')
}

export function fetchListVer(data: Recordable) {
  return request.Post<Recordable[]>('/flink/conf/list', data)
}

export function fetchRemoveConf(data: { id: string }) {
  return request.Post<boolean>('/flink/conf/delete', data)
}

export function fetchConfHistory(data: Recordable) {
  return request.Post<Recordable[]>('/flink/conf/history', data)
}
