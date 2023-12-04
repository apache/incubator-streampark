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
import { defHttp } from '/@/utils/http/axios';

enum HISTORY_API {
  UPLOAD_JARS = '/flink/history/uploadJars',
  K8S_NAMESPACES = '/flink/history/k8sNamespaces',
  SESSION_CLUSTER_IDS = '/flink/history/sessionClusterIds',
  FLINK_BASE_IMAGES = '/flink/history/flinkBaseImages',
  FLINK_POD_TEMPLATES = '/flink/history/flinkPodTemplates',
  FLINK_JM_POD_TEMPLATES = '/flink/history/flinkJmPodTemplates',
  FLINK_TM_POD_TEMPLATES = '/flink/history/flinkTmPodTemplates',
}

/**
 * get k8s
 * @returns {Promise<string[]>}
 */
export function fetchK8sNamespaces(): Promise<string[]> {
  return defHttp.post({ url: HISTORY_API.K8S_NAMESPACES });
}

/**
 * upload jar
 * @returns {Promise<string[]>}
 */
export function fetchUploadJars(): Promise<string[]> {
  return defHttp.post({ url: HISTORY_API.UPLOAD_JARS });
}

export function fetchSessionClusterIds(data) {
  return defHttp.post({ url: HISTORY_API.SESSION_CLUSTER_IDS, data });
}

export function fetchFlinkBaseImages() {
  return defHttp.post({ url: HISTORY_API.FLINK_BASE_IMAGES });
}

export function fetchFlinkPodTemplates(data: Recordable): Promise<string[]> {
  return defHttp.post({ url: HISTORY_API.FLINK_POD_TEMPLATES, data });
}

export function fetchFlinkJmPodTemplates(data: Recordable): Promise<string[]> {
  return defHttp.post({ url: HISTORY_API.FLINK_JM_POD_TEMPLATES, data });
}

export function fetchFlinkTmPodTemplates(data: Recordable): Promise<string[]> {
  return defHttp.post({ url: HISTORY_API.FLINK_TM_POD_TEMPLATES, data });
}
