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
import http from '@/utils/request'

const api = {
  user: '/user',
  role: '/role',
  service: '/service',
  permission: '/permission',
  permissionNoPager: '/permission/no-pager',
  orgTree: '/org/tree'
}

export default api

export function getUserList (parameter) {
  return http.get({
    url: api.user,
    params: parameter
  })
}

export function getRoleList (parameter) {
  return http.get({
    url: api.role,
    params: parameter
  })
}

export function getServiceList (parameter) {
  return http.get({
    url: api.service,
    params: parameter
  })
}

export function getPermissions (parameter) {
  return http.get({
    url: api.permissionNoPager,
    params: parameter
  })
}

export function getOrgTree (parameter) {
  return http.get({
    url: api.orgTree,
    params: parameter
  })
}

// id === 0 add     post
// id !== 0 update  put
export function saveService (parameter) {
  return http[parameter.id === 0 ? 'post' : 'put']({
    url: api.service,
    data: parameter
  })
}
