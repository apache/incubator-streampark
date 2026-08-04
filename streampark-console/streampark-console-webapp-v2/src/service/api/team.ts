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

import type { TeamListRecord, TeamParam } from '@/types/api/system/model/teamModel'
import type { BasicTableParams } from '@/types/api/model/baseModel'
import { request } from '../http'

export function fetchTeamList(data: BasicTableParams & { teamName?: string }) {
    return request.Post<TeamListRecord[]>('/team/list', data)
}

export function fetchTeamCreate(data: TeamParam) {
    return request.Post('/team/post', data)
}

export function fetchTeamUpdate(data: TeamParam) {
    return request.Put('/team/update', data)
}

export function fetchTeamDelete(params: { id: string }) {
    const method = request.Delete<{ status: string }>('/team/delete')
    method.config.params = params
    return method
}
