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
import type { FlinkCluster } from '@/types/api/flink/flinkCluster.type'
import { request } from '../../http'

export function fetchFlinkClusterPage(data: BasicTableParams) {
  return request.Post<FlinkCluster[] | { records?: FlinkCluster[], total?: number }>('/flink/cluster/page', data)
}

export function fetchFlinkClusterList() {
  return request.Post<FlinkCluster[]>('/flink/cluster/list')
}

export function fetchClusterStart(id: string) {
  return request.Post<{ code?: number, msg?: string }>('/flink/cluster/start', { id })
}

export function fetchClusterRemove(id: string) {
  return request.Post<{ code?: number, msg?: string }>('/flink/cluster/delete', { id })
}

export function fetchClusterShutdown(id: string) {
  return request.Post<{ code?: number, msg?: string }>('/flink/cluster/shutdown', { id })
}

export function fetchRemoteURL(id: string) {
  return request.Post<string>('/flink/cluster/remote_url', { id })
}

export function fetchCheckCluster(data: Recordable) {
  return request.Post<{ status?: number, msg?: string }>('/flink/cluster/check', data)
}

export function fetchCreateCluster(data: Recordable) {
  return request.Post<boolean>('/flink/cluster/create', data)
}

export function fetchUpdateCluster(data: Recordable) {
  return request.Post<boolean>('/flink/cluster/update', data)
}

export function fetchGetCluster(data: Recordable) {
  return request.Post<FlinkCluster>('/flink/cluster/get', data)
}

export function fetchK8sNamespaces() {
  return request.Post<string[]>('/flink/history/k8s_namespaces')
}

export function fetchSessionClusterIds(data: Recordable) {
  return request.Post<string[]>('/flink/history/session_cluster_ids', data)
}

export function fetchFlinkBaseImages() {
  return request.Post<string[]>('/flink/history/flink_base_images')
}
