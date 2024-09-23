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
import { FlinkCreate, FlinkEnv } from './flinkEnv.type';
import { Result } from '/#/axios';
import { defHttp } from '/@/utils/http/axios';
import type { BasicTableParams } from '../model/baseModel';

enum FLINK_API {
  PAGE = '/flink/env/page',
  LIST = '/flink/env/list',
  CREATE = '/flink/env/create',
  DELETE = '/flink/env/delete',
  CHECK = '/flink/env/check',
  GET = '/flink/env/get',
  SYNC = '/flink/env/sync',
  UPDATE = '/flink/env/update',
  DEFAULT = '/flink/env/default',
  VALIDITY = '/flink/env/validity',
}
/**
 * flink environment data
 * @returns Promise<FlinkEnv[]>
 */
export function fetchFlinkEnvPage(data: BasicTableParams) {
  return defHttp.post<FlinkEnv[]>({
    url: FLINK_API.PAGE,
    data,
  });
}
/**
 * flink environment data
 * @returns Promise<FlinkEnv[]>
 */
export function fetchListFlinkEnv() {
  return defHttp.post<FlinkEnv[]>({
    url: FLINK_API.LIST,
  });
}

export function fetchFlinkEnv(id: string) {
  return defHttp.post<FlinkEnv>({
    url: FLINK_API.GET,
    data: { id: id },
  });
}

/**
 * Set the default
 * @param {String} id
 * @returns {Promise<FlinkEnv[]>}
 */
export function fetchDefaultSet(id: string): Promise<FlinkEnv[]> {
  return defHttp.post({
    url: FLINK_API.DEFAULT,
    data: { id },
  });
}
/**
 * Get flink details
 * @param {String} id
 * @returns {Promise<FlinkEnv>}
 */
export function fetchFlinkInfo(id: string): Promise<FlinkEnv> {
  return defHttp.post({
    url: FLINK_API.GET,
    data: { id },
  });
}

/**
 * delete flink env
 * @param {String} id
 * @returns {Promise<Boolean>}
 */
export function fetchFlinkEnvRemove(id: string): Promise<AxiosResponse<Result<boolean>>> {
  return defHttp.post(
    {
      url: FLINK_API.DELETE,
      data: { id },
    },
    { isReturnNativeResponse: true },
  );
}

/**
 * Check if the environment exists
 * @param {Recordable} data
 * @returns {Promise<Boolean>}
 */
export function fetchCheckEnv(data: {
  id: string | null;
  flinkName: string;
  flinkHome: string;
}): Promise<AxiosResponse<Result<number>>> {
  return defHttp.post({ url: FLINK_API.CHECK, data }, { isReturnNativeResponse: true });
}

/**
 * check for update or delete operation
 * @param {String} id
 * @returns {Promise<Boolean>}
 */
export function fetchValidity(id: string): Promise<AxiosResponse<Result<boolean>>> {
  return defHttp.post(
    {
      url: FLINK_API.VALIDITY,
      data: { id },
    },
    { isReturnNativeResponse: true },
  );
}

/**
 * Create flink
 * @param {FlinkCreate} data
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchFlinkCreate(data: FlinkCreate): Promise<AxiosResponse<Result>> {
  return defHttp.post({ url: FLINK_API.CREATE, data }, { isReturnNativeResponse: true });
}

/**
 * update flink
 * @returns {Promise<AxiosResponse<Result>>}
 * @param data
 */
export function fetchFlinkUpdate(data: FlinkCreate): Promise<AxiosResponse<Result>> {
  return defHttp.post<AxiosResponse<Result>>(
    { url: FLINK_API.UPDATE, data },
    { isReturnNativeResponse: true },
  );
}

/**
 * Configure synchronization
 * @param {String} id
 * @returns {Promise<Boolean>}
 */
export function fetchFlinkSync(id: string): Promise<boolean> {
  return defHttp.post<boolean>({
    url: FLINK_API.SYNC,
    data: { id },
  });
}
