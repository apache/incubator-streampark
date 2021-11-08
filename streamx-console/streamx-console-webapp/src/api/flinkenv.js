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

export function list(params) {
  return http.post(api.FlinkEnv.LIST, params)
}

export function exists(params) {
  return http.post(api.FlinkEnv.EXISTS, params)
}

export function get(params) {
  return http.post(api.FlinkEnv.GET, params)
}

export function sync(params) {
  return http.post(api.FlinkEnv.SYNC, params)
}

export function create(params) {
  return http.post(api.FlinkEnv.CREATE, params)
}

export function update(params) {
  return http.post(api.FlinkEnv.UPDATE, params)
}

export function setDefault(params) {
  return http.post(api.FlinkEnv.DEFAULT, params)
}
