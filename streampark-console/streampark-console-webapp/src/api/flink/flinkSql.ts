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

enum FLINK_SQL_API {
  VERIFY = '/flink/sql/verify',
  GET = '/flink/sql/get',
  LIST = '/flink/sql/list',
  DELETE = '/flink/sql/delete',
  HISTORY = '/flink/sql/history',
}

export function fetchFlinkSqlVerify(data): Promise<AxiosResponse<Result>> {
  return defHttp.post({ url: FLINK_SQL_API.VERIFY, data }, { onlyData: false });
}

export function fetchFlinkSql(data) {
  return defHttp.post({
    url: FLINK_SQL_API.GET,
    data,
  });
}

export function fetchFlinkSqlList(data) {
  return defHttp.post({
    url: FLINK_SQL_API.LIST,
    data,
  });
}

export function fetchRemoveFlinkSql(data: { appId: any; id: any }): Promise<boolean> {
  return defHttp.post({
    url: FLINK_SQL_API.DELETE,
    data,
  });
}

export function fetchFlinkHistory(data) {
  return defHttp.post({
    url: FLINK_SQL_API.HISTORY,
    data,
  });
}
