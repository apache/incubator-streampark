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
import { defHttp } from '/@/utils/http/axios';

enum SETTING_APi {
  GET = '/flink/setting/get',
  GET_DOCKER = '/flink/setting/docker',
  GET_EMAIL = '/flink/setting/email',
  ALL = '/flink/setting/all',
  UPDATE = '/flink/setting/update',
  CHECK_HADOOP = '/flink/setting/check/hadoop',
  CHECK_DOCKER = '/flink/setting/check/docker',
  UPDATE_DOCKER = '/flink/setting/update/docker',
  CHECK_EMAIL = '/flink/setting/check/email',
  UPDATE_ALERT = '/flink/setting/update/email',
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
 * @returns {Promise<Boolean>}
 */
export function fetchSystemSettingUpdate(data: {
  settingKey: string;
  settingValue: boolean;
}): Promise<boolean> {
  return defHttp.post({
    url: SETTING_APi.UPDATE,
    data,
  });
}
/**
 * Check configuration
 * @returns {Promise<Boolean>}
 */
export function fetchCheckHadoop(): Promise<boolean> {
  return defHttp.post({
    url: SETTING_APi.CHECK_HADOOP,
  });
}

/**
 * get docker setting info
 */
export function fetchDockerConfig() {
  return defHttp.post({ url: SETTING_APi.GET_DOCKER });
}

/**
 * verify docker setting info
 */
export function fetchVerifyDocker(data: Recordable): Promise<boolean> {
  return defHttp.post({
    url: SETTING_APi.CHECK_DOCKER,
    data,
  });
}

/**
 * verify docker setting info
 */
export function fetchVerifyEmail(data: Recordable): Promise<boolean> {
  return defHttp.post({
    url: SETTING_APi.CHECK_EMAIL,
    data,
  });
}

/**
 * get alert setting info
 */
export function fetchEmailConfig() {
  return defHttp.post({ url: SETTING_APi.GET_EMAIL });
}

/**
 * Update docker setting
 * @returns {Promise<Boolean>}
 */
export function fetchDockerUpdate(data: Recordable): Promise<boolean> {
  return defHttp.post({
    url: SETTING_APi.UPDATE_DOCKER,
    data,
  });
}

/**
 * Update alert setting
 * @returns {Promise<Boolean>}
 */
export function fetchEmailUpdate(data: Recordable): Promise<boolean> {
  return defHttp.post({
    url: SETTING_APi.UPDATE_ALERT,
    data,
  });
}
