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
import type { BasicTableParams } from '/@/api/model/baseModel';
import type { CloudAccount, CloudAccountForm, CloudAccountGrant } from './cloudAccount.type';

enum Api {
  PAGE = '/cloud/account/page',
  CREATE = '/cloud/account/create',
  UPDATE = '/cloud/account/update',
  TEST = '/cloud/account/test',
  DISABLE = '/cloud/account/disable',
  DELETE = '/cloud/account/delete',
  GRANTS = '/cloud/account/grants',
  GRANT = '/cloud/account/grant',
}

export function fetchCloudAccountPage(data: BasicTableParams) {
  return defHttp.post({ url: Api.PAGE, data });
}

export function fetchCloudAccountCreate(data: CloudAccountForm): Promise<string> {
  return defHttp.post({ url: Api.CREATE, data });
}

export function fetchCloudAccountUpdate(data: CloudAccountForm): Promise<void> {
  return defHttp.post({ url: Api.UPDATE, data });
}

export function fetchCloudAccountTest(data: Pick<CloudAccount, 'id' | 'version'>) {
  return defHttp.post<CloudAccount>({ url: Api.TEST, data }, { errorMessageMode: 'none' });
}

export function fetchCloudAccountDisable(
  data: Pick<CloudAccount, 'id' | 'version'>,
): Promise<void> {
  return defHttp.post({ url: Api.DISABLE, data });
}

export function fetchCloudAccountDelete(data: Pick<CloudAccount, 'id' | 'version'>): Promise<void> {
  return defHttp.post({ url: Api.DELETE, data });
}

export function fetchCloudAccountGrants(accountId: string): Promise<CloudAccountGrant[]> {
  return defHttp.post({ url: Api.GRANTS, data: { accountId } });
}

export function fetchCloudAccountGrant(data: {
  accountId: string;
  accountVersion: number;
  teamIds: string[];
}): Promise<void> {
  return defHttp.post({ url: Api.GRANT, data });
}
