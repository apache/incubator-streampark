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
import { LoginParams, LoginResultModel, GetUserInfoModel } from './model/userModel';

import { ErrorMessageMode } from '/#/axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';

enum Api {
  Login = '/passport/signin',
  Logout = '/passport/signout',
  GetUserInfo = '/getUserInfo',
  GetPermCode = '/getPermCode',
  TestRetry = '/testRetry',
  UserList = '/user/list',
  NoTokenUsers = '/user/getNoTokenUser',
  UserUpdate = '/user/update',
  UserAdd = '/user/post',
  UserDelete = '/user/delete',
  ResetPassword = '/user/password/reset',
  Password = '/user/password',
  CheckName = '/user/check/name',
  TYPES = '/user/types',
}

/**
 * @description: user login api
 */
export function loginApi(params: LoginParams, mode: ErrorMessageMode = 'modal') {
  return defHttp.post<LoginResultModel>(
    {
      url: Api.Login,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      errorMessageMode: mode,
    },
  );
}

/**
 * @description: getUserInfo
 */
export function getUserInfo() {
  return defHttp.get<GetUserInfoModel>({ url: Api.GetUserInfo }, { errorMessageMode: 'none' });
}

export function getPermCode() {
  return defHttp.get<string[]>({ url: Api.GetPermCode });
}

export function doLogout() {
  return defHttp.post({ url: Api.Logout });
}

export function getUserList(params) {
  return defHttp.post({
    url: Api.UserList,
    data: params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function getNoTokenUserList(params) {
  return defHttp.post<GetUserInfoModel>({
    url: Api.NoTokenUsers,
    data: params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function updateUser(data) {
  return defHttp.put({
    url: Api.UserUpdate,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function addUser(data) {
  return defHttp.post({
    url: Api.UserAdd,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function deleteUser(data) {
  return defHttp.delete({
    url: Api.UserDelete,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function resetPassword(data) {
  return defHttp.put({
    url: Api.ResetPassword,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function checkUserName(params) {
  return defHttp.post({
    url: Api.CheckName,
    data: params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchUserTypes() {
  return defHttp
    .post({
      url: Api.TYPES,
      data: {},
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    })
    .then((res) => {
      return res.map((t) => ({ label: t, value: t }));
    });
}
/**
 * User change password
 * @param{String} username username
 * @param{String} password password
 */
export function fetchUserPasswordUpdate(params: { username: string; password: string }) {
  return defHttp.put<boolean>({
    url: Api.Password,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function testRetry() {
  return defHttp.get(
    { url: Api.TestRetry },
    {
      retryRequest: {
        isOpenRetry: true,
        count: 5,
        waitTime: 1000,
      },
    },
  );
}
