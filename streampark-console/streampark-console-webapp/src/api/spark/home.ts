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

import type { SparkCreate, SparkEnv } from './home.type';
import { defHttp } from '/@/utils/http/axios';

const apiPrefix = '/spark/env';
/**
 * spark environment data
 * @returns Promise<SparkEnv[]>
 */
export function fetchSparkEnvList() {
  return defHttp.post<SparkEnv[]>({
    url: `${apiPrefix}/list`,
  });
}

export function fetchSparkEnv(id: string) {
  return defHttp.post<SparkEnv>({
    url: `${apiPrefix}/get`,
    data: { id: id },
  });
}

/**
 * Set the default
 * @param {String} id
 */
export function fetchSetDefault(id: string) {
  return defHttp.post({
    url: `${apiPrefix}/default`,
    data: { id },
  });
}

/**
 * delete flink env
 * @param {String} id
 */
export function fetchSparkEnvRemove(id: string) {
  return defHttp.post({
    url: `${apiPrefix}/delete`,
    data: { id },
  });
}

/**
 * Check if the environment exists
 * @param {Recordable} data
 */
export function fetchSparkEnvCheck(data: {
  id: string | null;
  sparkName: string;
  sparkHome: string;
}) {
  return defHttp.post({
    url: `${apiPrefix}/check`,
    data,
  });
}

/**
 * Create spark
 *
 */
export function fetchSparkEnvCreate(data: SparkCreate) {
  return defHttp.post(
    {
      url: `${apiPrefix}/create`,
      data,
    },
    { isTransformResponse: false },
  );
}

/**
 * update spark
 * @param data
 */
export function fetchSparkEnvUpdate(data: SparkCreate) {
  return defHttp.post(
    {
      url: `${apiPrefix}/update`,
      data,
    },
    { isTransformResponse: false },
  );
}

/**
 * Configure synchronization
 */
export function fetchSparkSync(id: string): Promise<boolean> {
  return defHttp.post<boolean>({
    url: `${apiPrefix}/sync`,
    data: { id },
  });
}
