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
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum SAVE_POINT_API {
  LATEST = '/flink/savepoint/latest',
  HISTORY = '/flink/savepoint/history',
  DELETE = '/flink/savepoint/delete',
}

export function fetchLatest(params) {
  return defHttp.post({
    url: SAVE_POINT_API.LATEST,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function fetchSavePonitHistory(params) {
  return defHttp.post({
    url: SAVE_POINT_API.HISTORY,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * delete
 * @param params id
 * @returns
 */
export function fetchRemoveSavePoint(params: { id: string }) {
  return defHttp.post<boolean>({
    url: SAVE_POINT_API.DELETE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
