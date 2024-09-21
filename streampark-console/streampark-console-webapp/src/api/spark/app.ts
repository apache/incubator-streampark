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
import { AppListResponse, SparkApplication, DashboardResponse } from './app.type';
import type { Result } from '/#/axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import type { AppExistsStateEnum } from '/@/enums/sparkEnum';
import { defHttp } from '/@/utils/http/axios';

const apiPrefix = `/spark/app`;

/**
 * Get application information by id
 * @param params get parameters
 */
export function fetchGetSparkApp(data: { id: string }) {
  return defHttp.post<SparkApplication>({ url: `${apiPrefix}/get`, data });
}
/**
 * create spark application information
 * @param params get parameters
 */
export function fetchCreateSparkApp(data: SparkApplication) {
  return defHttp.post<boolean>({ url: `${apiPrefix}/create`, data });
}
/**
 * copy spark application information
 * @param params get parameters
 */
export function fetchCopySparkApp(data: SparkApplication) {
  return defHttp.post({ url: `${apiPrefix}/copy`, data }, { isTransformResponse: false });
}
/**
 * update spark application information
 * @param params get parameters
 */
export function fetchUpdateSparkApp(data: SparkApplication) {
  return defHttp.post<boolean>({ url: `${apiPrefix}/update`, data });
}

/**
 * Dashboard data
 * @returns Promise<DashboardResponse>
 */
export function fetchSparkDashboard() {
  return defHttp.post<DashboardResponse>({
    url: `${apiPrefix}/dashboard`,
  });
}

/**
 * Get app list data
 */
export function fetchSparkAppRecord(data: Recordable) {
  return defHttp.post<AppListResponse>({ url: `${apiPrefix}/list`, data });
}

/**
 * mapping
 * @param params {id:string,appId:string,jobId:string}
 */
export function fetchSparkMapping(data: SparkApplication) {
  return defHttp.post<boolean>({ url: `${apiPrefix}/mapping`, data });
}

export function fetchSparkAppStart(data: SparkApplication) {
  return defHttp.post<Result<boolean>>(
    { url: `${apiPrefix}/start`, data },
    { isTransformResponse: false },
  );
}

export function fetchCheckSparkAppStart(data: SparkApplication) {
  return defHttp.post<AppExistsStateEnum>({ url: `${apiPrefix}/check/start`, data });
}

/**
 * Cancel
 * @param {CancelParam} data
 */
export function fetchSparkAppCancel(data: SparkApplication) {
  return defHttp.post({ url: `${apiPrefix}/cancel`, data });
}

/**
 * clean
 * @param {CancelParam} data
 */
export function fetchSparkAppClean(data: SparkApplication) {
  return defHttp.post({ url: `${apiPrefix}/clean`, data });
}
/**
 * forcedStop
 */
export function fetchSparkAppForcedStop(data: SparkApplication) {
  return defHttp.post({ url: `${apiPrefix}/forcedStop`, data });
}

/**
 * get yarn address
 */
export function fetchSparkYarn() {
  return defHttp.post<string>({ url: `${apiPrefix}/yarn` });
}

/**
 * check spark name
 */
export function fetchCheckSparkName(data: { id?: string; appName: string }) {
  return defHttp.post<AppExistsStateEnum>({ url: `${apiPrefix}/check/name`, data });
}

/**
 * read configuration file
 */
export function fetchSparkAppConf(params?: { config: any }) {
  return defHttp.post<string>({
    url: `${apiPrefix}/read_conf`,
    params,
  });
}

/**
 * main
 */
export function fetchSparkMain(data: SparkApplication) {
  return defHttp.post<string>({
    url: `${apiPrefix}/main`,
    data,
  });
}

export function fetchSparkBackUps(data: SparkApplication) {
  return defHttp.post({ url: `${apiPrefix}/backups`, data });
}

export function fetchSparkOptionLog(data: SparkApplication) {
  return defHttp.post({ url: `${apiPrefix}/opt_log`, data });
}

export function fetchSparkDeleteOptLog(id: string) {
  return defHttp.post({ url: `${apiPrefix}/delete/opt_log`, data: { id } });
}

/**
 * remove the app
 */
export function fetchSparkAppRemove(id: string) {
  return defHttp.post({ url: `${apiPrefix}/delete`, data: { id } });
}

export function fetchSparkRemoveBackup(id: string) {
  return defHttp.post({ url: `${apiPrefix}/delete/bak`, data: { id } });
}

/**
 * upload
 * @param params
 */
export function fetchSparkUpload(params: any) {
  return defHttp.post<string>({
    url: `${apiPrefix}/upload`,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_DATA,
    },
    timeout: 1000 * 60 * 10, // Uploading files timed out for 10 minutes
  });
}
