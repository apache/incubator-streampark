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
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { Result } from '/#/axios';
import { AxiosResponse } from 'axios';

enum Api {
  TeamListByUser = '/team/listByUser',
  POST = '/team/post',
  UPDATE = '/team/update',
  LIST = '/team/list',
  CHECK_NAME = '/team/check/name',
  DELETE = '/team/delete',
}

export function getTeamListByUser(params?) {
  return defHttp.post({ url: Api.TeamListByUser, params });
}

export function getTeamList(params) {
  return defHttp.post({
    url: Api.LIST,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * Create an organization
 * @param {Recordable} teamParams
 * @returns Promise<Boolean>
 */
export function fetchTeamCreate(params) {
  return defHttp.post({
    url: Api.POST,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

/**
 * update organization
 * @param {Recordable} teamParams
 * @returns Promise<Boolean>
 */
export function fetchTeamUpdate(params: Recordable) {
  return defHttp.put<boolean>({
    url: Api.UPDATE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * delete organization
 * @param {String} id organization id
 * @returns Promise<AxiosResponse>
 */
export function deleteTeam(params: { id: string }) {
  return defHttp.delete<AxiosResponse<Result>>(
    {
      url: Api.DELETE,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
      errorMessageMode: 'none',
    },
  );
}
