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
import { defHttp } from '/@/utils/http/axios';
import { Result } from '/#/axios';
import {
  BasicTableParams,
  ResourceDeleteParam,
  ResourceListRecord,
  ResourceParam,
} from './model/resourceModel';

enum RESOURCE_API {
  PAGE = '/resource/page',
  POST = '/resource/add',
  UPDATE = '/resource/update',
  DELETE = '/resource/delete',
  LIST = '/resource/list',
}

/**
 * get dependency list
 * @param data
 * @returns
 */
export function fetchResourceList(data: BasicTableParams): Promise<ResourceListRecord[]> {
  return defHttp.post({ url: RESOURCE_API.PAGE, data });
}

/**
 * add dependency
 * @param {ResourceParam} data
 * @returns {Promise<boolean>}
 */
export function fetchAddResource(data: ResourceParam): Promise<boolean> {
  return defHttp.post({ url: RESOURCE_API.POST, data });
}

/**
 * update dependency
 * @param {ResourceParam} data
 * @returns {Promise<boolean|undefined>}
 */
export function fetchUpdateResource(data: ResourceParam): Promise<boolean | undefined> {
  return defHttp.put({ url: RESOURCE_API.UPDATE, data });
}

/**
 * delete
 * @param {ResourceDeleteParam} data
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchResourceDelete(data: ResourceDeleteParam): Promise<AxiosResponse<Result>> {
  return defHttp.delete({ url: RESOURCE_API.DELETE, data }, { isReturnNativeResponse: true });
}

/**
 * get team dependency list
 * @param data
 * @returns
 */
export function fetchTeamResource(data: Recordable): Promise<ResourceListRecord[]> {
  return defHttp.post({ url: RESOURCE_API.LIST, data });
}
