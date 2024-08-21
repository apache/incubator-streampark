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

enum SAVE_POINT_API {
  LATEST = '/flink/savepoint/latest',
  HISTORY = '/flink/savepoint/history',
  DELETE = '/flink/savepoint/delete',
  TRIGGER = '/flink/savepoint/trigger',
}

export function fetchLatest(data: Recordable) {
  return defHttp.post({ url: SAVE_POINT_API.LATEST, data });
}
export function fetchSavePointHistory(data: Recordable) {
  return defHttp.post({ url: SAVE_POINT_API.HISTORY, data });
}
/**
 * delete
 * @param data id
 * @returns {Promise<boolean>}
 */
export function fetchRemoveSavePoint(data: { appId: any; id: any }): Promise<boolean> {
  return defHttp.post({
    url: SAVE_POINT_API.DELETE,
    data,
  });
}

/**
 * Trigger a savepoint manually.
 * @param data app id & optional savepoint path.
 */
export function trigger(data: { appId: string | number; savepointPath: string | null }) {
  return defHttp.post({ url: SAVE_POINT_API.TRIGGER, data });
}
