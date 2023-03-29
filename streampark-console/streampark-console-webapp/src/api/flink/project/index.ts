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
import { ProjectModel } from '/@/api/flink/project/model/projectModel';
import { Result } from '/#/axios';
import { AxiosResponse } from 'axios';

enum Api {
  BRANCHES = '/flink/project/branches',
  GIT_CHECK = '/flink/project/gitcheck',
  EXISTS = '/flink/project/exists',
  CREATE = '/flink/project/create',
  GET = '/flink/project/get',
  UPDATE = '/flink/project/update',
  COPY = '/flink/project/copy',
  BUILD = '/flink/project/build',
  BUILD_LOG = '/flink/project/buildlog',
  CLOSE_BUILD = '/flink/project/closebuild',
  LIST = '/flink/project/list',
  FILE_LIST = '/flink/project/filelist',
  MODULES = '/flink/project/modules',
  LIST_CONF = '/flink/project/listconf',
  JARS = '/flink/project/jars',
  DELETE = '/flink/project/delete',
  SELECT = '/flink/project/select',
}

export function getList(data?: Recordable): Promise<ProjectModel> {
  return defHttp.post({
    url: Api.LIST,
    data,
  });
}

export function isExist(data: Recordable) {
  return defHttp.post({
    url: Api.EXISTS,
    data,
  });
}

export function gitCheck(data: Recordable) {
  return defHttp.post({
    url: Api.GIT_CHECK,
    data,
  });
}
/**
 *
 * @param data
 * @returns
 */
export function fetchBranches(data: Recordable): Promise<string[]> {
  return defHttp.post({
    url: Api.BRANCHES,
    data,
  });
}

export function createProject(data: Recordable): Promise<AxiosResponse<Result>> {
  return defHttp.post({ url: Api.CREATE, data }, { isReturnNativeResponse: true });
}

export function getDetail(data: Recordable) {
  return defHttp.post({
    url: Api.GET,
    data,
  });
}

export function updateProject(data: Recordable): Promise<AxiosResponse<Result<boolean>>> {
  return defHttp.post({ url: Api.UPDATE, data }, { isReturnNativeResponse: true });
}

export function buildProject(data: Recordable): Promise<boolean> {
  return defHttp.post({
    url: Api.BUILD,
    data,
  });
}

export function buildLog(data: Recordable): Promise<AxiosResponse<any>> {
  return defHttp.post({ url: Api.BUILD_LOG, data }, { isReturnNativeResponse: true });
}

export function closeBuild(data: Recordable) {
  return defHttp.post({
    url: Api.CLOSE_BUILD,
    data,
  });
}

export function deleteProject(data: Recordable): Promise<AxiosResponse<Result<boolean>>> {
  return defHttp.post({ url: Api.DELETE, data }, { isReturnNativeResponse: true });
}

export function modules(data: Recordable) {
  return defHttp.post({
    url: Api.MODULES,
    data,
  });
}

export function jars(data: Recordable) {
  return defHttp.post({
    url: Api.JARS,
    data,
  });
}

export function fetchListConf(data: Recordable) {
  return defHttp.post<any>({
    url: Api.LIST_CONF,
    data,
  });
}
export function fetchSelect(data: Recordable) {
  return defHttp.post<any>({
    url: Api.SELECT,
    data,
  });
}

export function fetchListJars(data: Recordable): Promise<string[]> {
  return defHttp.post({
    url: Api.JARS,
    data,
  });
}
