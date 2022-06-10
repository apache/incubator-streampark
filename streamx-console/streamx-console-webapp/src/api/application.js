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

export function readConf (params) {
  return http.post(api.Application.READCONF, params)
}

export function get (params) {
  return http.post(api.Application.GET, params)
}

export function dashboard (params) {
  return http.post(api.Application.DASHBOARD, params)
}

export function main (params) {
  return http.post(api.Application.MAIN, params)
}

export function update (params) {
  return http.post(api.Application.UPDATE, params)
}

export function upload (params) {
  return http.upload(api.Application.UPLOAD, params)
}

export function deploy (params) {
  return http.post(api.Application.DEPLOY, params)
}

export function mapping (params) {
  return http.post(api.Application.MAPPING, params)
}

export function yarn (params) {
  return http.post(api.Application.YARN, params)
}

export function list (params) {
  return http.post(api.Application.LIST, params)
}

export function name (params) {
  return http.post(api.Application.NAME, params)
}

export function checkName (params) {
  return http.post(api.Application.CHECKNAME, params)
}

export function cancel (params) {
  return http.post(api.Application.CANCEL, params)
}

export function create (params) {
  return http.post(api.Application.CREATE, params)
}

export function remove (params) {
  return http.post(api.Application.DELETE, params)
}

export function removeBak (params) {
  return http.post(api.Application.DELETEBAK, params)
}

export function start (params) {
  return http.post(api.Application.START, params)
}

export function clean (params) {
  return http.post(api.Application.CLEAN, params)
}

export function backUps (params) {
  return http.post(api.Application.BACKUPS, params)
}

export function rollback (params) {
  return http.post(api.Application.ROLLBACK, params)
}

export function revoke (params) {
  return http.post(api.Application.REVOKE, params)
}

export function optionLog (params) {
  return http.post(api.Application.OPTIONLOG, params)
}

export function downLog (params) {
  return http.post(api.Application.DOWNLOG, params)
}

export function checkJar(params) {
  return http.post(api.Application.CHECKJAR, params)
}

export function verifySchema(params) {
  return http.post(api.Application.VERIFYSCHEMA, params)
}
