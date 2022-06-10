/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import api from './index'
import http from '@/utils/request'

export function getExecUser () {
  return http.post(api.User.EXECUSER)
}

export function list (queryParam) {
  return http.post(api.User.LIST, queryParam)
}

export function getNoTokenUser(queryParam) {
  return http.post(api.User.GET_NOTOKEN_USER, queryParam)
}

export function update (queryParam) {
  return http.put(api.User.UPDATE, queryParam)
}

export function password (queryParam) {
  return http.put(api.User.PASSWORD, queryParam)
}

export function reset (queryParam) {
  return http.put(api.User.RESET, queryParam)
}

export function get (queryParam) {
  return http.get(api.User.GET, queryParam)
}

export function checkUserName (queryParam) {
  return http.post(api.User.CHECK_NAME, queryParam)
}

export function post (queryParam) {
  return http.post(api.User.POST, queryParam)
}

export function getRouter (queryParam) {
  return http.post(api.Menu.ROUTER, queryParam)
}

export function deleteUser (queryParam) {
  return http.delete(api.User.DELETE, queryParam)
}
