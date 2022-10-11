import { AxiosResponse } from 'axios';
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

export interface AddMemberParams {
  teamId: string;
  userName: string;
  roleId: number;
}

export interface UpdateMemberParams extends AddMemberParams {
  id: string;
  userId: string;
}

enum MEMBER_API {
  POST = '/member/post',
  UPDATE = '/member/update',
  LIST = '/member/list',
  TEAMS = '/member/teams',
  CHECK_NAME = '/user/check/name',
  DELETE = '/member/delete',
}
/**
 * get member list
 * @param params
 * @returns
 */
export function fetchMemberList(params) {
  return defHttp.post({
    url: MEMBER_API.LIST,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

/**
 * add member
 * @param {String} teamId organization id
 * @param {String} userName username
 * @param {Number} roleId role id
 * @returns Promise<boolean>
 */
export function fetchAddMember(params: AddMemberParams) {
  return defHttp.post<boolean>({
    url: MEMBER_API.POST,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * update member
 * @param params
 * @returns
 */
export function fetchUpdateMember(params) {
  return defHttp.put({
    url: MEMBER_API.UPDATE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * Find user team
 * @param {Number|String} userId
 * @returns Promise<Array<{ id: string; teamName: string }>>
 */
export function fetchUserTeam(params: { userId: number | string }) {
  return defHttp.post<Array<{ id: string; teamName: string }>>({
    url: MEMBER_API.TEAMS,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * name check
 * @param {String} username name
 * @returns Promise<boolean>
 */
export function fetchCheckUserName(params: { username: string }) {
  return defHttp.post<boolean>({
    url: MEMBER_API.CHECK_NAME,
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
export function fetchMemberDelete(params: { id: string }) {
  return defHttp.delete<AxiosResponse<Result>>(
    {
      url: MEMBER_API.DELETE,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
