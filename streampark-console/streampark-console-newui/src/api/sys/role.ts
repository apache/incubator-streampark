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
  RoleUserList = '/role/listByUser',
  AddRole = '/role/post',
  UpdateRole = '/role/update',
  DeleteRole = '/role/delete',
  CHECK_NAME = '/role/check/name',
}

export function getRoleListByUser(params?) {
  return defHttp.post({ url: Api.RoleUserList, params });
}

export function addRole(params?) {
  return defHttp.post({
    url: Api.AddRole,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function editRole(params?) {
  return defHttp.put({
    url: Api.UpdateRole,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function deleteRole(params?) {
  return defHttp.delete({
    url: Api.DeleteRole,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchCheckName(params) {
  return defHttp.post({
    url: Api.CHECK_NAME,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
