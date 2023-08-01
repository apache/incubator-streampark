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

enum Catalog_API {
  CREATE = '/flink/catalog/create',
  UPDATE = '/flink/catalog/update',
  LIST = '/flink/catalog/list',
  DELETE = '/flink/catalog/delete/{id}',
  GET = '/flink/catalog/get',
  CHECK_NAME = '/flink/catalog/check/name',
}

/**
 * fetch Catalog list.
 */
export function fetchCatalogList() {
  return defHttp.get({
    url: Catalog_API.LIST,
  });
}

/**
 * fetch Catalog remove result.
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchCatalogDelete(id: string): Promise<AxiosResponse<Result>> {
  return defHttp.delete(
    { url: Catalog_API.DELETE.replace('{id}', id) },
    { isReturnNativeResponse: true },
  );
}

export function fetchCatalogCreate(data: Recordable) {
  return defHttp.postJson({
    url: Catalog_API.CREATE,
    data,
  });
}

export function fetchCatalogUpdate(data: Recordable) {
  return defHttp.postJson({
    url: Catalog_API.UPDATE,
    data,
  });
}

export function fetchCatalogCheckName(name: string) {
  return defHttp.get({
    url: Catalog_API.CHECK_NAME,
    params: { name },
  });
}

