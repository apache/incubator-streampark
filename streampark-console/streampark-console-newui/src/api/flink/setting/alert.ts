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
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum ALERT_APi {
  ADD = '/flink/alert/add',
  EXISTS = '/flink/alert/exists',
  UPDATE = '/flink/alert/update',
  GET = '/flink/alert/get',
  LIST = '/flink/alert/list',
  LIST_WITHOUTPAGE = '/flink/alert/listWithOutPage',
  DELETE = '/flink/alert/delete',
  SEND = '/flink/alert/send',
}
/**
 * 获取告警设置
 * @returns Promise<AlertSetting[]>
 */
export function fetchAlertSetting() {
  return defHttp.post<AlertSetting[]>({
    url: ALERT_APi.LIST_WITHOUTPAGE,
  });
}
/**
 * 测试告警设置
 * @returns Promise<boolean>
 */
export function fetchSendAlert(params: { id: string }) {
  return defHttp.post<boolean>(
    {
      url: ALERT_APi.SEND,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      errorMessageMode: 'none',
    },
  );
}
/**
 * 删除告警设置
 * @returns Promise<MavenSetting[]>
 */
export function fetchAlertDelete(params: { id: string }) {
  return defHttp.delete<boolean>({
    url: ALERT_APi.DELETE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 告警名称测试
 * @returns Promise<boolean>
 */
export function fetchExistsAlert(params: { alertName: string; isJsonType?: boolean }) {
  return defHttp.post<boolean>({
    url: ALERT_APi.EXISTS,
    params,
  });
}

/**
 * 添加告警设置
 * @param {AlertCreate} params
 * @returns Promise<boolean>
 */
export function fetchAlertAdd(params: AlertCreate) {
  return defHttp.post<boolean>({
    url: ALERT_APi.ADD,
    params,
  });
}
/**
 * 更新告警设置
 * @param {AlertCreate} params
 * @returns Promise<boolean>
 */
export function fetchAlertUpdate(params: AlertCreate) {
  return defHttp.post<boolean>({
    url: ALERT_APi.UPDATE,
    params,
  });
}

/**
 * 更新系统设置
 * @param {String} settingKey key
 * @param {Boolean} settingValue value
 * @returns Promise<boolean>
 */
export function fetchSystemSettingUpdate(params: { settingKey: string; settingValue: boolean }) {
  return defHttp.post<boolean>({
    url: ALERT_APi.UPDATE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
