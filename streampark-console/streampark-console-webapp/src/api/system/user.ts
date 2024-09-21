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
import { UserInfo } from '/#/store';
import { AxiosResponse } from 'axios';
import { defHttp } from '/@/utils/http/axios';
import { GetUserInfoModel, TeamSetResponse, UserListRecord } from './model/userModel';

import { Result } from '/#/axios';
import { BasicTableParams } from '../model/baseModel';

enum Api {
  Login = '/passport/signin',
  GetPermCode = '/getPermCode',
  UserList = '/user/list',
  NoTokenUsers = '/user/getNoTokenUser',
  UserUpdate = '/user/update',
  UserDelete = '/user/delete',
  UserAdd = '/user/post',
  ResetPassword = '/user/password/reset',
  Password = '/user/password',
  CheckName = '/user/check/name',
  SET_TEAM = '/user/set_team',
  APP_OWNERS = '/user/appOwners',
  TransferUserResource = '/user/transferResource',
}

/**
 * get user permission code list
 * @returns {Promise<string[]>}
 */
export function getPermCode(): Promise<string[]> {
  return defHttp.get({ url: Api.GetPermCode });
}

/**
 * get user list
 * @param {BasicTableParams} data
 * @returns {Promise<UserListRecord>} user array
 */
export function getUserList(data: BasicTableParams): Promise<{ records: UserListRecord[] }> {
  return defHttp.post({ url: Api.UserList, data });
}

export function getNoTokenUserList(data: Recordable): Promise<GetUserInfoModel> {
  return defHttp.post({ url: Api.NoTokenUsers, data });
}

export function updateUser(data: Recordable) {
  return defHttp.put({ url: Api.UserUpdate, data });
}

export function deleteUser(data) {
  return defHttp.delete({ url: Api.UserDelete, data });
}

export function addUser(data: Recordable) {
  return defHttp.post({ url: Api.UserAdd, data });
}

export function resetPassword(data): Promise<AxiosResponse<Result<string>>> {
  return defHttp.put({ url: Api.ResetPassword, data }, { isReturnNativeResponse: true });
}

export function checkUserName(data) {
  return defHttp.post({
    url: Api.CheckName,
    data,
  });
}

/**
 * User change password
 * @param data
 */
export function fetchUserPasswordUpdate(data: {
  userId: string | number;
  oldPassword: string;
  password: string;
}): Promise<boolean> {
  return defHttp.put({
    url: Api.Password,
    data,
  });
}

export function fetchAppOwners(data: Recordable): Promise<Array<UserInfo>> {
  return defHttp.post({
    url: Api.APP_OWNERS,
    data,
  });
}

export function fetchSetUserTeam(data: { teamId: string }): Promise<TeamSetResponse> {
  return defHttp.post({
    url: Api.SET_TEAM,
    data,
  });
}

export function transferUserResource(data: {
  userId: string;
  targetUserId: string;
}): Promise<TeamSetResponse> {
  return defHttp.put({ url: Api.TransferUserResource, data });
}
