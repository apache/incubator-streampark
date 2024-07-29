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
import { AxiosResponse } from 'axios';

enum GATEWAY_API {
  CREATE = '/flink/gateway/create',
  UPDATE = '/flink/gateway/update',
  LIST = '/flink/gateway/list',
  DELETE = '/flink/gateway/delete',
  GET = '/flink/gateway/get',
  CHECK_NAME = '/flink/gateway/check/name',
  CHECK_ADDRESS = '/flink/gateway/check/address',
}

/**
 * fetch gateway list.
 */
export function fetchGatewayList() {
  return defHttp.get({
    url: GATEWAY_API.LIST,
  });
}

/**
 * fetch gateway remove result.
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchGatewayDelete(id: string): Promise<AxiosResponse<boolean>> {
  return defHttp.delete({ url: GATEWAY_API.DELETE, data: { id } });
}

export function fetchGatewayCreate(data: Recordable) {
  return defHttp.postJson({
    url: GATEWAY_API.CREATE,
    data,
  });
}

export function fetchGatewayUpdate(data: Recordable) {
  return defHttp.postJson({
    url: GATEWAY_API.UPDATE,
    data,
  });
}

export function fetchGatewayCheckName(name: string) {
  return defHttp.get({
    url: GATEWAY_API.CHECK_NAME,
    params: { name },
  });
}

export function fetchGatewayCheckAddress(address: string) {
  return defHttp.get({
    url: GATEWAY_API.CHECK_ADDRESS,
    params: { address },
  });
}
