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
  return http.post(api.FlinkCluster.LIST, params)
}

export function activeURL(params) {
  return http.post(api.FlinkCluster.ACTIVEURL, params)
}

export function check(params) {
  return http.post(api.FlinkCluster.CHECK, params)
}

export function get(params) {
  return http.post(api.FlinkCluster.GET, params)
}

export function create(params) {
  return http.post(api.FlinkCluster.CREATE, params)
}

export function update(params) {
  return http.post(api.FlinkCluster.UPDATE, params)
}

export function start(params) {
  return http.post(api.FlinkCluster.START, params)
}

export function shutdown(params) {
  return http.post(api.FlinkCluster.SHUTDOWN, params)
}

export function remove(params) {
  return http.post(api.FlinkCluster.DELETE, params)
}
