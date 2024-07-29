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
import { ExternalLink } from './types/externalLink.type';
import { Result } from '/#/axios';
import { defHttp } from '/@/utils/http/axios';

enum EXTERNAL_LINK_API {
  LIST = '/flink/externalLink/list',
  CREATE = '/flink/externalLink/create',
  UPDATE = '/flink/externalLink/update',
  DELETE = '/flink/externalLink/delete',
  RENDER = '/flink/externalLink/render',
}
/**
 * Get external link settings
 * @returns {Promise<ExternalLink[]>}
 */
export function fetchExternalLink(): Promise<ExternalLink[]> {
  return defHttp.post({ url: EXTERNAL_LINK_API.LIST });
}

/**
 * Create external link
 * @param {ExternalLink} data
 * @returns {Promise<AxiosResponse<Result<ExternalLink>>>}
 */
export function fetchExternalLinkCreate(data: ExternalLink): Promise<AxiosResponse<Result>> {
  return defHttp.post({ url: EXTERNAL_LINK_API.CREATE, data }, { onlyData: false });
}

/**
 * Update external link
 * @param {ExternalLink} data
 * @returns {Promise<AxiosResponse<Result<ExternalLink>>>}
 */
export function fetchExternalLinkUpdate(data: ExternalLink): Promise<AxiosResponse<Result>> {
  return defHttp.post({ url: EXTERNAL_LINK_API.UPDATE, data }, { onlyData: true });
}

/**
 * Delete external link
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchExternalLinkDelete(id: string): Promise<AxiosResponse<Result>> {
  return defHttp.delete({ url: EXTERNAL_LINK_API.DELETE, data: { id } });
}

export function fetchAppExternalLink(data: {
  appId: string;
}): Promise<AxiosResponse<Result<ExternalLink[]>>> {
  return defHttp.post({ url: EXTERNAL_LINK_API.RENDER, data });
}
