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

enum Api {
  TokenList = '/token/list',
  ToggleTokenStatus = '/token/toggle',
  AddToken = 'token/create',
  DeleteToken = 'token/delete',
  CHECK = 'token/check',
  CURL = '/token/curl',
}

export function getTokenList(params?) {
  return defHttp.post({
    url: Api.TokenList,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function setTokenStatus(data) {
  return defHttp.post({
    url: Api.ToggleTokenStatus,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function addToken(data) {
  return defHttp.post({
    url: Api.ToggleTokenStatus,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function deleteToken(data) {
  return defHttp.post({
    url: Api.DeleteToken,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

/**
 * check token
 * @param data
 * @returns {Promise<number>}
 */
export function fetchCheckToken(data) {
  return defHttp.post<number>({
    url: Api.CHECK,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * copyCurl
 * @param data
 * @returns {Promise<string>}
 */
export function fetchCopyCurl(data) {
  return defHttp.post<string>({
    url: Api.CURL,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
