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

export function fetchSparkSqlVerify(data: Recordable) {
  return request.Post<Recordable>('/spark/sql/verify', data)
}

export function fetchSparkSqlList(data: Recordable) {
  return request.Post<Recordable[] | { records?: Recordable[], total?: number }>('/spark/sql/list', data)
}

export function fetchSparkSqlRemove(data: { id: string, appId: string }) {
  return request.Post<boolean>('/spark/sql/delete', data)
}

export function fetchSparkSql(data: Recordable) {
  return request.Post<Recordable>('/spark/sql/get', data)
}

export function fetchSparkSqlHistory(data: Recordable) {
  return request.Post<Recordable[]>('/spark/sql/history', data)
}
