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
import { Result } from '/#/axios';
import { AxiosResponse } from 'axios';
import { BasicTableParams } from '/@/api/model/baseModel';

enum YARN_QUEUE_API {
  CREATE = '/yarn/queue/create',
  UPDATE = '/yarn/queue/update',
  LIST = '/yarn/queue/list',
  DELETE = '/yarn/queue/delete',
  GET = '/yarn/queue/get',
  CHECK_YARN_QUEUE = '/yarn/queue/check',
}

/**
 * fetch yarn queues in the specified team.
 */
export function fetchYarnQueueList(data: BasicTableParams) {
  return defHttp.post({
    url: YARN_QUEUE_API.LIST,
    data: data,
  });
}

/**
 * fetch yarn queue remove result.
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchYarnQueueDelete(data: Recordable): Promise<AxiosResponse<Result>> {
  return defHttp.post(
    {
      url: YARN_QUEUE_API.DELETE,
      data,
    },
    { isReturnNativeResponse: true },
  );
}

/**
 * fetch yarn queue existed check result.
 */
export function fetchCheckYarnQueue(data: Recordable) {
  return defHttp.post({
    url: YARN_QUEUE_API.CHECK_YARN_QUEUE,
    data,
  });
}

export function fetchYarnQueueCreate(data: Recordable) {
  return defHttp.post({
    url: YARN_QUEUE_API.CREATE,
    data,
  });
}

export function fetchYarnQueueUpdate(data: Recordable) {
  return defHttp.post({
    url: YARN_QUEUE_API.UPDATE,
    data,
  });
}
