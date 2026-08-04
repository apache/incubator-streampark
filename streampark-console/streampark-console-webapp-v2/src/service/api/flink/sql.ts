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

export function fetchFlinkSqlVerify(data: Recordable) {
    return request.Post<{ code?: number; data?: Recordable; msg?: string }>(
        '/flink/sql/verify',
        data,
    )
}

export function fetchFlinkSql(data: Recordable) {
    return request.Post<Recordable>('/flink/sql/get', data)
}

export function fetchFlinkSqlList(data: Recordable) {
    return request.Post<Recordable[] | { records?: Recordable[]; total?: number }>(
        '/flink/sql/list',
        data,
    )
}

export function fetchRemoveFlinkSql(data: { appId: string; id: string }) {
    return request.Post<boolean>('/flink/sql/delete', data)
}

export function fetchFlinkSqlHistory(data: Recordable) {
    return request.Post<Recordable[]>('/flink/sql/history', data)
}
