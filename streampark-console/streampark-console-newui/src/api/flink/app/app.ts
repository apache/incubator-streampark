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
import { AppListRecord, AppListResponse, CancelParam, DashboardResponse } from './app.type';
import { Result } from '/#/axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum APP_API {
  READ_CONF = '/flink/app/readConf',
  UPDATE = '/flink/app/update',
  COPY = '/flink/app/copy',
  UPLOAD = '/flink/app/upload',
  START_LOG = '/flink/app/detail',
  DEPLOY = '/flink/app/deploy',
  MAPPING = '/flink/app/mapping',
  YARN = '/flink/app/yarn',
  LIST = '/flink/app/list',
  GET = '/flink/app/get',
  DASHBOARD = '/flink/app/dashboard',
  MAIN = '/flink/app/main',
  NAME = '/flink/app/name',
  CHECK_NAME = '/flink/app/checkName',
  CANCEL = '/flink/app/cancel',
  FORCED_STOP = '/flink/app/forcedStop',
  DELETE = '/flink/app/delete',
  DELETE_BAK = '/flink/app/deletebak',
  CREATE = '/flink/app/create',
  START = '/flink/app/start',
  CLEAN = '/flink/app/clean',
  BACKUPS = '/flink/app/backups',
  ROLLBACK = '/flink/app/rollback',
  REVOKE = '/flink/app/revoke',
  OPTION_LOG = '/flink/app/optionlog',
  DOWN_LOG = '/flink/app/downlog',
  CHECK_JAR = '/flink/app/checkjar',
  VERIFY_SCHEMA = '/flink/app/verifySchema',
  CHECK_SAVEPOINT_PATH = '/flink/app/checkSavepointPath',
}

/**
 * 读取配置文件
 * @returns Promise<any>
 */
export function fetchAppConf(params?: { config: any }) {
  return defHttp.post<any>({
    url: APP_API.READ_CONF,
    params,
  });
}

/**
 * 仪表盘数据
 * @returns Promise<DashboardResponse>
 */
export function fetchDashboard() {
  return defHttp.post<DashboardResponse>({
    url: APP_API.DASHBOARD,
    params: {},
  });
}

/**
 * 获取 app 列表数据
 * @returns Promise<AppListResponse>
 */
export function fetchAppRecord(params) {
  return defHttp.post<AppListResponse>({
    url: APP_API.LIST,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 移出 app
 * @returns Promise<boolean>
 */
export function fetchAppRemove(id: string) {
  return defHttp.post<boolean>({
    url: APP_API.DELETE,
    params: { id },
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 获取 yarn 地址
 * @returns Promise<any>
 */
export function fetchYarn() {
  return defHttp.post<string>({
    url: APP_API.YARN,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

/**
 * 获取项目
 * @returns Promise<any>
 */
export function fetchCheckName(params: { id?: string; jobName: string }) {
  return defHttp.post<number>({
    url: APP_API.CHECK_NAME,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchMain(params) {
  return defHttp.post({
    url: APP_API.MAIN,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 上传
 * @param params
 * @returns {String} 文件路径
 */
export function fetchUpload(params) {
  return defHttp.post<string>({
    url: APP_API.UPLOAD,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_DATA,
    },
    timeout: 1000 * 60 * 10, // 上传文件超时10分钟
  });
}

/**
 * 创建
 * @param params 创建参数
 * @returns {Promise<AxiosResponse<Result>>} data 创建是否成功 message: 错误提示
 */
export function fetchCreate(params) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: APP_API.CREATE,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
/**
 * 更新
 * @param params 更新参数
 * @returns {Promise<AxiosResponse<Result>>} data 更新是否成功 message: 错误提示
 */
export function fetchUpdate(params) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: APP_API.UPDATE,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}

/**
 * 通过id 获取application 信息
 * @param params 创建参数
 * @returns {Promise<AxiosResponse<Result>>} data 创建是否成功 message: 错误提示
 */
export function fetchGet(params: { id: string }) {
  return defHttp.post<AppListRecord>({
    url: APP_API.GET,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchBackUps(params) {
  return defHttp.post({
    url: APP_API.BACKUPS,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function fetchOptionLog(params) {
  return defHttp.post({
    url: APP_API.OPTION_LOG,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 强制停止
 * @param params id:string
 * @returns
 */
export function fetchForcedStop(params: { id: string }) {
  return defHttp.post<boolean>({
    url: APP_API.FORCED_STOP,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchStart(params) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: APP_API.START,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
export function fetchCopy(params) {
  return defHttp.post<AxiosResponse<any>>(
    {
      url: APP_API.COPY,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
      errorMessageMode: 'none',
    },
  );
}
/**
 * mapping
 * @param params {id:string,appId:string,jobId:string}
 * @returns {Promise<Boolean>}
 */
export function fetchMapping(params) {
  return defHttp.post<boolean>({
    url: APP_API.MAPPING,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

/**
 * log
 * @param params
 * @returns
 */
export function fetchStartLog(params) {
  return defHttp.post<AxiosResponse<any>>(
    {
      url: APP_API.START_LOG,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
/**
 * SavepointPath
 * @param {String} id app Id
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchCheckSavepointPath(params: { id: string }) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: APP_API.CHECK_SAVEPOINT_PATH,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
/**
 * verify Schema
 * @param {String} path
 * @returns {Promise<AxiosResponse<Result>>}
 */
export function fetchVerifySchema(params: { path: string }) {
  return defHttp.post<AxiosResponse<Result>>(
    {
      url: APP_API.CHECK_SAVEPOINT_PATH,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}

/**
 * Cancel
 * @param {CancelParam} params
 * @returns {Promise<Boolean>}
 */
export function fetchCancel(params: CancelParam) {
  return defHttp.post<boolean>({
    url: APP_API.CANCEL,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchName(params) {
  return defHttp.post({
    url: APP_API.NAME,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
