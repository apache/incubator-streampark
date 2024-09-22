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
import { AxiosResponse } from 'axios';
import { FlinkCluster } from './flinkCluster.type';
import { Result } from '/#/axios';
import { defHttp } from '/@/utils/http/axios';
import type { BasicTableParams } from '../model/baseModel';

enum FLINK_API {
  PAGE = '/flink/cluster/page',
  LIST = '/flink/cluster/list',
  REMOTE_URL = '/flink/cluster/remote_url',
  CREATE = '/flink/cluster/create',
  CHECK = '/flink/cluster/check',
  GET = '/flink/cluster/get',
  UPDATE = '/flink/cluster/update',
  START = '/flink/cluster/start',
  SHUTDOWN = '/flink/cluster/shutdown',
  DELETE = '/flink/cluster/delete',
}
/**
 * flink cluster
 * @returns Promise<FlinkEnv[]>
 */
export function fetchFlinkClusterPage(data: BasicTableParams) {
  return defHttp.post<FlinkCluster[]>({
    url: FLINK_API.PAGE,
    data,
  });
}
/**
 * flink cluster
 * @returns Promise<FlinkEnv[]>
 */
export function fetchFlinkCluster() {
  return defHttp.post<FlinkCluster[]>({
    url: FLINK_API.LIST,
  });
}
/**
 * flink cluster start
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchClusterStart(id: string): Promise<AxiosResponse<Result>> {
  return defHttp.post({ url: FLINK_API.START, data: { id } }, { isReturnNativeResponse: true });
}
/**
 * flink cluster remove
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchClusterRemove(id: string): Promise<AxiosResponse<Result>> {
  return defHttp.post({ url: FLINK_API.DELETE, data: { id } }, { isReturnNativeResponse: true });
}
/**
 * flink cluster shutdown
 * @returns  {Promise<AxiosResponse<Result>>}
 */
export function fetchClusterShutdown(id: string): Promise<AxiosResponse<Result>> {
  return defHttp.post<AxiosResponse<Result>>(
    { url: FLINK_API.SHUTDOWN, data: { id } },
    { isReturnNativeResponse: true },
  );
}
/**
 * flink cluster shutdown
 * @returns {Promise<string>}
 */
export function fetchRemoteURL(id: string): Promise<string> {
  return defHttp.post<string>({
    url: FLINK_API.REMOTE_URL,
    data: { id },
  });
}

export function fetchCheckCluster(data: Recordable) {
  return defHttp.post({
    url: FLINK_API.CHECK,
    data,
  });
}

export function fetchCreateCluster(data: Recordable) {
  return defHttp.post({
    url: FLINK_API.CREATE,
    data,
  });
}
export function fetchUpdateCluster(data: Recordable) {
  return defHttp.post({
    url: FLINK_API.UPDATE,
    data,
  });
}

export function fetchGetCluster(data: Recordable) {
  return defHttp.post({
    url: FLINK_API.GET,
    data,
  });
}
