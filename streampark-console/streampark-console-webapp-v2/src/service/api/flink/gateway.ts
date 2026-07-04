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

import { request } from '../../http'

export interface FlinkGatewayRecord {
  id: string
  gatewayName: string
  gatewayType?: string
  address: string
  description?: string
  createTime?: string
  modifyTime?: string
}

export function fetchGatewayList() {
  return request.Get<FlinkGatewayRecord[]>('/flink/gateway/list')
}

export function fetchGatewayCreate(data: Recordable) {
  return request.Post('/flink/gateway/create', data)
}

export function fetchGatewayUpdate(data: Recordable) {
  return request.Post('/flink/gateway/update', data)
}

export function fetchGatewayDelete(data: { id: string }) {
  return request.Delete('/flink/gateway/delete', data)
}

export function fetchGatewayCheckName(params: { name: string }) {
  const method = request.Get<boolean>('/flink/gateway/check/name')
  method.config.params = params
  return method
}

export function fetchGatewayCheckAddress(params: { address: string }) {
  const method = request.Get<boolean>('/flink/gateway/check/address')
  method.config.params = params
  return method
}
