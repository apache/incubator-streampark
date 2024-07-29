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
import { Result } from '/#/axios';
import { AxiosResponse } from 'axios';
import { BasicTableParams } from '../model/baseModel';
import { TeamListRecord, TeamParam } from './model/teamModel';

enum Api {
  POST = '/team/post',
  UPDATE = '/team/update',
  LIST = '/team/list',
  DELETE = '/team/delete',
}

/**
 * get team list
 * @param {BasicTableParams} data
 * @returns {Promise<>}
 */
export function fetTeamList(data: BasicTableParams): Promise<TeamListRecord[]> {
  return defHttp.post({ url: Api.LIST, data });
}
/**
 * Create an organization
 * @param {TeamParam} data
 * @returns {Promise<boolean | undefined>}
 */
export function fetchTeamCreate(data: TeamParam): Promise<boolean | undefined> {
  return defHttp.post({ url: Api.POST, data });
}

/**
 * update organization
 * @param {TeamParam} data
 * @returns {Promise<boolean>}
 */
export function fetchTeamUpdate(data: TeamParam): Promise<boolean | undefined> {
  return defHttp.put({ url: Api.UPDATE, data });
}
/**
 * delete organization
 * @returns {Promise<AxiosResponse<Result>>}
 * @param params
 */
export function fetchTeamDelete(params: { id: string }): Promise<AxiosResponse<Result>> {
  return defHttp.delete({ url: Api.DELETE, params }, { onlyData: false, errorMessageMode: 'none' });
}
