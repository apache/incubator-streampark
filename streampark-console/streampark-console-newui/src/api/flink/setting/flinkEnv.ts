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
import { FlinkCreate, FlinkEnv } from './types/flinkEnv.type';
import { Result } from '/#/axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum FLINK_API {
  LIST = '/flink/env/list',
  CREATE = '/flink/env/create',
  EXISTS = '/flink/env/exists',
  GET = '/flink/env/get',
  SYNC = '/flink/env/sync',
  UPDATE = '/flink/env/update',
  DEFAULT = '/flink/env/default',
}
/**
 * flink 环境数据
 * @returns Promise<FlinkEnv[]>
 */
export function fetchFlinkEnv() {
  return defHttp.post<FlinkEnv[]>({
    url: FLINK_API.LIST,
  });
}

/**
 * 设置默认
 * @param {String} id
 * @returns Promise<Boolean>
 */
export function fetchDefaultSet(id: string) {
  return defHttp.post<FlinkEnv[]>({
    url: FLINK_API.DEFAULT,
    params: { id },
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 获取flink 详细信息
 * @param {String} id
 * @returns Promise<Boolean>
 */
export function fetchFlinkInfo(id: string) {
  return defHttp.post<FlinkEnv>({
    url: FLINK_API.GET,
    params: { id },
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

/**
 * 检测环境是否存在
 * @param {String} id
 * @returns Promise<Boolean>
 */
export function fetchExistsEnv(params: {
  id: string | null;
  flinkName: string;
  flinkHome: string;
}) {
  return defHttp.post<boolean>({
    url: FLINK_API.EXISTS,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 创建 flink
 * @param {FlinkCreate} params
 * @returns Promise<Boolean>
 */
export function fetchFlinkCreate(params: FlinkCreate) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: FLINK_API.CREATE,
      params,
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
 * 更新 flink
 * @param {FlinkCreate} params
 * @returns Promise<Boolean>
 */
export function fetchFlinkUpdate(params: FlinkCreate) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: FLINK_API.UPDATE,
      params,
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
 * 配置同步
 * @param {String} id
 * @returns Promise<Boolean>
 */
export function fetchFlinkSync(id: string) {
  return defHttp.post<boolean>({
    url: FLINK_API.SYNC,
    params: { id },
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
