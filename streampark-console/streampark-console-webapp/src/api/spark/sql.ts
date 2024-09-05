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

import type { SparkSql } from './sql.type';
import { defHttp } from '/@/utils/http/axios';
const apiPrefix = '/spark/sql';

export function fetchSparkSqlVerify(data: Recordable) {
  return defHttp.post({ url: `${apiPrefix}/verify`, data }, { isTransformResponse: false });
}

export function fetchSparkSqlList(data: Recordable) {
  return defHttp.post<{
    total: number;
    records: SparkSql[];
  }>({
    url: `${apiPrefix}/list`,
    data,
  });
}

export function fetchSparkSqlRemove(data: SparkSql) {
  return defHttp.post<boolean>({
    url: `${apiPrefix}/delete`,
    data,
  });
}

export function fetchSparkSql(data: Recordable) {
  return defHttp.post({
    url: `${apiPrefix}/get`,
    data,
  });
}
export function fetchSparkSqlHistory(data: Recordable) {
  return defHttp.post<SparkSql[]>({
    url: `${apiPrefix}/history`,
    data,
  });
}
export function fetchSparkSqlComplete(data: Recordable) {
  return defHttp.post({
    url: `${apiPrefix}/sqlComplete`,
    data,
  });
}
