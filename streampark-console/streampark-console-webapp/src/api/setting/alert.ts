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
import { AlertCreate, AlertSetting } from './types/alert.type';
import { Result } from '/#/axios';
import { defHttp } from '/@/utils/http/axios';
import { AxiosResponse } from 'axios';

enum ALERT_APi {
  ADD = '/flink/alert/add',
  EXISTS = '/flink/alert/exists',
  UPDATE = '/flink/alert/update',
  GET = '/flink/alert/get',
  PAGE = '/flink/alert/page',
  LIST = '/flink/alert/list',
  DELETE = '/flink/alert/delete',
  SEND = '/flink/alert/send',
}
/**
 * Get alert settings
 * @returns {Promise<AlertSetting[]>}
 */
export function fetchAlertSetting(): Promise<AlertSetting[]> {
  return defHttp.post({ url: ALERT_APi.LIST });
}
/**
 * Test alert settings
 * @returns {Promise<boolean>}
 */
export function fetchSendAlert(data: { id: string }): Promise<boolean> {
  return defHttp.post<boolean>({ url: ALERT_APi.SEND, data }, { errorMessageMode: 'none' });
}
/**
 * Delete alert settings
 * @returns {Promise<boolean|undefined>}
 */
export function fetchAlertDelete(data: {
  id: string;
}): Promise<AxiosResponse<Result<boolean | undefined>>> {
  return defHttp.delete({ url: ALERT_APi.DELETE, data }, { isReturnNativeResponse: true });
}
/**
 * Alarm name test
 * @returns {Promise<boolean>}
 */
export function fetchExistsAlert(data: { alertName: string }): Promise<boolean> {
  return defHttp.postJson({ url: ALERT_APi.EXISTS, data });
}

/**
 * Add alert settings
 * @param {AlertCreate} data
 * @returns {Promise<AxiosResponse<Result<boolean>>>}
 */
export function fetchAlertAdd(data: AlertCreate): Promise<AxiosResponse<Result<boolean>>> {
  return defHttp.postJson({ url: ALERT_APi.ADD, data }, { isReturnNativeResponse: true });
}
/**
 * Update alert settings
 * @param {AlertCreate} params
 * @returns {Promise<boolean>}
 */
export function fetchAlertUpdate(data: AlertCreate): Promise<AxiosResponse<Result<boolean>>> {
  return defHttp.postJson({ url: ALERT_APi.UPDATE, data }, { isReturnNativeResponse: true });
}
