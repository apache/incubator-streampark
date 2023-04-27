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
  BasicTableParams, DependencyDeleteParam,
  DependencyListRecord,
  DependencyParam,
} from './model/dependencyModel';

enum DEPENDENCY_API {
  PAGE = '/dependency/page',
  POST = '/dependency/add',
  UPDATE = '/dependency/update',
  DELETE = '/dependency/delete',
  LIST = '/dependency/list',
}

/**
 * get dependency list
 * @param data
 * @returns
 */
export function fetchDependencyList(data: BasicTableParams): Promise<DependencyListRecord[]> {
  return defHttp.post({ url: DEPENDENCY_API.PAGE, data });
}

/**
 * add dependency
 * @param {DependencyParam} data
 * @returns {Promise<boolean>}
 */
export function fetchAddDependency(data: DependencyParam): Promise<boolean> {
  return defHttp.post({ url: DEPENDENCY_API.POST, data });
}

/**
 * update dependency
 * @param {DependencyParam} data
 * @returns {Promise<boolean|undefined>}
 */
export function fetchUpdateDependency(data: DependencyParam): Promise<boolean | undefined> {
  return defHttp.put({ url: DEPENDENCY_API.UPDATE, data });
}

/**
 * delete
 * @param {DependencyDeleteParam} data
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchDependencyDelete(data: DependencyDeleteParam): Promise<AxiosResponse<Result>> {
  return defHttp.delete({ url: DEPENDENCY_API.DELETE, data }, { isReturnNativeResponse: true });
}

/**
 * get team dependency list
 * @param data
 * @returns
 */
export function fetchTeamDependency(data: Recordable): Promise<DependencyListRecord[]> {
  return defHttp.post({ url: DEPENDENCY_API.LIST, data });
}

