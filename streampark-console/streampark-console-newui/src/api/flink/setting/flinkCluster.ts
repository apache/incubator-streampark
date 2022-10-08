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
import { FlinkCluster } from './types/flinkCluster.type';
import { Result } from '/#/axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum FLINK_API {
  LIST = '/flink/cluster/list',
  ACTIVE_URL = '/flink/cluster/activeUrl',
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
export function fetchFlinkCluster() {
  return defHttp.post<FlinkCluster[]>({
    url: FLINK_API.LIST,
  });
}
/**
 * flink cluster start
 * @returns Promise<AxiosResponse>
 */
export function fetchClusterStart(id: string) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: FLINK_API.START,
      params: { id },
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
/**
 * flink cluster remove
 * @returns Promise<AxiosResponse>
 */
export function fetchClusterRemove(id: string) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: FLINK_API.DELETE,
      params: { id },
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
/**
 * flink cluster shutdown
 * @returns Promise<AxiosResponse>
 */
export function fetchClusterShutdown(id: string) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: FLINK_API.SHUTDOWN,
      params: { id },
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
/**
 * flink cluster shutdown
 * @returns Promise<AxiosResponse>
 */
export function fetchActiveURL(id: string) {
  return defHttp.post<string>({
    url: FLINK_API.ACTIVE_URL,
    params: { id },
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchCheckCluster(params: Recordable) {
  return defHttp.post({
    url: FLINK_API.CHECK,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchCreateCluster(params: Recordable) {
  return defHttp.post({
    url: FLINK_API.CREATE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function fetchUpdateCluster(params: Recordable) {
  return defHttp.post({
    url: FLINK_API.UPDATE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchGetCluster(params: Recordable) {
  return defHttp.post({
    url: FLINK_API.GET,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
