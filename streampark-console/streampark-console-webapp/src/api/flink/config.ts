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
import { HadoopConf } from './config.type';
import { defHttp } from '/@/utils/http/axios';

enum CONFIG_API {
  GET = '/flink/conf/get',
  TEMPLATE = '/flink/conf/template',
  LIST = '/flink/conf/list',
  HISTORY = '/flink/conf/history',
  DELETE = '/flink/conf/delete',
  SYS_HADOOP_CONF = '/flink/conf/sys_hadoop_conf',
}

export function fetchGetVer(data: { id: string }) {
  return defHttp.post({ url: CONFIG_API.GET, data });
}
export function handleConfTemplate() {
  return defHttp.post<string>({
    url: CONFIG_API.TEMPLATE,
  });
}
export function fetchSysHadoopConf() {
  return defHttp.post<HadoopConf>({
    url: CONFIG_API.SYS_HADOOP_CONF,
  });
}
export function fetchListVer(data) {
  return defHttp.post({
    url: CONFIG_API.LIST,
    data,
  });
}
/**
 * delete configuration
 * @returns {Promise<Boolean>}
 * @param data
 */
export function fetchRemoveConf(data: { id: string }): Promise<boolean> {
  return defHttp.post({
    url: CONFIG_API.DELETE,
    data,
  });
}

export function fetchConfHistory(data) {
  return defHttp.post({
    url: CONFIG_API.HISTORY,
    data,
  });
}
