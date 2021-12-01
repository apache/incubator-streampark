/*
 * Copyright (c) 2021 The StreamX Project
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

export function uploadJars() {
  return http.post(api.FlinkHistory.UPLOAD_JARS)
}

export function k8sNamespaces(params) {
  return http.post(api.FlinkHistory.K8S_NAMESPACES, params)
}

export function sessionClusterIds(params) {
  return http.post(api.FlinkHistory.SESSION_CLUSTER_IDS, params)
}

export function flinkBaseImages(params) {
  return http.post(api.FlinkHistory.FLINK_BASE_IMAGES, params)
}

export function flinkPodTemplates(params) {
  return http.post(api.FlinkHistory.FLINK_POD_TEMPLATES, params)
}

export function flinkJmPodTemplates(params) {
  return http.post(api.FlinkHistory.FLINK_JM_POD_TEMPLATES, params)
}

export function flinkTmPodTemplates(params) {
  return http.post(api.FlinkHistory.FLINK_TM_POD_TEMPLATES, params)
}
