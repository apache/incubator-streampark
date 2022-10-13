/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import api from './index'
import http from '@/utils/request'

export function post (queryParam) {
  return http.post(api.Team.POST, queryParam)
}

export function update (queryParam) {
  return http.put(api.Team.UPDATE, queryParam)
}

export function list (queryParam) {
  return http.post(api.Team.LIST, queryParam)
}

export function users (queryParam) {
  return http.post(api.Team.USERS, queryParam)
}

export function checkName (queryParam) {
  return http.post(api.Team.CHECK_NAME, queryParam)
}

export function remove (queryParam) {
  return http.delete(api.Team.DELETE, queryParam)
}
