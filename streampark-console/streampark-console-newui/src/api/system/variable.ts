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
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { Result } from '/#/axios';

export interface AddMemberParams {
  teamId: string;
  userName: string;
  roleId: number;
}

export interface TeamMemberResp {
  id: string;
  teamName: string;
}

export interface UpdateMemberParams extends AddMemberParams {
  id: string;
  userId: string;
}

enum VARIABLE_API {
  LIST = '/variable/list',
  UPDATE = '/variable/update',
  POST = '/variable/post',
  DELETE = '/variable/delete',
  SELECT = '/variable/select',
  CHECK_CODE = '/variable/check/code',
}
/**
 * get variable list
 * @param params
 * @returns
 */
export function fetchVariableList(params: Recordable) {
  return defHttp.post({ url: VARIABLE_API.LIST, params });
}

/**
 * add member
 * @param {String} teamId organization id
 * @param {String} userName username
 * @param {Number} roleId role id
 * @returns Promise<boolean>
 */
export function fetchAddVariable(params: AddMemberParams) {
  return defHttp.post<boolean>({
    url: VARIABLE_API.POST,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchUpdateVariable(params) {
  return defHttp.put({
    url: VARIABLE_API.UPDATE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

/**
 * delete
 * @param {String} id memeber Id
 * @returns Promise<boolean>
 */
export function fetchVariableDelete(params: Recordable) {
  return defHttp.delete<AxiosResponse<Result>>(
    {
      url: VARIABLE_API.DELETE,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    { isReturnNativeResponse: true },
  );
}

export function fetchCheckVariableCode(params: { variableCode: string }) {
  return defHttp.post<AxiosResponse<Result>>(
    { url: VARIABLE_API.CHECK_CODE, params },
    { isReturnNativeResponse: true },
  );
}
