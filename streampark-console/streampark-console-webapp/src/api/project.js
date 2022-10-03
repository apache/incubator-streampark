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

export function branches (params) {
  return http.post(api.Project.BRANCHES, params)
}

export function gitcheck (params) {
  return http.post(api.Project.GIT_CHECK, params)
}

export function exists (params) {
  return http.post(api.Project.EXISTS, params)
}

export function create (params) {
  return http.post(api.Project.CREATE, params)
}

export function get(params) {
  return http.post(api.Project.GET, params)
}

export function update(params) {
  return http.post(api.Project.UPDATE, params)
}

export function list (params) {
  return http.post(api.Project.LIST, params)
}

export function build (params) {
  return http.post(api.Project.BUILD, params)
}

export function buildlog (params) {
  return http.post(api.Project.BUILD_LOG, params)
}

export function fileList (params) {
  return http.post(api.Project.FILE_LIST, params)
}

export function modules (params) {
  return http.post(api.Project.MODULES, params)
}

export function listConf (params) {
  return http.post(api.Project.LIST_CONF, params)
}

export function jars (params) {
  return http.post(api.Project.JARS, params)
}

export function remove (params) {
  return http.post(api.Project.DELETE, params)
}

export function select (params) {
  return http.post(api.Project.SELECT, params)
}
