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
import { SystemSetting } from './types/setting.type';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum SETTING_APi {
  GET = '/flink/setting/get',
  WEB_URL = '/flink/setting/weburl',
  ALL = '/flink/setting/all',
  CHECK_HADOOP = '/flink/setting/checkHadoop',
  SYNC = '/flink/setting/sync',
  UPDATE = '/flink/setting/update',
}
/**
 * Get system settings
 * @returns Promise<MavenSetting[]>
 */
export function fetchSystemSetting() {
  return defHttp.post<SystemSetting[]>({
    url: SETTING_APi.ALL,
  });
}

/**
 * Update system settings
 * @param {String} settingKey key
 * @param {Boolean} settingValue value
 * @returns Promise<boolean>
 */
export function fetchSystemSettingUpdate(params: { settingKey: string; settingValue: boolean }) {
  return defHttp.post<boolean>({
    url: SETTING_APi.UPDATE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * Check configuration
 * @returns Promise<boolean>
 */
export function fetchCheckHadoop() {
  return defHttp.post<boolean>({
    url: SETTING_APi.CHECK_HADOOP,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
